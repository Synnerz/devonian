package com.github.synnerz.devonian.utils.render.impl

import com.github.synnerz.devonian.Devonian
import com.github.synnerz.devonian.mixin.accessor.RenderSetupAccessor
import com.github.synnerz.devonian.mixin.accessor.RenderTypeAccessor
import com.github.synnerz.devonian.utils.StringUtils
import com.github.synnerz.devonian.utils.math.ShapeUtils
import com.github.synnerz.devonian.utils.render.IRender3D.LinesBuilder
import com.github.synnerz.devonian.utils.render.IRender3D.VertexBuilder
import com.github.synnerz.devonian.utils.render.Render3DTypes
import com.mojang.blaze3d.buffers.GpuBuffer
import com.mojang.blaze3d.pipeline.RenderTarget
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.BufferBuilder
import com.mojang.blaze3d.vertex.ByteBufferBuilder
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import com.mojang.blaze3d.vertex.VertexFormat
import com.mojang.math.Axis
import net.minecraft.client.gui.Font
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.SubmitNodeStorage
import net.minecraft.client.renderer.rendertype.RenderType
import net.minecraft.world.phys.shapes.VoxelShape
import org.joml.Matrix4f
import org.joml.Vector3d
import org.joml.Vector3f
import org.joml.Vector4f
import java.awt.Color
import java.util.*
import kotlin.math.sqrt

/**
 * you should never be calling any methods here directly
 *
 * only handles actually dumping vertices into a buffer
 */
object Render3DVertex {
    private val minecraft = Devonian.minecraft
    private val textRenderer = minecraft.font
    private val vertexAllocator = ByteBufferBuilder(RenderType.SMALL_BUFFER_SIZE)
    private val batchedDraws = Array(BatchedRenderType.MAX_ID + 1) { mutableListOf<BatchedDraw>() }
    private val batchedDrawsPhase = Array(BatchedRenderType.MAX_ID + 1) { mutableListOf<BatchedDraw>() }
    private data class BatchedDraw(val pose: PoseStack.Pose, val type: BatchedRenderType, val cb: (pose: PoseStack.Pose, consumer: VertexConsumer) -> Unit)
    private val batchedText = mutableListOf<SubmitNodeStorage.TextSubmit>()
    private val batchedTextPhase = mutableListOf<SubmitNodeStorage.TextSubmit>()

    private fun addBatchedDraw(
        batch: BatchedRenderType,
        stack: PoseStack,
        cb: (pose: PoseStack.Pose, consumer: VertexConsumer) -> Unit
    ) {
        val b = if (batch.phase) batchedDrawsPhase else batchedDraws
        b[batch.batchId].add(BatchedDraw(stack.last().copy(), batch, cb))
    }

    fun renderFilledShape(
        stack: PoseStack,
        batchedType: BatchedRenderType,
        shape: VoxelShape,
        ox: Double,
        oy: Double,
        oz: Double,
        color: Color,
    ) {
        val faces = ShapeUtils.getFaces(shape)
        addBatchedDraw(batchedType, stack) { pose, consumer ->
            for (i in faces.indices step 3) {
                val x = faces[i + 0] + ox
                val y = faces[i + 1] + oy
                val z = faces[i + 2] + oz

                val dir = Vector3d(x, y, z)
                dir.mul(-0.01 / dir.length())

                consumer
                    .addVertex(pose, (x + dir.x).toFloat(), (y + dir.y).toFloat(), (z + dir.z).toFloat())
                    .setColor(color.rgb)
            }
        }
    }

    fun renderWireframeShape(
        stack: PoseStack,
        batchedType: BatchedRenderType,
        shape: VoxelShape,
        ox: Double,
        oy: Double,
        oz: Double,
        color: Color,
        lineWidth: Double,
    ) {
        addBatchedDraw(batchedType, stack) { pose, consumer ->
            shape.forAllEdges { h, j, k, l, m, n ->
                val vector3f = Vector3f((l - h).toFloat(), (m - j).toFloat(), (n - k).toFloat()).normalize()

                consumer.addVertex(pose, (h + ox).toFloat(), (j + oy).toFloat(), (k + oz).toFloat())
                    .setColor(color.rgb)
                    .setNormal(pose, vector3f)
                    .setLineWidth(lineWidth.toFloat())
                consumer.addVertex(pose, (l + ox).toFloat(), (m + oy).toFloat(), (n + oz).toFloat())
                    .setColor(color.rgb)
                    .setNormal(pose, vector3f)
                    .setLineWidth(lineWidth.toFloat())
            }
        }
    }

