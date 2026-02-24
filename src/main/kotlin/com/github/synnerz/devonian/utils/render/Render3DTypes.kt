package com.github.synnerz.devonian.utils.render

import net.minecraft.client.renderer.blockentity.BeaconRenderer
import net.minecraft.client.renderer.rendertype.RenderSetup
import net.minecraft.client.renderer.rendertype.RenderType

object Render3DTypes {
    val LINES_OPAQUE = RenderType.create(
        "devonian/lines_opaque",
        RenderSetup.builder(Render3DPipelines.LINES_OPAQUE)
            .createRenderSetup(),
    )

    val LINES_OPAQUE_ESP = RenderType.create(
        "devonian/lines_opaque_esp",
        RenderSetup.builder(Render3DPipelines.LINES_OPAQUE_ESP)
            .createRenderSetup(),
    )

    val LINES_TRANSLUCENT = RenderType.create(
        "devonian/lines_translucent",
        RenderSetup.builder(Render3DPipelines.LINES_TRANSLUCENT)
            .sortOnUpload()
            .createRenderSetup(),
    )

    val LINES_TRANSLUCENT_ESP = RenderType.create(
        "devonian/lines_translucent_esp",
        RenderSetup.builder(Render3DPipelines.LINES_TRANSLUCENT_ESP)
            .sortOnUpload()
            .createRenderSetup(),
    )

    val TRIANGLE_STRIP_OPAQUE = RenderType.create(
        "devonian/triangle_strip_opaque",
        RenderSetup.builder(Render3DPipelines.TRIANGLE_STRIP_OPAQUE)
            .createRenderSetup(),
    )

    val TRIANGLE_STRIP_OPAQUE_ESP = RenderType.create(
        "devonian/triangle_strip_opaque_esp",
        RenderSetup.builder(Render3DPipelines.TRIANGLE_STRIP_OPAQUE_ESP)
            .createRenderSetup(),
    )

    val TRIANGLE_STRIP_TRANSLUCENT = RenderType.create(
        "devonian/triangle_strip_translucent",
        RenderSetup.builder(Render3DPipelines.TRIANGLE_STRIP_TRANSLUCENT)
            .sortOnUpload()
            .createRenderSetup(),
    )

    val TRIANGLE_STRIP_TRANSLUCENT_ESP = RenderType.create(
        "devonian/triangle_strip_translucent_esp",
        RenderSetup.builder(Render3DPipelines.TRIANGLE_STRIP_TRANSLUCENT_ESP)
            .sortOnUpload()
            .createRenderSetup(),
    )

    val QUADS_OPAQUE = RenderType.create(
        "devonian/quads_opaque",
        RenderSetup.builder(Render3DPipelines.QUADS_OPAQUE)
            .createRenderSetup(),
    )

    val QUADS_OPAQUE_ESP = RenderType.create(
        "devonian/quads_opaque_esp",
        RenderSetup.builder(Render3DPipelines.QUADS_OPAQUE_ESP)
            .createRenderSetup(),
    )

    val QUADS_TRANSLUCENT = RenderType.create(
        "devonian/quads_translucent",
        RenderSetup.builder(Render3DPipelines.QUADS_TRANSLUCENT)
            .sortOnUpload()
            .createRenderSetup(),
    )

    val QUADS_TRANSLUCENT_ESP = RenderType.create(
        "devonian/quads_translucent_esp",
        RenderSetup.builder(Render3DPipelines.QUADS_TRANSLUCENT_ESP)
            .sortOnUpload()
            .createRenderSetup(),
    )

    val BEACON_BEAM_OPAQUE = RenderType.create(
        "devonian/beacon_beam_opaque",
        RenderSetup.builder(Render3DPipelines.BEACON_BEAM_OPAQUE)
            .withTexture("Sampler0", BeaconRenderer.BEAM_LOCATION)
            .createRenderSetup(),
    )

    val BEACON_BEAM_OPAQUE_ESP = RenderType.create(
        "devonian/beacon_beam_opaque_esp",
        RenderSetup.builder(Render3DPipelines.BEACON_BEAM_OPAQUE_ESP)
            .withTexture("Sampler0", BeaconRenderer.BEAM_LOCATION)
            .createRenderSetup(),
    )

    val BEACON_BEAM_TRANSLUCENT = RenderType.create(
        "devonian/beacon_beam_translucent",
        RenderSetup.builder(Render3DPipelines.BEACON_BEAM_TRANSLUCENT)
            .withTexture("Sampler0", BeaconRenderer.BEAM_LOCATION)
            .sortOnUpload()
            .createRenderSetup(),
    )

    val BEACON_BEAM_TRANSLUCENT_ESP = RenderType.create(
        "devonian/beacon_beam_translucent_esp",
        RenderSetup.builder(Render3DPipelines.BEACON_BEAM_TRANSLUCENT_ESP)
            .withTexture("Sampler0", BeaconRenderer.BEAM_LOCATION)
            .sortOnUpload()
            .createRenderSetup(),
    )
}