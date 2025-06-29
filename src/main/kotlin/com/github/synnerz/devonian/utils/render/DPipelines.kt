package com.github.synnerz.devonian.utils.render

import com.mojang.blaze3d.pipeline.BlendFunction
import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.platform.DepthTestFunction
import com.mojang.blaze3d.vertex.VertexFormat
import net.minecraft.client.gl.RenderPipelines
import net.minecraft.client.render.VertexFormats

object DPipelines {
    val LINES = RenderPipeline.builder(RenderPipelines.RENDERTYPE_LINES_SNIPPET)
        .withLocation("devonian/lines")
        .withCull(false)
        .withBlend(BlendFunction.TRANSLUCENT)
        .withDepthWrite(true)
        .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
        .build()

    val LINES_ESP = RenderPipeline.builder(RenderPipelines.RENDERTYPE_LINES_SNIPPET)
        .withLocation("devonian/lines")
        .withCull(false)
        .withBlend(BlendFunction.TRANSLUCENT)
        .withDepthWrite(false)
        .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
        .build()

    val TRIANGLE_STRIP = RenderPipeline.builder(RenderPipelines.POSITION_COLOR_SNIPPET)
        .withLocation("devonian/triangle_strip")
        .withCull(false)
        .withVertexFormat(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.TRIANGLE_STRIP)
        .withDepthWrite(true)
        .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
        .withBlend(BlendFunction.TRANSLUCENT)
        .build()

    val TRIANGLE_STRIP_ESP = RenderPipeline.builder(RenderPipelines.POSITION_COLOR_SNIPPET)
        .withLocation("devonian/triangle_strip_esp")
        .withCull(false)
        .withVertexFormat(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.TRIANGLE_STRIP)
        .withDepthWrite(false)
        .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
        .withBlend(BlendFunction.TRANSLUCENT)
        .build()
}