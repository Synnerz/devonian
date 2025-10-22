package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.devonian.api.events.RenderOverlayEvent
import com.github.synnerz.devonian.api.events.TabUpdateEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.hud.HudManager
import com.github.synnerz.devonian.utils.render.Render2D

object CryptsDisplay : Feature("cryptsDisplay", "catacombs") {
    private val hud = HudManager.createHud("CryptsDisplay", "&aCrypts&f: &65")
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
            Render2D.drawString(
                event.ctx,
                "&aCrypts&f: ${format}$cryptsCount",
                hud.x, hud.y, hud.scale
            )
        }
    }
}