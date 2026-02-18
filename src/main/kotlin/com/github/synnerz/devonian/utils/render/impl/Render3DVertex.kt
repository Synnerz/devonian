package com.github.synnerz.devonian.utils.render.impl

import com.github.synnerz.devonian.Devonian
import com.github.synnerz.devonian.utils.math.ShapeUtils
import com.github.synnerz.devonian.utils.render.IRender3D.LinesBuilder
import com.github.synnerz.devonian.utils.render.IRender3D.VertexBuilder
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.gui.Font
import net.minecraft.client.renderer.LightTexture
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.ShapeRenderer
import net.minecraft.client.renderer.rendertype.RenderType
import net.minecraft.client.renderer.state.CameraRenderState
import net.minecraft.world.phys.shapes.VoxelShape
import org.joml.Vector3d
import java.awt.Color
import kotlin.math.sqrt

/**
 * you should never be calling any methods here directly
 *
 * only handles actually dumping vertices into a buffer
 */
object Render3DVertex {
    private val minecraft = Devonian.minecraft
    private val textRenderer = minecraft.font

    fun renderFilledShape(
        stack: PoseStack,
        consumer: VertexConsumer,
        shape: VoxelShape,
        ox: Double,
        oy: Double,
        oz: Double,
        color: Color,
    ) {
        val mat = stack.last()

        val faces = ShapeUtils.getFaces(shape)
        for (i in faces.indices step 3) {
            val x = faces[i + 0] + ox
            val y = faces[i + 1] + oy
            val z = faces[i + 2] + oz

            val dir = Vector3d(x, y, z)
            dir.mul(-0.01 / dir.length())

            consumer
                .addVertex(mat, (x + dir.x).toFloat(), (y + dir.y).toFloat(), (z + dir.z).toFloat())
                .setColor(color.rgb)
        }
    }

    fun renderWireframeShape(
        stack: PoseStack,
        consumer: VertexConsumer,
        shape: VoxelShape,
        ox: Double,
        oy: Double,
        oz: Double,
        color: Color,
    ) {
        ShapeRenderer.renderShape(
            stack,
            consumer,
            shape,
            ox, oy, oz,
            color.rgb,
            1f // line width - thanks a lot mojang
        )
    }

    fun renderFilledBox(
        stack: PoseStack,
        consumer: VertexConsumer,
        x: Double,
        y: Double,
        z: Double,
        wx: Double,
        h: Double,
        wz: Double,
        color: Color,
    ) {
        ShapeRenderer.addChainedFilledBoxVertices(
            stack,
            consumer,
            x, y, z,
            x + wx, y + h, z + wz,
            color.red / 255f,
            color.green / 255f,
            color.blue / 255f,
            color.alpha / 255f,
        )
    }

    fun renderWireframeBox(
        stack: PoseStack,
        consumer: VertexConsumer,
        x: Double,
        y: Double,
        z: Double,
        wx: Double,
        h: Double,
        wz: Double,
        color: Color,
    ) {
        ShapeRenderer.renderLineBox(
            stack.last(),
            consumer,
            x, y, z,
            x + wx, y + h, z + wz,
            color.red / 255f,
            color.green / 255f,
            color.blue / 255f,
            color.alpha / 255f,
        )
    }

