package com.github.synnerz.devonian.features.end

import com.github.synnerz.devonian.api.events.RenderOverlayEvent
import com.github.synnerz.devonian.api.events.TabUpdateEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.hud.HudManager
import com.github.synnerz.devonian.utils.Location
import com.github.synnerz.devonian.utils.Render2D

object EyesPlacedDisplay : Feature("eyesPlaced", "the end") {
    private val hud = HudManager.createHud("EyesPlacedDisplay", "&dEyes Placed&f: &d8&f/&d8")
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

        on<RenderOverlayEvent> {
            if (Location.subarea == null) return@on
            if (!Location.subarea!!.contains("dragon's nest")) return@on

            Render2D.drawString(
                it.ctx,
                displayStr ?: "&dEyes Placed&f: &e0&f/&d8",
                hud.x, hud.y, hud.scale
            )
        }

        on<WorldChangeEvent> {
            displayStr = null
        }
    }
}