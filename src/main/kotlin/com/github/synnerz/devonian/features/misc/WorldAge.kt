package com.github.synnerz.devonian.features.misc

import com.github.synnerz.devonian.api.events.RenderOverlayEvent
import com.github.synnerz.devonian.api.events.TickEvent
import com.github.synnerz.devonian.hud.texthud.TextHudFeature

object WorldAge : TextHudFeature(
    "worldAge",
    "Displays the current World's age."
) {
    override fun initialize() {
        on<TickEvent> {
            val w = minecraft.level ?: return@on
            setLine("&bDay&f: &6${w.dayTime / 24000}")
        }

        on<RenderOverlayEvent> { event ->
            draw(event.ctx)
        }
    }

    override fun getEditText(): List<String> = listOf("&bDay&f: &610")
}