    fun renderFilledBox(
        stack: PoseStack,
        batchedType: BatchedRenderType,
        x: Double,
        y: Double,
        z: Double,
        wx: Double,
        h: Double,
        wz: Double,
        color: Color,
    ) {
        val x1 = x.toFloat()
        val y1 = y.toFloat()
        val z1 = z.toFloat()
        val x2 = (x + wx).toFloat()
        val y2 = (y + h).toFloat()
        val z2 = (z + wz).toFloat()
        val c = color.rgb

        addBatchedDraw(batchedType, stack) { m, consumer ->
            consumer.addVertex(m, x1, y1, z1).setColor(c)
            consumer.addVertex(m, x1, y2, z1).setColor(c)
            consumer.addVertex(m, x2, y1, z1).setColor(c)

            consumer.addVertex(m, x2, y1, z1).setColor(c)
            consumer.addVertex(m, x1, y2, z1).setColor(c)
            consumer.addVertex(m, x2, y2, z1).setColor(c)

            consumer.addVertex(m, x2, y1, z1).setColor(c)
            consumer.addVertex(m, x2, y2, z1).setColor(c)
            consumer.addVertex(m, x2, y1, z2).setColor(c)

            consumer.addVertex(m, x2, y1, z2).setColor(c)
            consumer.addVertex(m, x2, y2, z1).setColor(c)
            consumer.addVertex(m, x2, y2, z2).setColor(c)

            consumer.addVertex(m, x2, y1, z2).setColor(c)
            consumer.addVertex(m, x2, y2, z2).setColor(c)
            consumer.addVertex(m, x1, y1, z2).setColor(c)

            consumer.addVertex(m, x1, y1, z2).setColor(c)
            consumer.addVertex(m, x2, y2, z2).setColor(c)
            consumer.addVertex(m, x1, y2, z2).setColor(c)

            consumer.addVertex(m, x1, y1, z2).setColor(c)
            consumer.addVertex(m, x1, y2, z2).setColor(c)
            consumer.addVertex(m, x1, y1, z1).setColor(c)

            consumer.addVertex(m, x1, y1, z1).setColor(c)
            consumer.addVertex(m, x1, y2, z2).setColor(c)
            consumer.addVertex(m, x1, y2, z1).setColor(c)

            consumer.addVertex(m, x1, y2, z1).setColor(c)
            consumer.addVertex(m, x1, y2, z2).setColor(c)
            consumer.addVertex(m, x2, y2, z1).setColor(c)

            consumer.addVertex(m, x2, y2, z1).setColor(c)
            consumer.addVertex(m, x1, y2, z2).setColor(c)
            consumer.addVertex(m, x2, y2, z2).setColor(c)

            consumer.addVertex(m, x1, y1, z2).setColor(c)
            consumer.addVertex(m, x1, y1, z1).setColor(c)
            consumer.addVertex(m, x2, y1, z2).setColor(c)

            consumer.addVertex(m, x2, y1, z2).setColor(c)
            consumer.addVertex(m, x1, y1, z1).setColor(c)
            consumer.addVertex(m, x2, y1, z1).setColor(c)
        }
    }

    fun renderWireframeBox(
        stack: PoseStack,
        batchedType: BatchedRenderType,
        x: Double,
        y: Double,
        z: Double,
        wx: Double,
        h: Double,
        wz: Double,
        color: Color,
        lineWidth: Double,
    ) {
        val x1 = x
        val y1 = y
        val z1 = z
        val x2 = x + wx
        val y2 = y + h
        val z2 = z + wz

        renderLines(stack, batchedType) {
            submit(x1, y1, z1, x2, y1, z1, color, color, lineWidth, lineWidth)
            submit(x1, y2, z1, x2, y2, z1, color, color, lineWidth, lineWidth)
            submit(x1, y1, z1, x1, y2, z1, color, color, lineWidth, lineWidth)
            submit(x2, y1, z1, x2, y2, z1, color, color, lineWidth, lineWidth)
            submit(x1, y1, z2, x2, y1, z2, color, color, lineWidth, lineWidth)
            submit(x1, y2, z2, x2, y2, z2, color, color, lineWidth, lineWidth)
            submit(x1, y1, z2, x1, y2, z2, color, color, lineWidth, lineWidth)
            submit(x2, y1, z2, x2, y2, z2, color, color, lineWidth, lineWidth)
            submit(x1, y1, z1, x1, y1, z2, color, color, lineWidth, lineWidth)
            submit(x1, y2, z1, x1, y2, z2, color, color, lineWidth, lineWidth)
            submit(x2, y1, z1, x2, y1, z2, color, color, lineWidth, lineWidth)
            submit(x2, y2, z1, x2, y2, z2, color, color, lineWidth, lineWidth)
        }
    }

