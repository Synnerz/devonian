package com.github.synnerz.devonian.utils.render.impl

import com.github.synnerz.devonian.Devonian
import com.github.synnerz.devonian.mixin.accessor.LevelRendererAccessor
import com.github.synnerz.devonian.utils.StringUtils
import com.github.synnerz.devonian.utils.math.ShapeUtils
import com.github.synnerz.devonian.utils.render.IRender3D.LinesBuilder
import com.github.synnerz.devonian.utils.render.IRender3D.VertexBuilder
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.gui.Font
import net.minecraft.client.renderer.rendertype.RenderType
import net.minecraft.client.renderer.state.level.CameraRenderState
import net.minecraft.world.phys.shapes.VoxelShape
import org.joml.Vector3d
import org.joml.Vector3f
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
    val submitNodeStorage get() = (minecraft.levelRenderer as LevelRendererAccessor).submitNodeStorage

    fun renderFilledShape(
        stack: PoseStack,
        renderType: RenderType,
        shape: VoxelShape,
        ox: Double,
        oy: Double,
        oz: Double,
        color: Color,
    ) {
        val mat = stack.last()

        val faces = ShapeUtils.getFaces(shape)
        submitNodeStorage.submitCustomGeometry(stack, renderType) { _, consumer ->
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
    }

    fun renderWireframeShape(
        stack: PoseStack,
        renderType: RenderType,
        shape: VoxelShape,
        ox: Double,
        oy: Double,
        oz: Double,
        color: Color,
        lineWidth: Double,
    ) {
        submitNodeStorage.submitCustomGeometry(stack, renderType) { pose, consumer ->
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
        renderType: RenderType,
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

        submitNodeStorage.submitCustomGeometry(stack, renderType) { m, consumer ->
            consumer.addVertex(m, x1, y1, z1).setColor(c)
            consumer.addVertex(m, x1, y2, z1).setColor(c)
            consumer.addVertex(m, x2, y1, z1).setColor(c)
            consumer.addVertex(m, x2, y2, z1).setColor(c)

            consumer.addVertex(m, x2, y1, z2).setColor(c)
            consumer.addVertex(m, x2, y2, z2).setColor(c)

            consumer.addVertex(m, x1, y1, z2).setColor(c)
            consumer.addVertex(m, x1, y2, z2).setColor(c)

            consumer.addVertex(m, x1, y1, z1).setColor(c)
            consumer.addVertex(m, x1, y2, z1).setColor(c)

            consumer.addVertex(m, x1, y2, z1).setColor(c)

            consumer.addVertex(m, x1, y2, z2).setColor(c)
            consumer.addVertex(m, x2, y2, z1).setColor(c)
            consumer.addVertex(m, x2, y2, z2).setColor(c)

            consumer.addVertex(m, x2, y2, z2).setColor(c)
            consumer.addVertex(m, x1, y1, z2).setColor(c)

            consumer.addVertex(m, x1, y1, z2).setColor(c)
            consumer.addVertex(m, x1, y1, z1).setColor(c)
            consumer.addVertex(m, x2, y1, z2).setColor(c)
            consumer.addVertex(m, x2, y1, z1).setColor(c)
        }

    }

    fun renderWireframeBox(
        stack: PoseStack,
        renderType: RenderType,
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

        renderLines(stack, renderType) {
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
        camera: CameraRenderState,
        stack: PoseStack,
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

        val deltaPartialTick = minecraft.deltaTracker.getGameTimeDeltaPartialTick(true)
        submitNodeStorage.submitText(
            stack,
            offset,
            0f,
            StringUtils.fromLegacy(str).visualOrderText,
            true,
            mode,
            minecraft.entityRenderDispatcher.getPackedLightCoords(minecraft.player!!, deltaPartialTick),
            color.rgb,
            ((minecraft.options.getBackgroundOpacity(0.25f) * backgroundBox.alpha).toInt() shl 24) or
                    (backgroundBox.rgb and 0x00FFFFFF),
            0
        )

        stack.popPose()
    }

    // TODO: clip beam to view frustum https://github.com/PerseusPotter/Apelles/blob/42b1f9f83136293af648c0b92e76599aa4f6bd3d/java/src/main/kotlin/com/perseuspotter/apelles/Renderer.kt#L511

    fun renderBeamInner(
        stack: PoseStack,
        opaqueLayer: RenderType,
        translucentLayer: RenderType,
        color: Color,
        h: Double,
    ) {
        submitNodeStorage.submitCustomGeometry(stack, if (color.alpha == 255) opaqueLayer else translucentLayer) { _, consumer ->
            BeaconBeamRenderer.renderBeamInner(
                stack,
                consumer,
                minecraft.deltaTracker.getGameTimeDeltaPartialTick(false),
                minecraft.level?.gameTime ?: 0L,
                color,
                h,
            )
        }
    }

    fun renderBeamOuter(
        stack: PoseStack,
        opaqueLayer: RenderType,
        translucentLayer: RenderType,
        color: Color,
        h: Double,
    ) {
        submitNodeStorage.submitCustomGeometry(stack, translucentLayer) { _, consumer ->
            BeaconBeamRenderer.renderBeamOuter(
                stack,
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
        renderType: RenderType,
        supplier: LinesBuilder.() -> Unit,
    ) {
        submitNodeStorage.submitCustomGeometry(stack, renderType) { pose, consumer ->
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
        renderType: RenderType,
        supplier: VertexBuilder.() -> Unit,
    ) {
        var first = true
        var px = 0.0
        var py = 0.0
        var pz = 0.0
        var pc = Color(0, true)
        var pw = 1.0

        renderLines(stack, renderType) {
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
}