    fun renderString(
        camera: CameraRenderState,
        stack: PoseStack,
        bufferSource: MultiBufferSource.BufferSource,
        mode: Font.DisplayMode,
        str: String,
        x: Double,
        y: Double,
        z: Double,
        scale: Float,
        maxDist: Double,
        color: Color,
        backgroundBox: Color,
    ) {
        var scale = scale

        val offset = -textRenderer.width(str) * 0.5f
        val dist = x * x + y * y + z * z
        if (dist > maxDist * maxDist) {
            val f = sqrt(dist) / maxDist
            scale = (scale * f).toFloat()
        }

        stack.pushPose()
        stack.last()
            .translate(x.toFloat(), y.toFloat(), z.toFloat())
            .rotate(camera.orientation)
            .scale(scale * 0.025f, -scale * 0.025f, scale * 0.025f)

        textRenderer.drawInBatch(
            str,
            offset,
            0f,
            color.rgb,
            true,
            stack.last().pose(),
            bufferSource,
            mode,
            ((minecraft.options.getBackgroundOpacity(0.25f) * backgroundBox.alpha).toInt() shl 24) or
            (backgroundBox.rgb and 0x00FFFFFF),
            LightTexture.FULL_BLOCK,
        )

        bufferSource.endBatch()

        stack.popPose()
    }

    // TODO: clip beam to view frustum https://github.com/PerseusPotter/Apelles/blob/42b1f9f83136293af648c0b92e76599aa4f6bd3d/java/src/main/kotlin/com/perseuspotter/apelles/Renderer.kt#L511

    fun renderBeamInner(
        stack: PoseStack,
        bufferSource: MultiBufferSource.BufferSource,
        opaqueLayer: RenderType,
        translucentLayer: RenderType,
        color: Color,
        h: Double,
    ) {
        BeaconBeamRenderer.renderBeamInner(
            stack,
            bufferSource,
            opaqueLayer,
            translucentLayer,
            minecraft.deltaTracker.getGameTimeDeltaPartialTick(false),
            minecraft.level?.gameTime ?: 0L,
            color,
            h,
        )
    }

    fun renderBeamOuter(
        stack: PoseStack,
        bufferSource: MultiBufferSource.BufferSource,
        opaqueLayer: RenderType,
        translucentLayer: RenderType,
        color: Color,
        h: Double,
    ) {
        BeaconBeamRenderer.renderBeamOuter(
            stack,
            bufferSource,
            opaqueLayer,
            translucentLayer,
            minecraft.deltaTracker.getGameTimeDeltaPartialTick(false),
            minecraft.level?.gameTime ?: 0L,
            color,
            h,
        )
    }

    fun renderLines(
        stack: PoseStack,
        consumer: VertexConsumer,
        supplier: LinesBuilder.() -> Unit,
    ) {
        val mat = stack.last()

        supplier.invoke(
            object : LinesBuilder {
                override fun submit(
                    x0: Double, y0: Double, z0: Double,
                    x1: Double, y1: Double, z1: Double,
                    c0: Color, c1: Color,
                ) {
                    var dx = (x1 - x0).toFloat()
                    var dy = (y1 - y0).toFloat()
                    var dz = (z1 - z0).toFloat()
                    val f = 1f / sqrt(dx * dx + dy * dy + dz * dz)
                    dx *= f
                    dy *= f
                    dz *= f

                    consumer
                        .addVertex(mat, x0.toFloat(), y0.toFloat(), z0.toFloat())
                        .setColor(c0.rgb)
                        .setNormal(mat, dx, dy, dz)

                    consumer
                        .addVertex(mat, x1.toFloat(), y1.toFloat(), z1.toFloat())
                        .setColor(c1.rgb)
                        .setNormal(mat, dx, dy, dz)
                }
            }
        )
    }

    fun renderLineStrip(
        stack: PoseStack,
        consumer: VertexConsumer,
        supplier: VertexBuilder.() -> Unit,
    ) {
        var first = true
        var px = 0.0
        var py = 0.0
        var pz = 0.0
        var pc = Color(0, true)

        renderLines(stack, consumer) {
            supplier.invoke(
                object : VertexBuilder {
                    override fun submit(x: Double, y: Double, z: Double, c: Color) {
                        if (!first) {
                            submit(px, py, pz, x, y, z, pc, c)
                        }
                        first = false
                        px = x
                        py = y
                        pz = z
                        pc = c
                    }

                    override fun endBatch() {
                        first = true
                    }
                }
            )
        }
    }
}