    fun renderString(
        stack: PoseStack,
        str: String,
        phase: Boolean,
        color: Color,
        backgroundBox: Color,
    ) {
        val offset = -textRenderer.width(str) * 0.5f
        val deltaPartialTick = minecraft.deltaTracker.getGameTimeDeltaPartialTick(true)
        val mode = if (phase) Font.DisplayMode.SEE_THROUGH else Font.DisplayMode.NORMAL

        val b = if (phase) batchedTextPhase else batchedText
        b.add(
            SubmitNodeStorage.TextSubmit(
                Matrix4f(stack.last().pose()),
                offset,
                0f,
                StringUtils.fromLegacy(str).visualOrderText,
                true,
                mode,
                minecraft.entityRenderDispatcher.getPackedLightCoords(minecraft.player!!, deltaPartialTick),
                color.rgb,
                ((minecraft.options.getBackgroundOpacity(0.25f) * backgroundBox.alpha).toInt() shl 24) or
                (backgroundBox.rgb and 0x00FFFFFF),
                0,
            )
        )
    }

    // TODO: clip beam to view frustum https://github.com/PerseusPotter/Apelles/blob/42b1f9f83136293af648c0b92e76599aa4f6bd3d/java/src/main/kotlin/com/perseuspotter/apelles/Renderer.kt#L511

    fun renderBeamInner(
        stack: PoseStack,
        batchedType: BatchedRenderType,
        color: Color,
        h: Double,
    ) {
        val worldTime = minecraft.level?.gameTime ?: 0L
        val partialTicks = minecraft.deltaTracker.getGameTimeDeltaPartialTick(false)
        val time = Math.floorMod(worldTime, 40) + partialTicks

        stack.pushPose()
        stack.mulPose(Axis.YP.rotationDegrees(time * 2.25f - 45.0f))
        addBatchedDraw(batchedType, stack) { pose, consumer ->
            BeaconBeamRenderer.renderBeamInner(
                pose,
                consumer,
                partialTicks,
                worldTime,
                color,
                h,
            )
        }
        stack.popPose()
    }

    fun renderBeamOuter(
        stack: PoseStack,
        batchedType: BatchedRenderType,
        color: Color,
        h: Double,
    ) {
        addBatchedDraw(batchedType, stack) { pose, consumer ->
            BeaconBeamRenderer.renderBeamOuter(
                pose,
                consumer,
                minecraft.deltaTracker.getGameTimeDeltaPartialTick(false),
                minecraft.level?.gameTime ?: 0L,
                color,
                h,
            )
        }
    }

    fun renderLines(
        stack: PoseStack,
        batchedType: BatchedRenderType,
        supplier: LinesBuilder.() -> Unit,
    ) {
        addBatchedDraw(batchedType, stack) { pose, consumer ->
            supplier.invoke(
                object : LinesBuilder {
                    override fun submit(
                        x0: Double, y0: Double, z0: Double,
                        x1: Double, y1: Double, z1: Double,
                        c0: Color, c1: Color,
                        lineWidth0: Double, lineWidth1: Double,
                    ) {
                        var dx = (x1 - x0).toFloat()
                        var dy = (y1 - y0).toFloat()
                        var dz = (z1 - z0).toFloat()
                        val f = 1f / sqrt(dx * dx + dy * dy + dz * dz)
                        dx *= f
                        dy *= f
                        dz *= f

                        consumer
                            .addVertex(pose, x0.toFloat(), y0.toFloat(), z0.toFloat())
                            .setColor(c0.rgb)
                            .setNormal(pose, dx, dy, dz)
                            .setLineWidth(lineWidth0.toFloat())

                        consumer
                            .addVertex(pose, x1.toFloat(), y1.toFloat(), z1.toFloat())
                            .setColor(c1.rgb)
                            .setNormal(pose, dx, dy, dz)
                            .setLineWidth(lineWidth1.toFloat())
                    }
                }
            )
        }
    }

    fun renderLineStrip(
        stack: PoseStack,
        batchedType: BatchedRenderType,
        supplier: VertexBuilder.() -> Unit,
    ) {
        var first = true
        var px = 0.0
        var py = 0.0
        var pz = 0.0
        var pc = Color(0, true)
        var pw = 1.0

        renderLines(stack, batchedType) {
            supplier.invoke(
                object : VertexBuilder {
                    override fun submit(x: Double, y: Double, z: Double, c: Color, lineWidth: Double) {
                        if (!first) {
                            submit(px, py, pz, x, y, z, pc, c, pw, lineWidth)
                        }
                        first = false
                        px = x
                        py = y
                        pz = z
                        pc = c
                        pw = lineWidth
                    }

                    override fun endBatch() {
                        first = true
                    }
                }
            )
        }
    }

