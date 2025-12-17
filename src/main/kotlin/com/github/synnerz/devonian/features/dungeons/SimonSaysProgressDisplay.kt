package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.devonian.api.events.RenderOverlayEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.dungeons.solvers.SimonSaysSolver
import com.github.synnerz.devonian.hud.texthud.TextHudFeature

object SimonSaysProgressDisplay : TextHudFeature(
    "simonSaysProgressDisplay",
    "display stage of ss, must have solver on",
    Categories.DUNGEONS,
    "catacombs",
    subcategory = "HUD",
) {
    override fun getEditText(): List<String> = listOf("SS at 4/5")

    override fun initialize() {
        on<RenderOverlayEvent> { event ->
            setLine("SS at ${SimonSaysSolver.solutionTotal}/5")
            draw(event.ctx)
        }
    }
}