package com.github.synnerz.devonian.features.misc

import com.github.synnerz.barrl.Context
import com.github.synnerz.devonian.api.events.PreRenderEntityEvent
import com.github.synnerz.devonian.features.Feature
import net.minecraft.client.renderer.entity.state.ArrowRenderState
import java.awt.Color

object ArrowHitboxes : Feature(
    "arrowHitboxes",
    "draws boxes around arrows",
    subcategory = "General",
) {
    private val SETTING_WIRE_COLOR = addColorPicker(
        "wire",
        Color(255, 255, 255).rgb,
        "",
        "Arrow Wire Color",
    )
    private val SETTING_FILL_COLOR = addColorPicker(
        "fill",
        0,
        "",
        "Arrow Fill Color",
    )
    private val SETTING_LINE_WIDTH = addSlider(
        "lineWidth",
        1.0,
        0.0, 10.0,
        "",
        "Arrow Line Width",
    )

    override fun initialize() {
        on<PreRenderEntityEvent> { event ->
            val state = event.entityState as? ArrowRenderState ?: return@on
            if (state.hitboxesRenderState != null) return@on

            val w = state.boundingBoxWidth
            val h = state.boundingBoxHeight
            Context.Immediate?.renderBox(
                state.x - w * 0.5,
                state.y,
                state.z - w * 0.5,
                w.toDouble(), h.toDouble(),
                SETTING_WIRE_COLOR.getColor(),
                lineWidth = SETTING_LINE_WIDTH.get(),
            )
            Context.Immediate?.renderFilledBox(
                state.x - w * 0.5,
                state.y,
                state.z - w * 0.5,
                w.toDouble(), h.toDouble(),
                SETTING_FILL_COLOR.getColor(),
            )
        }
    }
}