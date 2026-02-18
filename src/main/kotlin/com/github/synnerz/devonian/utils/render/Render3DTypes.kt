package com.github.synnerz.devonian.utils.render

import net.minecraft.client.renderer.blockentity.BeaconRenderer
import net.minecraft.client.renderer.rendertype.RenderSetup
import net.minecraft.client.renderer.rendertype.RenderType
import java.util.*
import kotlin.math.ceil

object Render3DTypes {
    private data class LineLayerKey(val lineWidth: Double, val phase: Boolean, val opaque: Boolean)
    private val cachedLineLayers = mutableMapOf<LineLayerKey, RenderType>()

    fun lines(lineWidth: Double = 1.0, phase: Boolean = false, opaque: Boolean): RenderType {
        // TODO: as far as i can tell, mojang removed the line width from the pipelines themselves
        //  and now its supposed to be passed through to the vertices (i think) so we might need to
        //  remake this part of the api ):
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
        RenderSetup.builder(Render3DPipelines.TRIANGLE_STRIP_OPAQUE)
            .sortOnUpload()
            .createRenderSetup(),
    )

    val TRIANGLE_STRIP_OPAQUE_ESP = RenderType.create(
        "devonian/triangle_strip_opaque_esp",
        RenderSetup.builder(Render3DPipelines.TRIANGLE_STRIP_OPAQUE_ESP)
            .sortOnUpload()
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
            .sortOnUpload()
            .createRenderSetup(),
    )

    val QUADS_OPAQUE_ESP = RenderType.create(
        "devonian/quads_opaque_esp",
        RenderSetup.builder(Render3DPipelines.QUADS_OPAQUE_ESP)
            .sortOnUpload()
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
            .sortOnUpload()
            .createRenderSetup(),
    )

    val BEACON_BEAM_OPAQUE_ESP = RenderType.create(
        "devonian/beacon_beam_opaque_esp",
        RenderSetup.builder(Render3DPipelines.BEACON_BEAM_OPAQUE_ESP)
            .withTexture("Sampler0", BeaconRenderer.BEAM_LOCATION)
            .sortOnUpload()
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