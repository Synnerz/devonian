package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.devonian.api.events.RenderOverlayEvent
import com.github.synnerz.devonian.api.events.TabUpdateEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.hud.texthud.TextHudFeature

object CryptsDisplay : TextHudFeature(
    "cryptsDisplay",
    "Displays the current amount of Crypts killed.",
    "Dungeons",
    "catacombs"
) {
    private val cryptsRegex = "^ Crypts: (\\d+)$".toRegex()
    private var cryptsCount = 0

    override fun initialize() {
        on<TabUpdateEvent> { event ->
            val matches = event.matches(cryptsRegex) ?: return@on
            val ( amount ) = matches

            cryptsCount = amount.toInt()
        }

        on<WorldChangeEvent> {
            cryptsCount = 0
        }

        on<RenderOverlayEvent> { event ->
            val format = if (cryptsCount > 4) "&6" else "&c"
            setLine("&aCrypts&f: ${format}$cryptsCount")
            draw(event.ctx)
        }
    }

    override fun getEditText(): List<String> = listOf("&aCrypts&f: &65")
}