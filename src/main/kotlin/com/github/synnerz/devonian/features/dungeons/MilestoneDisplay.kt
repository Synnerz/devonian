package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.devonian.events.RenderOverlayEvent
import com.github.synnerz.devonian.events.TabUpdateEvent
import com.github.synnerz.devonian.events.WorldChangeEvent
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.hud.HudManager
import com.github.synnerz.devonian.utils.render.Render2D

object MilestoneDisplay : Feature("milestoneDisplay", "catacombs") {
    private val milestoneRegex = "^ Your Milestone: .(.)\$".toRegex()
    private val hud = HudManager.createHud("MilestoneDisplay", "&bMilestone&f: &69")
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
            Render2D.drawString(
                event.ctx,
                "&bMilestone&f: ${format}$milestoneCount",
                hud.x, hud.y, hud.scale
            )
        }
    }
}