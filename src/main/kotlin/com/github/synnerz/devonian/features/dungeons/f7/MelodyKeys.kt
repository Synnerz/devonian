package com.github.synnerz.devonian.features.dungeons.f7

import com.github.synnerz.devonian.api.ScreenUtils
import com.github.synnerz.devonian.api.dungeon.Stages
import com.github.synnerz.devonian.api.events.GuiKeyDownEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.utils.BasicState
import com.mojang.blaze3d.platform.InputConstants

object MelodyKeys : Feature(
    "melodyKeys",
    "use 1-4 to click melody terminal buttons",
    Categories.F7,
    "catacombs",
    searchTags = setOf("terminal"),
    subcategory = "Terminals",
) {
    override fun createRequirements(): List<BasicState<Boolean>?> {
        return super.createRequirements() + listOf(Stages.Terminals.isActiveState)
    }

    private val keybindList = listOf(
        InputConstants.KEY_1,
        InputConstants.KEY_2,
        InputConstants.KEY_3,
        InputConstants.KEY_4,
    )

    override fun initialize() {
        on<GuiKeyDownEvent> { event ->
            val screen = event.screen
            if (screen.title.string != "Click the button on time!") return@on

            keybindList.forEachIndexed { i, key ->
                if (key != event.key) return@forEachIndexed

                event.cancel()
                ScreenUtils.click(i * 9 + 16)

                return@on
            }
        }
    }
}