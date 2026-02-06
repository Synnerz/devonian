package com.github.synnerz.devonian.features.dungeons.clear

import com.github.synnerz.devonian.api.dungeon.Stages
import com.github.synnerz.devonian.api.events.RenderOverlayEvent
import com.github.synnerz.devonian.api.events.TabUpdateEvent
import com.github.synnerz.devonian.api.events.TickEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.hud.texthud.TextHudFeature
import com.github.synnerz.devonian.utils.BasicState

object CryptsDisplay : TextHudFeature(
    "cryptsDisplay",
    "Displays the current number of Crypts killed.",
    Categories.DUNGEONS,
    "catacombs",
    subcategory = "HUD",
) {
    override fun createRequirements(): List<BasicState<Boolean>?> {
        return super.createRequirements() + listOf(Stages.Clear.isActiveState)
    }

    private val cryptsRegex = "^ Crypts: (\\d+)$".toRegex()
    private var cryptsCount = 0

    override fun initialize() {
        on<TabUpdateEvent> { event ->
            val matches = event.matches(cryptsRegex) ?: return@on
            val ( amount ) = matches

            cryptsCount = amount.toInt()
        }

        on<TickEvent> {
            val format = if (cryptsCount > 4) "&6" else "&c"
            setLine("&aCrypts&f: ${format}$cryptsCount")
        }

        on<RenderOverlayEvent> { event ->
            draw(event.ctx)
        }
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        cryptsCount = 0
    }

    override fun getEditText(): List<String> = listOf("&aCrypts&f: &65")
}