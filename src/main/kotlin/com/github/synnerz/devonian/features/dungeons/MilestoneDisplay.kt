package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.devonian.api.events.RenderOverlayEvent
import com.github.synnerz.devonian.api.events.TabUpdateEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.hud.texthud.TextHudFeature

object MilestoneDisplay : TextHudFeature(
    "milestoneDisplay",
    "Displays your current Milestone.",
    Categories.DUNGEONS,
    "catacombs",
    subcategory = "HUD",
) {
    private val milestoneRegex = "^ Your Milestone: .(.)\$".toRegex()
    private val milestonSymbols = mutableListOf("⓿", "❶", "❷", "❸", "❹", "❺", "❻", "❼", "❽", "❾")
    private var milestoneCount = 0

    override fun initialize() {
        on<TabUpdateEvent> { event ->
            val matches = event.matches(milestoneRegex) ?: return@on
            val ( symb ) = matches

            milestoneCount = milestonSymbols.indexOf(symb)
        }

        on<WorldChangeEvent> {
            milestoneCount = 0
        }

        on<RenderOverlayEvent> { event ->
            val format = if (milestoneCount > 2) "&6" else "&c"
            setLine("&bMilestone&f: ${format}$milestoneCount")
            draw(event.ctx)
        }
    }

    override fun getEditText(): List<String> = listOf("&bMilestone&f: &69")
}