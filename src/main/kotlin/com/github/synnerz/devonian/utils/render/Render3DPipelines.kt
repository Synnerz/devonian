package com.github.synnerz.devonian.utils.render

import com.mojang.blaze3d.PrimitiveTopology
import com.mojang.blaze3d.pipeline.BlendFunction
import com.mojang.blaze3d.pipeline.ColorTargetState
import com.mojang.blaze3d.pipeline.DepthStencilState
import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.platform.CompareOp
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import net.minecraft.client.renderer.RenderPipelines
import java.util.*

object Render3DPipelines {
    @JvmStatic
    val ALWAYS_PASS_RENDER_PIPELINES: MutableSet<RenderPipeline> = Collections.newSetFromMap(IdentityHashMap())

    val LINES_OPAQUE = RenderPipeline.builder(RenderPipelines.LINES_SNIPPET)
        .withLocation("devonian/lines_opaque")
        .withCull(false)
        .build().also { RenderPipelines.register(it) }

    val LINES_OPAQUE_ESP = RenderPipeline.builder(RenderPipelines.LINES_SNIPPET)
        .withLocation("devonian/lines_opaque_esp")
        .withCull(false)
        .withColorTargetState(ColorTargetState(BlendFunction.TRANSLUCENT))
        .withDepthStencilState(DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, true))
        .build().also { RenderPipelines.register(it) }
        .withDepthTestAlways()

    val LINES_TRANSLUCENT = RenderPipeline.builder(RenderPipelines.LINES_SNIPPET)
        .withLocation("devonian/lines_transparent")
        .withCull(false)
        .withColorTargetState(ColorTargetState(BlendFunction.TRANSLUCENT))
        .build().also { RenderPipelines.register(it) }

    val LINES_TRANSLUCENT_ESP = RenderPipeline.builder(RenderPipelines.LINES_SNIPPET)
        .withLocation("devonian/lines_transparent_esp")
        .withCull(false)
        .withColorTargetState(ColorTargetState(BlendFunction.TRANSLUCENT))
        .withDepthStencilState(DepthStencilState(CompareOp.NOT_EQUAL, false))
        .build().also { RenderPipelines.register(it) }

    val TRIANGLE_STRIP_OPAQUE = RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
        .withLocation("devonian/triangle_strip_opaque")
        .withCull(false)
        .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR)
        .withPrimitiveTopology(PrimitiveTopology.TRIANGLE_STRIP)
        .build().also { RenderPipelines.register(it) }

    val TRIANGLE_STRIP_OPAQUE_ESP = RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
        .withLocation("devonian/triangle_strip_opaque_esp")
        .withCull(false)
        .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR)
        .withPrimitiveTopology(PrimitiveTopology.TRIANGLE_STRIP)
        .withDepthStencilState(DepthStencilState(CompareOp.NOT_EQUAL, false))
        .build().also { RenderPipelines.register(it) }
        .withDepthTestAlways()

    val TRIANGLE_STRIP_TRANSLUCENT = RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
        .withLocation("devonian/triangle_strip_translucent")
        .withCull(true)
        .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR)
        .withPrimitiveTopology(PrimitiveTopology.TRIANGLE_STRIP)
        .withColorTargetState(ColorTargetState(BlendFunction.TRANSLUCENT))
        .build().also { RenderPipelines.register(it) }

    val TRIANGLE_STRIP_TRANSLUCENT_ESP = RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
        .withLocation("devonian/triangle_strip_translucent_esp")
        .withCull(false)
        .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR)
        .withPrimitiveTopology(PrimitiveTopology.TRIANGLE_STRIP)
        .withDepthStencilState(DepthStencilState(CompareOp.NOT_EQUAL, false))
        .withColorTargetState(ColorTargetState(BlendFunction.TRANSLUCENT))
        .build().also { RenderPipelines.register(it) }

    val QUADS_OPAQUE = RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
        .withLocation("devonian/quads_opaque")
        .withCull(false)
        .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR)
        .withPrimitiveTopology(PrimitiveTopology.QUADS)
        .build().also { RenderPipelines.register(it) }

    val QUADS_OPAQUE_ESP = RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
        .withLocation("devonian/quads_opaque_esp")
        .withCull(false)
        .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR)
        .withPrimitiveTopology(PrimitiveTopology.QUADS)
        .withDepthStencilState(DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, true))
        .build().also { RenderPipelines.register(it) }
        .withDepthTestAlways()

    val QUADS_TRANSLUCENT = RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
        .withLocation("devonian/quads_translucent")
        .withCull(true)
        .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR)
        .withPrimitiveTopology(PrimitiveTopology.QUADS)
        .withColorTargetState(ColorTargetState(BlendFunction.TRANSLUCENT))
        .build().also { RenderPipelines.register(it) }

    val QUADS_TRANSLUCENT_ESP = RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
        .withLocation("devonian/quads_translucent_esp")
        .withCull(false)
        .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR)
        .withPrimitiveTopology(PrimitiveTopology.QUADS)
        .withDepthStencilState(DepthStencilState(CompareOp.NOT_EQUAL, false))
        .withColorTargetState(ColorTargetState(BlendFunction.TRANSLUCENT))
        .build().also { RenderPipelines.register(it) }

    val BEACON_BEAM_OPAQUE = RenderPipeline.builder(RenderPipelines.BEACON_BEAM_SNIPPET)
        .withLocation("devonian/beacon_beam_opaque")
        .withDepthStencilState(DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, true))
        .build().also { RenderPipelines.register(it) }

    val BEACON_BEAM_OPAQUE_ESP = RenderPipeline.builder(RenderPipelines.BEACON_BEAM_SNIPPET)
        .withLocation("devonian/beacon_beam_opaque_esp")
        .withDepthStencilState(DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, true))
        .build().also { RenderPipelines.register(it) }
        .withDepthTestAlways()

    val BEACON_BEAM_TRANSLUCENT = RenderPipeline.builder(RenderPipelines.BEACON_BEAM_SNIPPET)
        .withLocation("devonian/beacon_beam_translucent")
        .withColorTargetState(ColorTargetState(BlendFunction.TRANSLUCENT))
        .withDepthStencilState(DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, false))
        .build().also { RenderPipelines.register(it) }

    val BEACON_BEAM_TRANSLUCENT_ESP = RenderPipeline.builder(RenderPipelines.BEACON_BEAM_SNIPPET)
        .withLocation("devonian/beacon_beam_translucent_esp")
        .withColorTargetState(ColorTargetState(BlendFunction.TRANSLUCENT))
        .withDepthStencilState(DepthStencilState(CompareOp.NOT_EQUAL, false))
        .build().also { RenderPipelines.register(it) }

    fun RenderPipeline.withDepthTestAlways() = apply {
        ALWAYS_PASS_RENDER_PIPELINES.add(this)
    }
}