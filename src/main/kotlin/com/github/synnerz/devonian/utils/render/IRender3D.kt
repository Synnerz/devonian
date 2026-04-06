package com.github.synnerz.devonian.utils.render

import com.github.synnerz.devonian.Devonian
import net.minecraft.world.phys.Vec3
import net.minecraft.world.phys.shapes.VoxelShape
import org.joml.Vector3f
import java.awt.Color
import kotlin.math.sqrt

interface IRender3D {
    fun renderFilledShape(
        shape: VoxelShape,
        ox: Double,
        oy: Double,
        oz: Double,
        color: Color,
        phase: Boolean = false,
        translate: Boolean = true,
    )

    fun renderWireframeShape(
        shape: VoxelShape,
        ox: Double,
        oy: Double,
        oz: Double,
        color: Color,
        lineWidth: Double = 1.0,
        phase: Boolean = false,
        translate: Boolean = true,
    )

    fun renderFilledBox(
        x: Double,
        y: Double,
        z: Double,
        w: Double,
        h: Double,
        color: Color,
        phase: Boolean = false,
        translate: Boolean = true,
        wz: Double = w,
        centered: Boolean = false,
    )

    fun renderWireframeBox(
        x: Double,
        y: Double,
        z: Double,
        w: Double,
        h: Double,
        color: Color,
        lineWidth: Double = 1.0,
        phase: Boolean = false,
        translate: Boolean = true,
        wz: Double = w,
        centered: Boolean = false,
    )

    /**
     * @param maxDist maximum distance, after which the string will not shrink
     * @param backgroundBox 100% alpha is not full opacity, but max opacity (0.25)
     */
    fun renderString(
        str: String,
        x: Double,
        y: Double,
        z: Double,
        scale: Float = 1f,
        maxDist: Double = 8.0,
        color: Color = Color.WHITE,
        backgroundBox: Color = Color(0, true),
        phase: Boolean = false,
        translate: Boolean = true,
    )

    fun renderBeam(
        x: Double,
        y: Double,
        z: Double,
        color: Color,
        phase: Boolean = false,
        translate: Boolean = true,
        maxY: Double = 320.0,
        h: Double = maxY - y,
    )

    fun renderBeam(
        x: Int,
        y: Int,
        z: Int,
        color: Color,
        phase: Boolean = false,
        translate: Boolean = true,
        maxY: Double = 320.0,
        h: Double = maxY - y,
    ) = renderBeam(x + 0.5, y.toDouble(), z + 0.5, color, phase, translate, maxY, h)

    fun renderLines(
        opaque: Boolean = false,
        phase: Boolean = false,
        translate: Boolean = true,
        supplier: LinesBuilder.() -> Unit,
    )

    fun renderLineStrip(
        opaque: Boolean = false,
        phase: Boolean = false,
        translate: Boolean = true,
        supplier: VertexBuilder.() -> Unit,
    )

    fun renderLine(
        x0: Double, y0: Double, z0: Double,
        x1: Double, y1: Double, z1: Double,
        c0: Color,
        lineWidth0: Double = 1.0,
        phase: Boolean = false,
        translate: Boolean = true,
        c1: Color = c0,
        lineWidth1: Double = lineWidth0,
    ) = renderLines(c0.alpha == 255 && c1.alpha == 255, phase, translate) {
        submit(x0, y0, z0, x1, y1, z1, c0, c1, lineWidth0, lineWidth1)
    }

    fun renderLine(
        p0: Vec3,
        p1: Vec3,
        c0: Color,
        lineWidth: Double = 1.0,
        phase: Boolean = false,
        translate: Boolean = true,
        c1: Color = c0,
    ) = renderLines(c0.alpha == 255 && c1.alpha == 255, phase, translate) {
        submit(p0, p1, c0, c1, lineWidth)
    }

