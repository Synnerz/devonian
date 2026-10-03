package com.github.synnerz.devonian.features.dungeons.f7

import com.github.synnerz.devonian.api.dungeon.Stages
import com.github.synnerz.devonian.api.events.RenderOverlayEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.dungeons.solvers.SimonSaysSolver
import com.github.synnerz.devonian.hud.texthud.TextHudFeature
import com.github.synnerz.devonian.utils.BasicState

object SimonSaysProgressDisplay : TextHudFeature(
    "simonSaysProgressDisplay",
    "display stage of ss, must have solver on",
    Categories.F7,
    "catacombs",
    searchTags = setOf("ee2", "ss"),
    subcategory = "HUD",
) {
    override fun createRequirements(): List<BasicState<Boolean>?> {
        return super.createRequirements() + listOf(Stages.S1.isActiveState)
    }

    override fun getEditText(): List<String> = listOf("SS at 3/4")

    override fun initialize() {
        on<RenderOverlayEvent> { event ->
            setLine("SS at ${SimonSaysSolver.solutionTotal}/4")
            draw(event.ctx)
        }
    }
}