    private var textBuffer: BufferBuilder? = null
    private var textBufferType: BatchedRenderType? = null
    private val textRendererBufferSource = MultiBufferSource { renderType ->
        if (textBuffer == null) {
            val rt = renderType as RenderTypeAccessor
            val setup = rt.state

            @Suppress("CAST_NEVER_SUCCEEDS")
            val texture = (setup as RenderSetupAccessor).textures2
            val texturePath = texture.values.first().location
            val phase = rt.name == "text_see_through"
            val type = (if (phase) Render3DTypes.TEXT_ESP else Render3DTypes.TEXT).apply(texturePath)
            val id = BatchedRenderType.MAX_ID + (if (phase) 2 else 1)

            textBuffer = BufferBuilder(vertexAllocator, type.mode(), type.format())
            textBufferType = BatchedRenderType("Text", type, id, phase)
        }

        return@MultiBufferSource textBuffer!!
    }

    private fun drawBuffer(target: RenderTarget, rType: BatchedRenderType, buf: BufferBuilder) {
        val built = buf.build() ?: return
        val drawState = built.drawState()

        val type = rType.type
        // rType.type.draw(built)

        // TODO: sort on upload

        val rt = type as RenderTypeAccessor
        @Suppress("CAST_NEVER_SUCCEEDS")
        val rs = rt.state as RenderSetupAccessor

        val dynamicTransforms = RenderSystem.getDynamicUniforms()
            .writeTransform(
                RenderSystem.getModelViewMatrix(),
                Vector4f(1f),
                Vector3f(),
                rs.textureTransform.matrix,
            )
        val textures = rt.state.textures

        built.use { mesh ->
            val vertices = drawState.format.uploadImmediateVertexBuffer(mesh.vertexBuffer())
            val indexType: VertexFormat.IndexType
            val indices: GpuBuffer
            mesh.indexBuffer().let { iBuf ->
                if (iBuf == null) {
                    val autoIndices = RenderSystem.getSequentialBuffer(drawState.mode)
                    indexType = autoIndices.type()
                    indices = autoIndices.getBuffer(drawState.indexCount)
                } else {
                    indexType = drawState.indexType
                    indices = drawState.format.uploadImmediateIndexBuffer(iBuf)
                }
            }

            RenderSystem
                .getDevice()
                .createCommandEncoder()
                .createRenderPass(
                    { "Devonian Render Pass for ${rType.name}" },
                    target.colorTextureView!!,
                    OptionalInt.empty(),
                    target.depthTextureView,
                    OptionalDouble.empty(),
                )
                .use { renderPass ->
                    renderPass.setPipeline(type.pipeline())

                    RenderSystem.bindDefaultUniforms(renderPass)
                    renderPass.setUniform("DynamicTransforms", dynamicTransforms)

                    val scissor = RenderSystem.getScissorStateForRenderTypeDraws()
                    if (scissor.enabled()) renderPass.enableScissor(
                        scissor.x(), scissor.y(),
                        scissor.width(), scissor.height()
                    )
                    else renderPass.disableScissor()

                    renderPass.setVertexBuffer(0, vertices)
                    renderPass.setIndexBuffer(indices, indexType)

                    textures.forEach { (name, value) ->
                        renderPass.bindTexture(name, value.textureView, value.sampler)
                    }

                    renderPass.drawIndexed(0, 0, drawState.indexCount, 1)
                }
        }
    }

    private fun batchedRender(
        calls: Array<MutableList<BatchedDraw>>,
        textCalls: MutableList<SubmitNodeStorage.TextSubmit>,
        target: RenderTarget,
    ) {
        calls.forEach { arr ->
            if (arr.isEmpty()) return@forEach

            val batchedType = arr[0].type
            val buf = BufferBuilder(vertexAllocator, batchedType.type.mode(), batchedType.type.format())

            arr.forEach { draw ->
                draw.cb(draw.pose, buf)
            }
            arr.clear()

            drawBuffer(target, batchedType, buf)
        }

        val font = minecraft.font
        textCalls.forEach {
            font.drawInBatch(
                it.string,
                it.x, it.y,
                it.color,
                it.dropShadow,
                it.pose,
                textRendererBufferSource,
                it.displayMode,
                it.backgroundColor,
                it.lightCoords,
            )
        }
        textCalls.clear()

        if (textBuffer != null) drawBuffer(target, textBufferType!!, textBuffer!!)

        textBuffer = null
        textBufferType = null
    }

    fun internalBatchedRender() {
        batchedRender(batchedDraws, batchedText, minecraft.mainRenderTarget)
    }

    fun internalBatchedRenderPhase(target: RenderTarget) {
        batchedRender(batchedDrawsPhase, batchedTextPhase, target)
    }

    fun internalHasPhaseRenders(): Boolean {
        return batchedTextPhase.isNotEmpty() || batchedDrawsPhase.any { it.isNotEmpty() }
    }

    fun internalClose() {
        vertexAllocator.close()
    }
}