package com.github.synnerz.devonian.utils.render.impl

import com.github.synnerz.devonian.utils.render.IRender3D
import com.github.synnerz.devonian.utils.render.Render3DTypes
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.gui.Font
import net.minecraft.client.renderer.StagedVertexBuffer
import net.minecraft.client.renderer.rendertype.RenderType
import net.minecraft.client.renderer.state.level.CameraRenderState
import net.minecraft.world.phys.shapes.VoxelShape
import java.awt.Color
import kotlin.math.sqrt

/**
 * you should never be calling methods here directly
 *
 * handles last second translations (e.g. `centered`) and deriving buffers
 * then delegates toward `Render3DVertex` to fill the buffer
 * does not handle translating for camera
 */
object Render3DState {
    lateinit var camera: CameraRenderState
    lateinit var poseStack: PoseStack
    lateinit var bufferSource: StagedVertexBuffer

    fun isOpaque(color: Color) = color.alpha == 255

    fun renderFilledShape(
        shape: VoxelShape,
        ox: Double,
        oy: Double,
        oz: Double,
        color: Color,
        phase: Boolean,
    ) {
        if (!::camera.isInitialized) return

        Render3DVertex.renderFilledShape(
            poseStack,
            BatchedRenderType.get(isOpaque(color), phase, BatchedRenderType.Primitive.QUADS),
            shape,
            ox, oy, oz,
            color,
        )
    }

    fun renderWireframeShape(
        shape: VoxelShape,
        ox: Double,
        oy: Double,
        oz: Double,
        color: Color,
        lineWidth: Double,
        phase: Boolean,
    ) {
        if (!::camera.isInitialized) return

        Render3DVertex.renderWireframeShape(
            poseStack,
            BatchedRenderType.get(isOpaque(color), phase, BatchedRenderType.Primitive.LINES),
            shape,
            ox, oy, oz,
            color,
            lineWidth,
        )
    }

    fun renderFilledBox(
        x: Double,
        y: Double,
        z: Double,
        w: Double,
        h: Double,
        color: Color,
        phase: Boolean,
        wz: Double,
        centered: Boolean
    ) {
        if (!::camera.isInitialized) return
        if (centered) return renderFilledBox(
            x - w * 0.5, y, z - wz * 0.5,
            w, h,
            color,
            phase,
            wz,
            false,
        )

        var x = x
        var y = y
        var z = z
        var w = w
        var wz = wz
        var h = h
        if (!phase) {
            x -= 0.003
            y -= 0.003
            z -= 0.003
            w += 0.006
            wz += 0.006
            h += 0.006
        }

        Render3DVertex.renderFilledBox(
            poseStack,
            BatchedRenderType.get(isOpaque(color), phase, BatchedRenderType.Primitive.TRIS),
            x, y, z,
            w, h, wz,
            color,
        )
    }

    fun renderWireframeBox(
        x: Double,
        y: Double,
        z: Double,
        w: Double,
        h: Double,
        color: Color,
        lineWidth: Double,
        phase: Boolean,
        wz: Double,
        centered: Boolean
    ) {
        if (!::camera.isInitialized) return
        if (centered) return renderWireframeBox(
            x - w * 0.5, y, z - wz * 0.5,
            w, h,
            color,
            lineWidth,
            phase,
            wz,
            false,
        )

        Render3DVertex.renderWireframeBox(
            poseStack,
            BatchedRenderType.get(isOpaque(color), phase, BatchedRenderType.Primitive.LINES),
            x, y, z,
            w, h, wz,
            color,
            lineWidth,
        )
    }

    fun renderString(
        str: String,
        x: Double,
        y: Double,
        z: Double,
        scale: Float,
        maxDist: Double,
        color: Color,
        backgroundBox: Color,
        phase: Boolean,
    ) {
        if (!::camera.isInitialized) return

        var scale = scale

        val dist = x * x + y * y + z * z
        if (dist > maxDist * maxDist) {
            val f = sqrt(dist) / maxDist
            scale = (scale * f).toFloat()
        }

        poseStack.pushPose()
        poseStack.last()
            .translate(x.toFloat(), y.toFloat(), z.toFloat())
            .rotate(camera.orientation)
            .scale(scale * 0.025f, -scale * 0.025f, scale * 0.025f)

        Render3DVertex.renderString(
            poseStack,
            if (phase) Font.DisplayMode.SEE_THROUGH else Font.DisplayMode.NORMAL,
            str,
            color,
            backgroundBox,
        )

        poseStack.popPose()
    }

    fun renderBeamInner(
        color: Color,
        phase: Boolean,
        h: Double
    ) {
        if (!::camera.isInitialized) return

        Render3DVertex.renderBeamInner(
            poseStack,
            BatchedRenderType.get(isOpaque(color), phase, BatchedRenderType.Primitive.BEACON),
            color,
            h,
        )
    }

    fun renderBeamOuter(
        color: Color,
        phase: Boolean,
        h: Double
    ) {
        if (!::camera.isInitialized) return

        Render3DVertex.renderBeamOuter(
            poseStack,
            BatchedRenderType.get(isOpaque(color), phase, BatchedRenderType.Primitive.BEACON),
            color,
            h,
        )
    }

    fun renderLines(
        opaque: Boolean,
        phase: Boolean,
        supplier: IRender3D.LinesBuilder.() -> Unit
    ) {
        if (!::camera.isInitialized) return

        Render3DVertex.renderLines(
            poseStack,
            BatchedRenderType.get(opaque, phase, BatchedRenderType.Primitive.LINES),
            supplier,
        )
    }

    fun renderLineStrip(
        opaque: Boolean,
        phase: Boolean,
        supplier: IRender3D.VertexBuilder.() -> Unit
    ) {
        if (!::camera.isInitialized) return

        Render3DVertex.renderLineStrip(
            poseStack,
            BatchedRenderType.get(opaque, phase, BatchedRenderType.Primitive.LINES),
            supplier,
        )
    }
}