package com.github.synnerz.devonian.utils.render.impl

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
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
        pose: PoseStack.Pose,
        vertices: VertexConsumer,
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

        renderBeamLayer(
            pose,
            vertices,
            color.rgb,
            0f,
            height.toFloat(),
            0f,
            innerRadius,
            innerRadius,
            0f,
            -innerRadius,
            0f,
            0f,
            -innerRadius,
            0f, 1f,
            renderYOffset,
            animationStep,
        )
    }

    fun renderBeamOuter(
        pose: PoseStack.Pose,
        vertices: VertexConsumer,
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
            pose,
            vertices,
            color.rgb,
            height.toFloat(),
            0f,
            -outerRadius,
            -outerRadius,
            outerRadius,
            -outerRadius,
            -outerRadius,
            outerRadius,
            outerRadius,
            outerRadius,
            0f, 1f,
            renderYOffset,
            animationStep,
        )
    }

    fun renderBeamLayer(
        matrices: PoseStack.Pose,
        vertices: VertexConsumer,
        color: Int,
        y1: Float,
        y2: Float,
        x1: Float,
        z1: Float,
        x2: Float,
        z2: Float,
        x3: Float,
        z3: Float,
        x4: Float,
        z4: Float,
        u1: Float,
        u2: Float,
        v1: Float,
        v2: Float,
    ) {
        renderBeamFace(matrices, vertices, color, y1, y2, x1, z1, x2, z2, u1, u2, v1, v2)
        renderBeamFace(matrices, vertices, color, y1, y2, x4, z4, x3, z3, u1, u2, v1, v2)
        renderBeamFace(matrices, vertices, color, y1, y2, x2, z2, x4, z4, u1, u2, v1, v2)
        renderBeamFace(matrices, vertices, color, y1, y2, x3, z3, x1, z1, u1, u2, v1, v2)
    }

    fun renderBeamFace(
        matrix: PoseStack.Pose,
        vertices: VertexConsumer,
        color: Int,
        y1: Float,
        y2: Float,
        x1: Float,
        z1: Float,
        x2: Float,
        z2: Float,
        u1: Float,
        u2: Float,
        v1: Float,
        v2: Float,
    ) {
        renderBeamVertex(matrix, vertices, color, y2, x1, z1, u2, v1)
        renderBeamVertex(matrix, vertices, color, y1, x1, z1, u2, v2)
        renderBeamVertex(matrix, vertices, color, y1, x2, z2, u1, v2)
        renderBeamVertex(matrix, vertices, color, y2, x2, z2, u1, v1)
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