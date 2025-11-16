package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.devonian.api.events.RenderOverlayEvent
import com.github.synnerz.devonian.api.events.TabUpdateEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.hud.texthud.TextHudFeature

object DeathsDisplay : TextHudFeature(
    "deathsDisplay",
    "Displays the current amount of Team Deaths.",
    "Dungeons",
    "catacombs"
) {
    private val deathsRegex = "^Team Deaths: (\\d+)$".toRegex()
    private var deathCount = 0

    override fun initialize() {
        on<TabUpdateEvent> { event ->
            val matches = event.matches(deathsRegex) ?: return@on
            val ( amount ) = matches

            deathCount = amount.toInt()
        }

        on<WorldChangeEvent> {
            deathCount = 0
        }

        on<RenderOverlayEvent> { event ->
            val count = deathCount
            val format = if (count > 2) "&4" else "&c"
            setLine("&8&lDeaths&f: ${format}$count")
            draw(event.ctx)
        }
    }

    override fun getEditText(): List<String> = listOf("&8&lDeaths&f: &43")
}