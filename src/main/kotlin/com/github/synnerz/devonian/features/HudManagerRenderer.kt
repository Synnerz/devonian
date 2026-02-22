package com.github.synnerz.devonian.features

import com.github.synnerz.devonian.api.Scheduler
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.hud.texthud.StylizedTextHud
import com.github.synnerz.devonian.hud.texthud.TextHudFeature
import com.github.synnerz.devonian.utils.Toggleable

object HudManagerRenderer : TextHudFeature(
    "hudManagerRenderer",
    "Whether to use custom fonts for all text hud elements.",
    Categories.GLOBAL,
    displayName = "HUD Use Fonts",
    subcategory = "Mod",
    isInternal = true,
) {
    override fun getEditText(): List<String> = listOf("Change HUD Style").let {
        if (isEnabled()) listOf(it[0], "Hint: /dv font <name>")
        else it
    }

    override fun initialize() {
        children.add(
            object : Toggleable() {
                override fun add() {
                    Scheduler.scheduleTask {
                        StylizedTextHud.recreateRenderers(true)
                    }
                }
                override fun remove() {
                    Scheduler.scheduleTask {
                        StylizedTextHud.recreateRenderers(false)
                    }
                }
            }
        )
    }

    override fun setDefaultValues() {
        super.setDefaultValues()

        x = window.guiScaledWidth / 2.0
        y = window.guiScaledHeight * 3.0 / 8.0
    }
}