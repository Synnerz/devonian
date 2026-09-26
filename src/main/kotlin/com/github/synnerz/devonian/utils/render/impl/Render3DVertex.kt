package com.github.synnerz.devonian.utils.render.impl

import com.github.synnerz.devonian.Devonian
import com.github.synnerz.devonian.mixin.accessor.RenderSetupAccessor
import com.github.synnerz.devonian.mixin.accessor.RenderTypeAccessor
import com.github.synnerz.devonian.mixin.accessor.RenderTypeFeatureRendererAccessor
import com.github.synnerz.devonian.utils.StringUtils
import com.github.synnerz.devonian.utils.math.ShapeUtils
import com.github.synnerz.devonian.utils.render.IRender3D.LinesBuilder
import com.github.synnerz.devonian.utils.render.IRender3D.VertexBuilder
import com.github.synnerz.devonian.utils.render.Render3DTypes
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import com.mojang.math.Axis
import net.minecraft.client.gui.Font
import net.minecraft.client.renderer.StagedVertexBuffer
import net.minecraft.client.renderer.feature.FeatureFrameContext
import net.minecraft.client.renderer.feature.RenderTypeFeatureRenderer
import net.minecraft.client.renderer.feature.TextFeatureRenderer
import net.minecraft.client.renderer.rendertype.PreparedRenderType
import net.minecraft.client.renderer.rendertype.RenderType
import net.minecraft.world.phys.shapes.VoxelShape
import org.joml.Matrix4f
import org.joml.Vector3d
import org.joml.Vector3f
import org.joml.Vector4fc
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
    private val vertexBuffer = StagedVertexBuffer({ "Devonian Render Buffer "}, RenderType.SMALL_BUFFER_SIZE)
    private val batchedDraws = Array(BatchedRenderType.MAX_ID + 1) { mutableListOf<BatchedDraw>() }
    private data class BatchedDraw(val pose: PoseStack.Pose, val type: BatchedRenderType, val cb: (pose: PoseStack.Pose, consumer: VertexConsumer) -> Unit)
    private val batchedText = mutableListOf<TextFeatureRenderer.Submit>()

    private fun addBatchedDraw(
        batch: BatchedRenderType,
        stack: PoseStack,
        cb: (pose: PoseStack.Pose, consumer: VertexConsumer) -> Unit
    ) {
        batchedDraws[batch.batchId].add(BatchedDraw(stack.last().copy(), batch, cb))
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
        mode: Font.DisplayMode,
        str: String,
        color: Color,
        backgroundBox: Color,
    ) {
        val offset = -textRenderer.width(str) * 0.5f
        val deltaPartialTick = minecraft.deltaTracker.getGameTimeDeltaPartialTick(true)

        batchedText.add(
            TextFeatureRenderer.Submit(
                Matrix4f(stack.last().pose()),
                mode,
                minecraft.entityRenderDispatcher.getPackedLightCoords(minecraft.player!!, deltaPartialTick),
                TextFeatureRenderer.Content.Text(
                    offset,
                    0f,
                    StringUtils.fromLegacy(str).visualOrderText,
                    true,
                    color.rgb,
                    ((minecraft.options.getBackgroundOpacity(0.25f) * backgroundBox.alpha).toInt() shl 24) or
                    (backgroundBox.rgb and 0x00FFFFFF),
                    0,
                )
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
        stack.rotate(Axis.YP.rotationDegrees(time * 2.25f - 45.0f))
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

    private data class PreparedDraw(val name: String, val drawInfo: StagedVertexBuffer.Draw, val type: PreparedRenderType)
    private val draws = mutableListOf<PreparedDraw>()

    private fun getBuffer(batchedType: BatchedRenderType): VertexConsumer {
        val renderType = batchedType.type
        val pipeline = renderType.pipeline()
        val vertexFormat = pipeline.getVertexFormatBinding(0)
        val dataType = pipeline.primitiveTopology

        val drawInfo = vertexBuffer.appendDraw(
            vertexFormat!!,
            dataType,
            if (renderType.sortOnUpload()) RenderSystem.getProjectionType().vertexSorting() else null,
        )

        draws.add(PreparedDraw(batchedType.name, drawInfo, renderType.prepare()))

        return vertexBuffer.getVertexBuilder(drawInfo)
    }

    @Suppress("CAST_NEVER_SUCCEEDS", "UNCHECKED_CAST")
    private val textFeatureRenderer =
        (TextFeatureRenderer() as RenderTypeFeatureRendererAccessor<TextFeatureRenderer.Submit>).also {
            it.setCurrentGroup(
                object : RenderTypeFeatureRenderer.Group(vertexBuffer, false) {
                    override fun getVertexBuilder(renderType: RenderType): VertexConsumer {
                        val rt = renderType as RenderTypeAccessor
                        val setup = rt.state
                        @Suppress("CAST_NEVER_SUCCEEDS")
                        val texture = (setup as RenderSetupAccessor).textures
                        val texturePath = texture.values.first().location
                        val phase = rt.name == "text_see_through"
                        val type = (if (phase) Render3DTypes.TEXT_ESP else Render3DTypes.TEXT).apply(texturePath)
                        return getBuffer(BatchedRenderType("Text", type, 0))
                    }
                }
            )
        }

    fun internalBatchedRender() {
        batchedDraws.forEach { arr ->
            if (arr.isEmpty()) return@forEach

            val batchedType = arr[0].type
            val buffer = getBuffer(batchedType)

            arr.forEach { draw ->
                draw.cb(draw.pose, buffer)
            }
            arr.clear()
        }

        val gameRenderer = minecraft.gameRenderer

        val ffc = FeatureFrameContext(
            gameRenderer.gameRenderState().optionsRenderState,
            minecraft.font,
            minecraft.modelManager.blockStateModelSet,
            minecraft.blockColors,
            minecraft.textureManager,
            minecraft.atlasManager,
            gameRenderer.lightmap(),
            vertexBuffer,
        )
        textFeatureRenderer.invokeBuildGroup(ffc, batchedText)
        batchedText.clear()

        vertexBuffer.upload()

        val target = gameRenderer.mainRenderTarget()

        draws.forEach { (name, drawInfo, type) ->
            val info = vertexBuffer.getExecuteInfo(drawInfo) ?: return@forEach

            // type.drawFromBuffer(info)

            RenderSystem
                .getDevice()
                .createCommandEncoder()
                .createRenderPass(
                    { "Devonian Render Pass for $name" },
                    target.colorTextureView!!,
                    Optional.empty<Vector4fc>(),
                    target.depthTextureView,
                    OptionalDouble.empty(),
                )
                .use { renderPass ->
                    renderPass.setPipeline(RenderSystem.getCompiledPipeline(type.pipeline))

                    RenderSystem.bindDefaultUniforms(renderPass)
                    renderPass.setUniform("DynamicTransforms", type.dynamicTransforms)

                    if (type.scissorState.enabled()) renderPass.enableScissor(
                        type.scissorState.x(), type.scissorState.y(),
                        type.scissorState.width(), type.scissorState.height()
                    )
                    else renderPass.disableScissor()

                    renderPass.setVertexBuffer(0, info.vertexBuffer.slice())
                    renderPass.setIndexBuffer(info.indexBuffer(), info.indexType)

                    type.textures.forEach {
                        renderPass.setUniform(it.name, it.textureView, it.sampler)
                    }

                    renderPass.drawIndexed(
                        info.indexCount,
                        1,
                        info.firstIndex,
                        info.baseVertex,
                        0
                    )
                }
        }
        draws.clear()

        vertexBuffer.endFrame()
    }

    fun internalClose() {
        vertexBuffer.close()
    }
}