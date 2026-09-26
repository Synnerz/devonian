package com.github.synnerz.devonian.utils.render

import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.renderpearl.api.pipeline.BlendFunction
import com.mojang.renderpearl.api.pipeline.ColorTargetState
import com.mojang.renderpearl.api.pipeline.CompareOp
import com.mojang.renderpearl.api.pipeline.DepthStencilState
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology
import com.mojang.renderpearl.api.pipeline.RenderPipeline
import net.minecraft.client.renderer.RenderPipelines

object Render3DPipelines {
    // mc checks for "transparency" by the existence of a blend function
    // val BLEND_REPLACE = ColorTargetState(BlendFunction(BlendFactor.ONE, BlendFactor.ZERO, BlendFactor.ONE, BlendFactor.ZERO))
    val BLEND_REPLACE = ColorTargetState.DEFAULT
    val BLEND_TRANSPARENT = ColorTargetState(BlendFunction.TRANSLUCENT)

    val LINES_OPAQUE = RenderPipeline.builder(RenderPipelines.LINES_SNIPPET)
        .withLocation("devonian/lines_opaque")
        .withCull(false)
        .withColorTargetState(BLEND_REPLACE)
        .withDepthStencilState(DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, true))
        .build().also { RenderPipelines.register(it) }

    val LINES_OPAQUE_ESP = RenderPipeline.builder(RenderPipelines.LINES_SNIPPET)
        .withLocation("devonian/lines_opaque_esp")
        .withCull(false)
        .withColorTargetState(BLEND_REPLACE)
        .withDepthStencilState(DepthStencilState(CompareOp.ALWAYS_PASS, true))
        .build().also { RenderPipelines.register(it) }

    val LINES_TRANSLUCENT = RenderPipeline.builder(RenderPipelines.LINES_SNIPPET)
        .withLocation("devonian/lines_transparent")
        .withCull(false)
        .withColorTargetState(BLEND_TRANSPARENT)
        .withDepthStencilState(DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
        .build().also { RenderPipelines.register(it) }

    val LINES_TRANSLUCENT_ESP = RenderPipeline.builder(RenderPipelines.LINES_SNIPPET)
        .withLocation("devonian/lines_transparent_esp")
        .withCull(false)
        .withColorTargetState(BLEND_TRANSPARENT)
        .withDepthStencilState(DepthStencilState(CompareOp.ALWAYS_PASS, false))
        .build().also { RenderPipelines.register(it) }

    val TRIANGLES_OPAQUE = RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
        .withLocation("devonian/triangles_opaque")
        .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR)
        .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
        .withCull(true)
        .withColorTargetState(BLEND_REPLACE)
        .withDepthStencilState(DepthStencilState(CompareOp.GREATER_THAN, true))
        .build().also { RenderPipelines.register(it) }

    val TRIANGLES_OPAQUE_ESP = RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
        .withLocation("devonian/triangles_opaque_esp")
        .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR)
        .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
        .withCull(true)
        .withColorTargetState(BLEND_REPLACE)
        .withDepthStencilState(DepthStencilState(CompareOp.NOT_EQUAL, true))
        .build().also { RenderPipelines.register(it) }

    val TRIANGLES_TRANSLUCENT = RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
        .withLocation("devonian/triangles_translucent")
        .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR)
        .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
        .withCull(true)
        .withColorTargetState(BLEND_TRANSPARENT)
        .withDepthStencilState(DepthStencilState(CompareOp.GREATER_THAN, false))
        .build().also { RenderPipelines.register(it) }

    val TRIANGLES_TRANSLUCENT_ESP = RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
        .withLocation("devonian/triangles_translucent_esp")
        .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR)
        .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
        .withCull(true)
        .withColorTargetState(BLEND_TRANSPARENT)
        .withDepthStencilState(DepthStencilState(CompareOp.NOT_EQUAL, false))
        .build().also { RenderPipelines.register(it) }

    val QUADS_OPAQUE = RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
        .withLocation("devonian/quads_opaque")
        .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR)
        .withPrimitiveTopology(PrimitiveTopology.QUADS)
        .withCull(true)
        .withColorTargetState(BLEND_REPLACE)
        .withDepthStencilState(DepthStencilState(CompareOp.GREATER_THAN, true))
        .build().also { RenderPipelines.register(it) }

    val QUADS_OPAQUE_ESP = RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
        .withLocation("devonian/quads_opaque_esp")
        .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR)
        .withPrimitiveTopology(PrimitiveTopology.QUADS)
        .withCull(true)
        .withColorTargetState(BLEND_REPLACE)
        .withDepthStencilState(DepthStencilState(CompareOp.NOT_EQUAL, true))
        .build().also { RenderPipelines.register(it) }

    val QUADS_TRANSLUCENT = RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
        .withLocation("devonian/quads_translucent")
        .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR)
        .withPrimitiveTopology(PrimitiveTopology.QUADS)
        .withCull(true)
        .withColorTargetState(BLEND_TRANSPARENT)
        .withDepthStencilState(DepthStencilState(CompareOp.GREATER_THAN, false))
        .build().also { RenderPipelines.register(it) }

    val QUADS_TRANSLUCENT_ESP = RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
        .withLocation("devonian/quads_translucent_esp")
        .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR)
        .withPrimitiveTopology(PrimitiveTopology.QUADS)
        .withCull(true)
        .withColorTargetState(BLEND_TRANSPARENT)
        .withDepthStencilState(DepthStencilState(CompareOp.NOT_EQUAL, false))
        .build().also { RenderPipelines.register(it) }

    val BEACON_BEAM_OPAQUE = RenderPipeline.builder(RenderPipelines.BEACON_BEAM_SNIPPET)
        .withLocation("devonian/beacon_beam_opaque")
        .withCull(true)
        .withColorTargetState(BLEND_REPLACE)
        .withDepthStencilState(DepthStencilState(CompareOp.GREATER_THAN, true))
        .build().also { RenderPipelines.register(it) }

    val BEACON_BEAM_OPAQUE_ESP = RenderPipeline.builder(RenderPipelines.BEACON_BEAM_SNIPPET)
        .withLocation("devonian/beacon_beam_opaque_esp")
        .withCull(true)
        .withColorTargetState(BLEND_REPLACE)
        .withDepthStencilState(DepthStencilState(CompareOp.NOT_EQUAL, true))
        .build().also { RenderPipelines.register(it) }

    val BEACON_BEAM_TRANSLUCENT = RenderPipeline.builder(RenderPipelines.BEACON_BEAM_SNIPPET)
        .withLocation("devonian/beacon_beam_translucent")
        .withCull(true)
        .withColorTargetState(BLEND_TRANSPARENT)
        .withDepthStencilState(DepthStencilState(CompareOp.GREATER_THAN, false))
        .build().also { RenderPipelines.register(it) }

    val BEACON_BEAM_TRANSLUCENT_ESP = RenderPipeline.builder(RenderPipelines.BEACON_BEAM_SNIPPET)
        .withLocation("devonian/beacon_beam_translucent_esp")
        .withCull(true)
        .withColorTargetState(BLEND_TRANSPARENT)
        .withDepthStencilState(DepthStencilState(CompareOp.NOT_EQUAL, false))
        .build().also { RenderPipelines.register(it) }

    val TEXT = RenderPipeline.builder(RenderPipelines.WORLD_TEXT_SNIPPET)
        .withLocation("devonian/text")
        .withVertexShader("core/text")
        .withFragmentShader("core/text")
        .withCull(false)
        .withColorTargetState(BLEND_TRANSPARENT)
        .withDepthStencilState(DepthStencilState(CompareOp.GREATER_THAN, true))
        .build().also { RenderPipelines.register(it) }

    val TEXT_ESP = RenderPipeline.builder(RenderPipelines.WORLD_TEXT_SNIPPET)
        .withLocation("devonian/text_esp")
        .withVertexShader("core/text")
        .withFragmentShader("core/text")
        .withCull(false)
        .withColorTargetState(BLEND_TRANSPARENT)
        .withDepthStencilState(DepthStencilState(CompareOp.NOT_EQUAL, true))
        .build().also { RenderPipelines.register(it) }
}