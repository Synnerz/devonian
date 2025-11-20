package com.github.synnerz.devonian.features.misc

import com.github.synnerz.devonian.api.events.RenderOverlayEvent
import com.github.synnerz.devonian.hud.texthud.TextHudFeature

object EtherwarpOverlayFailReason : TextHudFeature(
    "etherwarpFailReasonDisplay",
    "Shows reason why etherwarp fails."
) {
    override fun initialize() {
        on<RenderOverlayEvent> { event ->
            val msg = EtherwarpOverlay.failReason
            if (msg.isEmpty()) return@on

            setLine(msg)
            draw(event.ctx)
        }
    }

    override fun getEditText(): List<String> = listOf("&4Can't TP: No air above!")
}