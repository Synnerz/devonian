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

    private enum class Types(private val arr: Array<RenderType>) {
        LINES(
            arrayOf(
                Render3DTypes.LINES_TRANSLUCENT,
                Render3DTypes.LINES_TRANSLUCENT_ESP,
                Render3DTypes.LINES_OPAQUE,
                Render3DTypes.LINES_OPAQUE_ESP,
            )
        ),
        TRIS(
            arrayOf(
                Render3DTypes.TRIANGLE_STRIP_TRANSLUCENT,
                Render3DTypes.TRIANGLE_STRIP_TRANSLUCENT_ESP,
                Render3DTypes.TRIANGLE_STRIP_OPAQUE,
                Render3DTypes.TRIANGLE_STRIP_OPAQUE_ESP,
            )
        ),
        QUADS(
            arrayOf(
                Render3DTypes.QUADS_TRANSLUCENT,
                Render3DTypes.QUADS_TRANSLUCENT_ESP,
                Render3DTypes.QUADS_OPAQUE,
                Render3DTypes.QUADS_OPAQUE_ESP,
            )
        ),
        BEACON(
            arrayOf(
                Render3DTypes.BEACON_BEAM_TRANSLUCENT,
                Render3DTypes.BEACON_BEAM_TRANSLUCENT_ESP,
                Render3DTypes.BEACON_BEAM_OPAQUE,
                Render3DTypes.BEACON_BEAM_OPAQUE_ESP,
            )
        );

        fun get(opaque: Boolean, phase: Boolean) = arr[
            (if (opaque) 2 else 0) +
                    (if (phase) 1 else 0)
        ]
    }

    fun renderFilledShape(
        shape: VoxelShape,
        ox: Double,
        oy: Double,
        oz: Double,
        color: Color,
        phase: Boolean,
    ) {
        if (!::camera.isInitialized) return

        val type = Types.QUADS.get(color.alpha == 255, phase)

        Render3DVertex.renderFilledShape(
            poseStack,
            type,
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

        val type = Types.LINES.get(color.alpha == 255, phase)

        Render3DVertex.renderWireframeShape(
            poseStack,
            type,
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

        val type = Types.TRIS.get(color.alpha == 255, phase)

        Render3DVertex.renderFilledBox(
            poseStack,
            type,
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

        val type = Types.LINES.get(color.alpha == 255, phase)

        Render3DVertex.renderWireframeBox(
            poseStack,
            type,
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

        Render3DVertex.renderString(
            camera,
            poseStack,
            if (phase) Font.DisplayMode.SEE_THROUGH else Font.DisplayMode.NORMAL,
            str,
            x, y, z,
            scale,
            maxDist,
            color,
            backgroundBox,
        )
    }

    fun renderBeamInner(
        color: Color,
        phase: Boolean,
        h: Double
    ) {
        if (!::camera.isInitialized) return

        Render3DVertex.renderBeamInner(
            poseStack,
            Types.BEACON.get(true, phase),
            Types.BEACON.get(false, phase),
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
            Types.BEACON.get(true, phase),
            Types.BEACON.get(false, phase),
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

        val type = Types.LINES.get(opaque, phase)

        Render3DVertex.renderLines(
            poseStack,
            type,
            supplier,
        )
    }

    fun renderLineStrip(
        opaque: Boolean,
        phase: Boolean,
        supplier: IRender3D.VertexBuilder.() -> Unit
    ) {
        if (!::camera.isInitialized) return

        val type = Types.LINES.get(opaque, phase)

        Render3DVertex.renderLineStrip(
            poseStack,
            type,
            supplier,
        )
    }
}