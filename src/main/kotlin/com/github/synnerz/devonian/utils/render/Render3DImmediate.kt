package com.github.synnerz.devonian.utils.render

import com.github.synnerz.devonian.utils.render.impl.Render3DState
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.renderer.state.CameraRenderState
import net.minecraft.world.phys.shapes.VoxelShape
import java.awt.Color

/** *
 * handles early exits (i.a. alpha == 0)
 * handles camera translation viz. `translate`
 * then delegates to `Render3DState` to render
 */
object Render3DImmediate : IRender3D {
    lateinit var camera: CameraRenderState
    lateinit var poseStack: PoseStack

    override fun renderFilledShape(
        shape: VoxelShape,
        ox: Double,
        oy: Double,
        oz: Double,
        color: Color,
        phase: Boolean,
        translate: Boolean,
    ) {
        if (color.alpha == 0) return

        if (translate) {
            poseStack.pushPose()
            poseStack.translate(camera.pos.reverse())
        }

        Render3DState.renderFilledShape(shape, ox, oy, oz, color, phase)

        if (translate) poseStack.popPose()
    }

    override fun renderWireframeShape(
        shape: VoxelShape,
        ox: Double,
        oy: Double,
        oz: Double,
        color: Color,
        lineWidth: Double,
        phase: Boolean,
        translate: Boolean,
    ) {
        if (color.alpha == 0) return

        if (translate) {
            poseStack.pushPose()
            poseStack.translate(camera.pos.reverse())
        }

        Render3DState.renderWireframeShape(shape, ox, oy, oz, color, lineWidth, phase)

        if (translate) poseStack.popPose()
    }

    override fun renderFilledBox(
        x: Double,
        y: Double,
        z: Double,
        w: Double,
        h: Double,
        color: Color,
        phase: Boolean,
        translate: Boolean,
        wz: Double,
        centered: Boolean
    ) {
        if (color.alpha == 0) return

        if (translate) {
            poseStack.pushPose()
            poseStack.translate(camera.pos.reverse())
        }

        Render3DState.renderFilledBox(x, y, z, w, h, color, phase, wz, centered)

        if (translate) poseStack.popPose()
    }

    override fun renderWireframeBox(
        x: Double,
        y: Double,
        z: Double,
        w: Double,
        h: Double,
        color: Color,
        lineWidth: Double,
        phase: Boolean,
        translate: Boolean,
        wz: Double,
        centered: Boolean
    ) {
        if (color.alpha == 0) return

        if (translate) {
            poseStack.pushPose()
            poseStack.translate(camera.pos.reverse())
        }

        Render3DState.renderWireframeBox(x, y, z, w, h, color, lineWidth, phase, wz, centered)

        if (translate) poseStack.popPose()
    }

    override fun renderString(
        str: String,
        x: Double,
        y: Double,
        z: Double,
        scale: Float,
        maxDist: Double,
        color: Color,
        backgroundBox: Color,
        phase: Boolean,
        translate: Boolean
    ) {
        if (color.alpha == 0) return

        var x = x
        var y = y
        var z = z
        if (translate) {
            x -= camera.pos.x
            y -= camera.pos.y
            z -= camera.pos.z
        }

        Render3DState.renderString(str, x, y, z, scale, maxDist, color, backgroundBox, phase)
    }

    override fun renderBeam(
        x: Double,
        y: Double,
        z: Double,
        color: Color,
        phase: Boolean,
        translate: Boolean,
        maxY: Double,
        h: Double
    ) {
        if (color.alpha == 0) return

        poseStack.pushPose()
        if (translate) poseStack.translate(camera.pos.reverse())
        poseStack.translate(x, y, z)

        Render3DState.renderBeamInner(color, phase, h)
        Render3DState.renderBeamOuter(color, phase, h)

        poseStack.popPose()
    }

    override fun renderLines(
        opaque: Boolean,
        lineWidth: Double,
        phase: Boolean,
        translate: Boolean,
        supplier: IRender3D.LinesBuilder.() -> Unit
    ) {
        if (translate) {
            poseStack.pushPose()
            poseStack.translate(camera.pos.reverse())
        }

        Render3DState.renderLines(opaque, lineWidth, phase, supplier)

        if (translate) poseStack.popPose()
    }

    override fun renderLineStrip(
        opaque: Boolean,
        lineWidth: Double,
        phase: Boolean,
        translate: Boolean,
        supplier: IRender3D.VertexBuilder.() -> Unit
    ) {
        if (translate) {
            poseStack.pushPose()
            poseStack.translate(camera.pos.reverse())
        }

        Render3DState.renderLineStrip(opaque, lineWidth, phase, supplier)

        if (translate) poseStack.popPose()
    }
}