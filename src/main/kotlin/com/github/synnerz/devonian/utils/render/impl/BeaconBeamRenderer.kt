package com.github.synnerz.devonian.utils.render.impl

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import com.mojang.math.Axis
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.rendertype.RenderType
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.util.Mth
import java.awt.Color

object BeaconBeamRenderer {
    /**
     * - Edited version of renderBeam from mojang's code to allow for different Rendering Layers
     *  so that see through walls can be set
     * @author Mojang
     */
    fun renderBeamInner(
        matrices: PoseStack,
        bufferSource: MultiBufferSource,
        opaqueLayer: RenderType,
        translucentLayer: RenderType,
        partialTicks: Float,
        worldTime: Long,
        color: Color,
        height: Double,
    ) {
        val heightScale = 1f
        val innerRadius = 0.2f
        val time = Math.floorMod(worldTime, 40) + partialTicks
        val fixedTime = -time
        val wavePhase = Mth.frac(fixedTime * 0.2f - Mth.floor(fixedTime * 0.1f).toFloat())
        val animationStep = -1f + wavePhase
        val renderYOffset = height.toFloat() * heightScale * (0.5f / innerRadius) + animationStep

        matrices.pushPose()
        matrices.mulPose(Axis.YP.rotationDegrees(time * 2.25f - 45.0f))

        renderBeamLayer(
            matrices,
            bufferSource.getBuffer(if (color.alpha == 255) opaqueLayer else translucentLayer),
            color.rgb,
            0f,
            innerRadius,
            innerRadius,
            0f,
            -innerRadius,
            0f,
            0f,
            -innerRadius,
            renderYOffset,
            animationStep,
            height.toFloat(),
        )

        matrices.popPose()
    }

    fun renderBeamOuter(
        matrices: PoseStack,
        bufferSource: MultiBufferSource,
        opaqueLayer: RenderType,
        translucentLayer: RenderType,
        partialTicks: Float,
        worldTime: Long,
        color: Color,
        height: Double,
    ) {
        val heightScale = 1f
        val outerRadius = 0.25f
        val time = Math.floorMod(worldTime, 40) + partialTicks
        val fixedTime = -time
        val wavePhase = Mth.frac(fixedTime * 0.2f - Mth.floor(fixedTime * 0.1f).toFloat())
        val animationStep = -1f + wavePhase
        val renderYOffset = height.toFloat() * heightScale + animationStep

        renderBeamLayer(
            matrices,
            bufferSource.getBuffer(translucentLayer),
            ((color.alpha / 4) shl 24) or (color.rgb and 0x00FFFFFF),
            -outerRadius,
            -outerRadius,
            outerRadius,
            -outerRadius,
            -outerRadius,
            outerRadius,
            outerRadius,
            outerRadius,
            renderYOffset,
            animationStep,
            height.toFloat(),
        )
    }

    fun renderBeamLayer(
        matrices: PoseStack,
        vertices: VertexConsumer,
        color: Int,
        x1: Float,
        z1: Float,
        x2: Float,
        z2: Float,
        x3: Float,
        z3: Float,
        x4: Float,
        z4: Float,
        v1: Float,
        v2: Float,
        h: Float,
    ) {
        val entry = matrices.last()
        renderBeamFace(entry, vertices, color, x1, z1, x2, z2, v1, v2, h)
        renderBeamFace(entry, vertices, color, x4, z4, x3, z3, v1, v2, h)
        renderBeamFace(entry, vertices, color, x2, z2, x4, z4, v1, v2, h)
        renderBeamFace(entry, vertices, color, x3, z3, x1, z1, v1, v2, h)
    }

    fun renderBeamFace(
        matrix: PoseStack.Pose,
        vertices: VertexConsumer,
        color: Int,
        x1: Float,
        z1: Float,
        x2: Float,
        z2: Float,
        v1: Float,
        v2: Float,
        h: Float,
    ) {
        renderBeamVertex(matrix, vertices, color, h, x1, z1, 1f, v1)
        renderBeamVertex(matrix, vertices, color, 0f, x1, z1, 1f, v2)
        renderBeamVertex(matrix, vertices, color, 0f, x2, z2, 0f, v2)
        renderBeamVertex(matrix, vertices, color, h, x2, z2, 0f, v1)
    }

    fun renderBeamVertex(
        matrix: PoseStack.Pose,
        vertices: VertexConsumer,
        color: Int,
        y: Float,
        x: Float,
        z: Float,
        u: Float,
        v: Float
    ) {
        vertices
            .addVertex(matrix, x, y, z)
            .setColor(color)
            .setUv(u, v)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(15728880)
            .setNormal(matrix, 0.0f, 1.0f, 0.0f)
    }
}