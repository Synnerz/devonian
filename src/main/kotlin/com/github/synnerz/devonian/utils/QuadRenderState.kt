package com.github.synnerz.devonian.utils

import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.gui.navigation.ScreenRectangle
import net.minecraft.client.gui.render.TextureSetup
import net.minecraft.client.gui.render.state.GuiElementRenderState
import org.joml.Matrix3x2f
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

class QuadRenderState(
    val pipeline: RenderPipeline,
    val pose: Matrix3x2f,
    val x00: Float,
    val y00: Float,
    val x01: Float,
    val y01: Float,
    val x10: Float,
    val y10: Float,
    val x11: Float,
    val y11: Float,
    val color: Int,
    val scissorArea: ScreenRectangle? = null
) : GuiElementRenderState {
    constructor(
        pipeline: RenderPipeline,
        pose: Matrix3x2f,
        x0: Float,
        y0: Float,
        x1: Float,
        y1: Float,
        color: Int,
        scissorArea: ScreenRectangle? = null
    ) : this(
        pipeline,
        pose,
        x0, y0,
        x0, y1,
        x1, y0,
        x1, y1,
        color,
        scissorArea
    )

    val bounds: ScreenRectangle

    init {
        val l = min(min(x00, x01), min(x10, x11)).toInt()
        var r = ceil(max(max(x00, x01), max(x10, x11))).toInt()
        val t = min(min(y00, y01), min(y10, y11)).toInt()
        var b = ceil(max(max(y00, y01), max(y10, y11))).toInt()
        bounds = ScreenRectangle(
            l, t,
            r - l, b - t
        ).transformMaxBounds(pose)
    }

    override fun buildVertices(vertexConsumer: VertexConsumer) {
        vertexConsumer.addVertexWith2DPose(pose, x00, y00)
            .setColor(color)
        vertexConsumer.addVertexWith2DPose(pose, x01, y01)
            .setColor(color)
        vertexConsumer.addVertexWith2DPose(pose, x11, y11)
            .setColor(color)
        vertexConsumer.addVertexWith2DPose(pose, x10, y10)
            .setColor(color)
    }

    override fun pipeline(): RenderPipeline = pipeline
    override fun textureSetup(): TextureSetup = TextureSetup.noTexture()
    override fun scissorArea(): ScreenRectangle? = scissorArea
    override fun bounds(): ScreenRectangle = bounds
}