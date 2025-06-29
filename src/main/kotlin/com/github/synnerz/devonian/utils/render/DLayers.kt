package com.github.synnerz.devonian.utils.render

import net.minecraft.client.render.RenderLayer

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
}