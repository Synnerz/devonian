package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.devonian.api.events.RenderOverlayEvent
import com.github.synnerz.devonian.api.events.TabUpdateEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.hud.HudManager
import com.github.synnerz.devonian.utils.render.Render2D

object DeathsDisplay : Feature("deathsDisplay", "catacombs") {
    private val deathsRegex = "^Team Deaths: (\\d+)$".toRegex()
    private val hud = HudManager.createHud("DeathsDisplay", "&8&lDeaths&f: &43")
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
            val format = if (deathCount > 2) "&4" else "&c"
            Render2D.drawString(
                event.ctx,
                "&8&lDeaths&f: ${format}$deathCount",
                hud.x, hud.y, hud.scale
            )
        }
    }
}