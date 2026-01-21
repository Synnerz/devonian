package com.github.synnerz.devonian.utils.render

import net.minecraft.client.renderer.RenderStateShard
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.blockentity.BeaconRenderer
import java.util.*
import kotlin.math.ceil

object Render3DTypes {
    private data class LineLayerKey(val lineWidth: Double, val phase: Boolean, val opaque: Boolean)
    private val cachedLineLayers = mutableMapOf<LineLayerKey, RenderType.CompositeRenderType>()

    fun lines(lineWidth: Double = 1.0, phase: Boolean = false, opaque: Boolean): RenderType.CompositeRenderType {
        val lineWidth = ceil(lineWidth * 10.0) / 10.0
        return cachedLineLayers.getOrPut(LineLayerKey(lineWidth, phase, opaque)) {
            val name = if (phase) "lines_esp_${lineWidth}_$opaque" else "lines_${lineWidth}_$opaque"
            val lw = RenderStateShard.LineStateShard(OptionalDouble.of(lineWidth))
            RenderType.create(
                "devonian/$name",
                1536,
                false,
                !opaque,
                if (phase) {
                    if (opaque) Render3DPipelines.LINES_OPAQUE_ESP
                    else Render3DPipelines.LINES_TRANSLUCENT_ESP
                } else {
                    if (opaque) Render3DPipelines.LINES_OPAQUE
                    else Render3DPipelines.LINES_TRANSLUCENT
                },
                RenderType.CompositeState
                    .builder()
                    .setLineState(lw)
                    .createCompositeState(false)
            )
        }
    }

    val TRIANGLE_STRIP_OPAQUE = RenderType.create(
        "devonian/triangle_strip_opaque",
        1536,
        false,
        false,
        Render3DPipelines.TRIANGLE_STRIP_OPAQUE,
        RenderType.CompositeState
            .builder()
            .createCompositeState(false)
    )

    val TRIANGLE_STRIP_OPAQUE_ESP = RenderType.create(
        "devonian/triangle_strip_opaque_esp",
        1536,
        false,
        false,
        Render3DPipelines.TRIANGLE_STRIP_OPAQUE_ESP,
        RenderType.CompositeState
            .builder()
            .createCompositeState(false)
    )

    val TRIANGLE_STRIP_TRANSLUCENT = RenderType.create(
        "devonian/triangle_strip_translucent",
        1536,
        false,
        true,
        Render3DPipelines.TRIANGLE_STRIP_TRANSLUCENT,
        RenderType.CompositeState
            .builder()
            .createCompositeState(false)
    )

    val TRIANGLE_STRIP_TRANSLUCENT_ESP = RenderType.create(
        "devonian/triangle_strip_translucent_esp",
        1536,
        false,
        true,
        Render3DPipelines.TRIANGLE_STRIP_TRANSLUCENT_ESP,
        RenderType.CompositeState
            .builder()
            .createCompositeState(false)
    )

    val QUADS_OPAQUE = RenderType.create(
        "devonian/quads_opaque",
        1536,
        false,
        false,
        Render3DPipelines.QUADS_OPAQUE,
        RenderType.CompositeState
            .builder()
            .createCompositeState(false)
    )

    val QUADS_OPAQUE_ESP = RenderType.create(
        "devonian/quads_opaque_esp",
        1536,
        false,
        false,
        Render3DPipelines.QUADS_OPAQUE_ESP,
        RenderType.CompositeState
            .builder()
            .createCompositeState(false)
    )

    val QUADS_TRANSLUCENT = RenderType.create(
        "devonian/quads_translucent",
        1536,
        false,
        true,
        Render3DPipelines.QUADS_TRANSLUCENT,
        RenderType.CompositeState
            .builder()
            .createCompositeState(false)
    )

    val QUADS_TRANSLUCENT_ESP = RenderType.create(
        "devonian/quads_translucent_esp",
        1536,
        false,
        true,
        Render3DPipelines.QUADS_TRANSLUCENT_ESP,
        RenderType.CompositeState
            .builder()
            .createCompositeState(false)
    )

    val BEACON_BEAM_OPAQUE = RenderType.create(
        "devonian/beacon_beam_opaque",
        1536,
        false,
        false,
        Render3DPipelines.BEACON_BEAM_OPAQUE,
        RenderType.CompositeState
            .builder()
            .setTextureState(RenderStateShard.TextureStateShard(BeaconRenderer.BEAM_LOCATION, false))
            .createCompositeState(false)
    )

    val BEACON_BEAM_OPAQUE_ESP = RenderType.create(
        "devonian/beacon_beam_opaque_esp",
        1536,
        false,
        false,
        Render3DPipelines.BEACON_BEAM_OPAQUE_ESP,
        RenderType.CompositeState
            .builder()
            .setTextureState(RenderStateShard.TextureStateShard(BeaconRenderer.BEAM_LOCATION, false))
            .createCompositeState(false)
    )

    val BEACON_BEAM_TRANSLUCENT = RenderType.create(
        "devonian/beacon_beam_translucent",
        1536,
        false,
        true,
        Render3DPipelines.BEACON_BEAM_TRANSLUCENT,
        RenderType.CompositeState
            .builder()
            .setTextureState(RenderStateShard.TextureStateShard(BeaconRenderer.BEAM_LOCATION, false))
            .createCompositeState(false)
    )

    val BEACON_BEAM_TRANSLUCENT_ESP = RenderType.create(
        "devonian/beacon_beam_translucent_esp",
        1536,
        false,
        true,
        Render3DPipelines.BEACON_BEAM_TRANSLUCENT_ESP,
        RenderType.CompositeState
            .builder()
            .setTextureState(RenderStateShard.TextureStateShard(BeaconRenderer.BEAM_LOCATION, false))
            .createCompositeState(false)
    )
}