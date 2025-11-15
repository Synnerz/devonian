package com.github.synnerz.devonian.features.misc

import com.github.synnerz.devonian.api.events.RenderOverlayEvent
import com.github.synnerz.devonian.hud.texthud.TextHudFeature

object WorldAge : TextHudFeature("worldAge") {
    override fun initialize() {
        on<RenderOverlayEvent> { event ->
            if (minecraft.world == null) return@on

            setLine("&bDay&f: &6${minecraft.world!!.timeOfDay / 24000}")
            draw(event.ctx)
        }
    }

    override fun getEditText(): List<String> = listOf("&bDay&f: &610")
}