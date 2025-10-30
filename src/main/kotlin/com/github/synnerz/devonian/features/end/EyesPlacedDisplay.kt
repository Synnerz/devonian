package com.github.synnerz.devonian.features.end

import com.github.synnerz.devonian.api.events.RenderOverlayEvent
import com.github.synnerz.devonian.api.events.TabUpdateEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.hud.HudManager
import com.github.synnerz.devonian.utils.Render2D

object EyesPlacedDisplay : Feature("eyesPlaced", "the end") {
    private val hud = HudManager.createHud("eyesPlaced", "&dEyes Placed&f: &d8&f/&d8")
    private val eyesPlacedRegex = "^ Eyes placed: (\\d+)/(\\d+)".toRegex()
    var currentEyes = 0
    var colorFormat = "&e"

    override fun initialize() {
        on<TabUpdateEvent> { event ->
            val match = event.matches(eyesPlacedRegex) ?: return@on

            currentEyes = match[0].toInt()
            if (currentEyes <= 4) colorFormat = "&e"
            else if (currentEyes <= 6) colorFormat = "&a"
            else colorFormat = "&d"
        }

        on<RenderOverlayEvent> {
            Render2D.drawString(
                it.ctx,
                "&dEyes Placed&f: ${colorFormat}${currentEyes}&f/&d8",
                hud.x, hud.y, hud.scale
            )
        }

        on<WorldChangeEvent> {
            currentEyes = 0
            colorFormat = "&6"
        }
    }
}