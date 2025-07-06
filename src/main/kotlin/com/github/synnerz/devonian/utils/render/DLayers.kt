package com.github.synnerz.devonian.utils.render

import net.minecraft.client.render.RenderLayer
import net.minecraft.client.render.RenderPhase
import net.minecraft.client.render.block.entity.BeaconBlockEntityRenderer
import net.minecraft.util.TriState

object DLayers {
    val LINES = RenderLayer.of(
        "devonian/lines",
        1536,
        false,
        true,
        DPipelines.LINES,
        RenderLayer.MultiPhaseParameters
            .builder()
            .build(false)
    )

    val LINES_ESP = RenderLayer.of(
        "devonian/lines_esp",
        1536,
        false,
        true,
        DPipelines.LINES_ESP,
        RenderLayer.MultiPhaseParameters
            .builder()
            .build(false)
    )

    val TRIANGLE_STRIP = RenderLayer.of(
        "devonian_triangle_strip",
        1536,
        false,
        true,
        DPipelines.TRIANGLE_STRIP,
        RenderLayer.MultiPhaseParameters
            .builder()
            .build(false)
    )

    val TRIANGLE_STRIP_ESP = RenderLayer.of(
        "devonian_triangle_strip_esp",
        1536,
        false,
        true,
        DPipelines.TRIANGLE_STRIP_ESP,
        RenderLayer.MultiPhaseParameters
            .builder()
            .build(false)
    )

    val BEACON_BEAM_OPAQUE = RenderLayer.of(
        "devonian_beacon_beam_opaque",
        1536,
        false,
        true,
        DPipelines.BEACON_BEAM_OPAQUE,
        RenderLayer.MultiPhaseParameters
            .builder()
            .texture(RenderPhase.Texture(BeaconBlockEntityRenderer.BEAM_TEXTURE, TriState.FALSE, false))
            .build(false)
    )

    val BEACON_BEAM_OPAQUE_ESP = RenderLayer.of(
        "devonian_beacon_beam_opaque_esp",
        1536,
        false,
        true,
        DPipelines.BEACON_BEAM_OPAQUE_ESP,
        RenderLayer.MultiPhaseParameters
            .builder()
            .texture(RenderPhase.Texture(BeaconBlockEntityRenderer.BEAM_TEXTURE, TriState.FALSE, false))
            .build(false)
    )

    val BEACON_BEAM_TRANSLUCENT = RenderLayer.of(
        "devonian_beacon_beam_translucent",
        1536,
        false,
        true,
        DPipelines.BEACON_BEAM_TRANSLUCENT,
        RenderLayer.MultiPhaseParameters
            .builder()
            .texture(RenderPhase.Texture(BeaconBlockEntityRenderer.BEAM_TEXTURE, TriState.FALSE, false))
            .build(false)
    )

    val BEACON_BEAM_TRANSLUCENT_ESP = RenderLayer.of(
        "devonian_beacon_beam_translucent_esp",
        1536,
        false,
        true,
        DPipelines.BEACON_BEAM_TRANSLUCENT_ESP,
        RenderLayer.MultiPhaseParameters
            .builder()
            .texture(RenderPhase.Texture(BeaconBlockEntityRenderer.BEAM_TEXTURE, TriState.FALSE, false))
            .build(false)
    )
}