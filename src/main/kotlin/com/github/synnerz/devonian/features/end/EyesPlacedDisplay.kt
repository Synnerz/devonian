package com.github.synnerz.devonian.features.end

import com.github.synnerz.devonian.api.events.RenderOverlayEvent
import com.github.synnerz.devonian.api.events.TabUpdateEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.hud.texthud.TextHudFeature
import com.github.synnerz.devonian.utils.Location

object EyesPlacedDisplay : TextHudFeature(
    "eyesPlaced",
    "Displays the amount of eyes placed whenever in the Dragon's Nest",
    Categories.END,
    "the end"
) {
    private val eyesPlacedRegex = "^ Eyes placed: (\\d+)/(\\d+)".toRegex()
    private val dragonSpawnedRegex = "^ Dragon spawned!$".toRegex()
    private val eggRespawningRegex = "^ Egg respawning!$".toRegex()
    var displayStr: String? = null

    override fun initialize() {
        on<TabUpdateEvent> { event ->
            if (event.matches(dragonSpawnedRegex) != null) {
                displayStr = "&aDragon Spawned!"
                return@on
            }
            if (event.matches(eggRespawningRegex) != null) {
                displayStr = "&dEgg Respawning!"
                return@on
            }

            val match = event.matches(eyesPlacedRegex) ?: return@on

            val currentEyes = match[0].toInt()
            val colorFormat = if (currentEyes > 6) "&d"
                else if (currentEyes >= 4) "&a"
                else "&e"

            displayStr = "&dEyes Placed&f: ${colorFormat}${currentEyes}&f/&d8"
        }

        on<RenderOverlayEvent> { event ->
            if (Location.subarea == null) return@on
            if (!Location.subarea!!.contains("dragon's nest")) return@on

            setLine(displayStr ?: "&dEyes Placed&f: &e0&f/&d8")
            draw(event.ctx)
        }

        on<WorldChangeEvent> {
            displayStr = null
        }
    }

    override fun getEditText(): List<String> = listOf("&dEyes Placed&f: &d8&f/&d8")
}