    /**
     * @param c0 color of line starting at crosshair
     * @param c1 color of line ending at target
     */
    fun renderTracer(
        x: Double, y: Double, z: Double,
        c0: Color,
        lineWidth: Double = 1.0,
        relative: Boolean = false,
        phase: Boolean = false,
        translate: Boolean = true,
        c1: Color = c0,
    ) {
        val cam = Devonian.minecraft.gameRenderer.gameRenderState.levelRenderState.cameraRenderState
        val look = cam.orientation.transform(Vector3f(0f, 0f, -1f))
        val pos = Vec3(x, y, z)
        val camPos = cam.pos
        renderLine(
            camPos.x + look.x,
            camPos.y + look.y,
            camPos.z + look.z,
            if (relative) camPos.x + pos.x else pos.x,
            if (relative) camPos.y + pos.y else pos.y,
            if (relative) camPos.z + pos.z else pos.z,
            c0,
            lineWidth,
            phase,
            translate,
            c1,
        )
    }

    fun renderWaypoint(
        x: Double,
        y: Double,
        z: Double,
        color: Color,
        phase: Boolean = false,
        translate: Boolean = true,

        lineWidth: Double = 1.0,
        centered: Boolean = false,

        title: String? = null,
        showTitleFarther: Double = 10.0,
        textScale: Float = 1f,
        textMaxDist: Double = 8.0,
        textColor: Color = Color.WHITE,
        textBackgroundBox: Color = Color(0, true),

        beacon: Boolean = true,
        beaconMaxY: Double = 320.0,
        beaconH: Double = beaconMaxY - y,

    ) {
        if (color.alpha == 0) return

        if (!centered) return renderWaypoint(
            x + 0.5, y, z + 0.5,
            color, phase, translate,
            lineWidth, true,
            title, showTitleFarther, textScale, textMaxDist, textColor, textBackgroundBox,
            beacon, beaconMaxY, beaconH,
        )

        val pos = Devonian.minecraft.player ?: return
        val dx = x - pos.x
        val dy = y + 2 - pos.y
        val dz = z - pos.z

        renderFilledBox(
            x, y, z,
            1.0, 1.0,
            Color(color.red, color.green, color.blue, color.alpha / 3),
            phase, translate,
            centered = true,
        )
        renderWireframeBox(
            x, y, z,
            1.0, 1.0,
            color, lineWidth,
            phase, translate,
            centered = true,
        )
        if (beacon) renderBeam(
            x, y + 1, z,
            color, phase, translate,
            h = beaconH,
        )

        val dist = sqrt(dx * dx + dy * dy + dz * dz)
        if (dist > showTitleFarther) renderString(
            title ?: "%.2fm".format(dist),
            x, y + 2, z,
            textScale, textMaxDist, textColor, textBackgroundBox,
            phase, translate,
        )
    }

    interface LinesBuilder {
        fun submit(
            x0: Double, y0: Double, z0: Double,
            x1: Double, y1: Double, z1: Double,
            c0: Color,
            c1: Color = c0,
            lineWidth0: Double,
            lineWidth1: Double = lineWidth0,
        )

        fun submit(
            x0: Double, y0: Double, z0: Double,
            x1: Double, y1: Double, z1: Double,
            c0: Color,
            c1: Color = c0,
        ) = submit(x0, y0, z0, x1, y1, z1, c0, c1, 1.0, 1.0)

        fun submit(
            p0: Vec3,
            p1: Vec3,
            c0: Color,
            c1: Color = c0,
            lineWidth0: Double,
            lineWidth1: Double = lineWidth0,
        ) = submit(
            p0.x, p0.y, p0.z,
            p1.x, p1.y, p1.z,
            c0, c1, lineWidth0, lineWidth1,
        )

        fun submit(
            p0: Vec3,
            p1: Vec3,
            c0: Color,
            c1: Color = c0,
        ) = submit(
            p0.x, p0.y, p0.z,
            p1.x, p1.y, p1.z,
            c0, c1,
        )
    }

    interface VertexBuilder {
        fun submit(
            x: Double, y: Double, z: Double,
            c: Color, lineWidth: Double,
        )

        fun submit(
            x: Double, y: Double, z: Double,
            c: Color,
        ) = submit(x, y, z, c, 1.0)

        fun submit(p: Vec3, c: Color, lineWidth: Double) = submit(p.x, p.y, p.z, c, lineWidth)

        fun submit(p: Vec3, c: Color) = submit(p.x, p.y, p.z, c)

        fun endBatch()
    }
}