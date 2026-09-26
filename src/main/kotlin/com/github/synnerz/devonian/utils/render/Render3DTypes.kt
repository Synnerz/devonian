package com.github.synnerz.devonian.utils.render

import net.minecraft.client.renderer.blockentity.BeaconRenderer
import net.minecraft.client.renderer.rendertype.RenderSetup
import net.minecraft.client.renderer.rendertype.RenderType
import net.minecraft.resources.Identifier
import net.minecraft.util.Util

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
            .createRenderSetup(),
    )

    val LINES_TRANSLUCENT_ESP = RenderType.create(
        "devonian/lines_translucent_esp",
        RenderSetup.builder(Render3DPipelines.LINES_TRANSLUCENT_ESP)
            .createRenderSetup(),
    )

    val TRIANGLES_OPAQUE = RenderType.create(
        "devonian/triangles_opaque",
        RenderSetup.builder(Render3DPipelines.TRIANGLES_OPAQUE)
            .createRenderSetup(),
    )

    val TRIANGLES_OPAQUE_ESP = RenderType.create(
        "devonian/triangles_opaque_esp",
        RenderSetup.builder(Render3DPipelines.TRIANGLES_OPAQUE_ESP)
            .createRenderSetup(),
    )

    val TRIANGLES_TRANSLUCENT = RenderType.create(
        "devonian/triangles_translucent",
        RenderSetup.builder(Render3DPipelines.TRIANGLES_TRANSLUCENT)
            .createRenderSetup(),
    )

    val TRIANGLES_TRANSLUCENT_ESP = RenderType.create(
        "devonian/triangles_translucent_esp",
        RenderSetup.builder(Render3DPipelines.TRIANGLES_TRANSLUCENT_ESP)
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

    val TEXT = Util.memoize<Identifier, RenderType> { texture ->
        RenderType.create(
            "devonian/text",
            RenderSetup.builder(Render3DPipelines.TEXT)
                .withTexture("Sampler0", texture)
                .useLightmap()
                .createRenderSetup(),
        )
    }

    val TEXT_ESP = Util.memoize<Identifier, RenderType> { texture ->
        RenderType.create(
            "devonian/text_esp",
            RenderSetup.builder(Render3DPipelines.TEXT_ESP)
                .withTexture("Sampler0", texture)
                .useLightmap()
                .createRenderSetup(),
        )
    }
}