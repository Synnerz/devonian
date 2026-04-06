package com.github.synnerz.devonian.utils.render

//import com.mojang.blaze3d.pipeline.BlendFunction
import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.VertexFormat
import net.minecraft.client.renderer.RenderPipelines
import java.util.*

object Render3DPipelines {
    @JvmStatic
    val ALWAYS_PASS_RENDER_PIPELINES: MutableSet<RenderPipeline> = Collections.newSetFromMap(IdentityHashMap())

    val LINES_OPAQUE = RenderPipeline.builder(RenderPipelines.LINES_SNIPPET)
        .withLocation("devonian/lines_opaque")
        .withCull(false)
//        .withoutBlend()
//        .withDepthWrite(true)
//        .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
        .build().also { RenderPipelines.register(it) }

    val LINES_OPAQUE_ESP = RenderPipeline.builder(RenderPipelines.LINES_SNIPPET)
        .withLocation("devonian/lines_opaque_esp")
        .withCull(false)
//        .withoutBlend()
//        .withDepthWrite(true)
        .build().also { RenderPipelines.register(it) }
        .withDepthTestAlways()

    val LINES_TRANSLUCENT = RenderPipeline.builder(RenderPipelines.LINES_SNIPPET)
        .withLocation("devonian/lines_transparent")
        .withCull(false)
//        .withBlend(BlendFunction.TRANSLUCENT)
//        .withDepthWrite(false)
//        .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
        .build().also { RenderPipelines.register(it) }

    val LINES_TRANSLUCENT_ESP = RenderPipeline.builder(RenderPipelines.LINES_SNIPPET)
        .withLocation("devonian/lines_transparent_esp")
        .withCull(false)
//        .withBlend(BlendFunction.TRANSLUCENT)
//        .withDepthWrite(false)
//        .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
        .build().also { RenderPipelines.register(it) }

    val TRIANGLE_STRIP_OPAQUE = RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
        .withLocation("devonian/triangle_strip_opaque")
        .withCull(false)
        .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.TRIANGLE_STRIP)
//        .withDepthWrite(true)
//        .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
//        .withoutBlend()
        .build().also { RenderPipelines.register(it) }

    val TRIANGLE_STRIP_OPAQUE_ESP = RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
        .withLocation("devonian/triangle_strip_opaque_esp")
        .withCull(false)
        .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.TRIANGLE_STRIP)
//        .withDepthWrite(true)
//        .withoutBlend()
        .build().also { RenderPipelines.register(it) }
        .withDepthTestAlways()

    val TRIANGLE_STRIP_TRANSLUCENT = RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
        .withLocation("devonian/triangle_strip_translucent")
        .withCull(true)
        .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.TRIANGLE_STRIP)
//        .withDepthWrite(false)
//        .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
//        .withBlend(BlendFunction.TRANSLUCENT)
        .build().also { RenderPipelines.register(it) }

    val TRIANGLE_STRIP_TRANSLUCENT_ESP = RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
        .withLocation("devonian/triangle_strip_translucent_esp")
        .withCull(false)
        .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.TRIANGLE_STRIP)
//        .withDepthWrite(false)
//        .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
//        .withBlend(BlendFunction.TRANSLUCENT)
        .build().also { RenderPipelines.register(it) }

    val QUADS_OPAQUE = RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
        .withLocation("devonian/quads_opaque")
        .withCull(false)
        .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS)
//        .withDepthWrite(true)
//        .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
//        .withoutBlend()
        .build().also { RenderPipelines.register(it) }

    val QUADS_OPAQUE_ESP = RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
        .withLocation("devonian/quads_opaque_esp")
        .withCull(false)
        .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS)
//        .withDepthWrite(true)
//        .withoutBlend()
        .build().also { RenderPipelines.register(it) }
        .withDepthTestAlways()

    val QUADS_TRANSLUCENT = RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
        .withLocation("devonian/quads_translucent")
        .withCull(true)
        .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS)
//        .withDepthWrite(false)
//        .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
//        .withBlend(BlendFunction.TRANSLUCENT)
        .build().also { RenderPipelines.register(it) }

    val QUADS_TRANSLUCENT_ESP = RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
        .withLocation("devonian/quads_translucent_esp")
        .withCull(false)
        .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS)
//        .withDepthWrite(false)
//        .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
//        .withBlend(BlendFunction.TRANSLUCENT)
        .build().also { RenderPipelines.register(it) }

    val BEACON_BEAM_OPAQUE = RenderPipeline.builder(RenderPipelines.BEACON_BEAM_SNIPPET)
        .withLocation("devonian/beacon_beam_opaque")
//        .withDepthWrite(true)
//        .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
//        .withoutBlend()
        .build().also { RenderPipelines.register(it) }

    val BEACON_BEAM_OPAQUE_ESP = RenderPipeline.builder(RenderPipelines.BEACON_BEAM_SNIPPET)
        .withLocation("devonian/beacon_beam_opaque_esp")
//        .withDepthWrite(true)
//        .withoutBlend()
        .build().also { RenderPipelines.register(it) }
        .withDepthTestAlways()

    val BEACON_BEAM_TRANSLUCENT = RenderPipeline.builder(RenderPipelines.BEACON_BEAM_SNIPPET)
        .withLocation("devonian/beacon_beam_translucent")
//        .withDepthWrite(false)
//        .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
//        .withBlend(BlendFunction.TRANSLUCENT)
        .build().also { RenderPipelines.register(it) }

    val BEACON_BEAM_TRANSLUCENT_ESP = RenderPipeline.builder(RenderPipelines.BEACON_BEAM_SNIPPET)
        .withLocation("devonian/beacon_beam_translucent_esp")
//        .withDepthWrite(false)
//        .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
//        .withBlend(BlendFunction.TRANSLUCENT)
        .build().also { RenderPipelines.register(it) }

    fun RenderPipeline.withDepthTestAlways() = apply {
        ALWAYS_PASS_RENDER_PIPELINES.add(this)
    }
}