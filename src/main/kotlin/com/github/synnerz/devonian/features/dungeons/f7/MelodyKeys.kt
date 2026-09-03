package com.github.synnerz.devonian.features.dungeons.f7

import com.github.synnerz.devonian.api.ScreenUtils
import com.github.synnerz.devonian.api.dungeon.Stages
import com.github.synnerz.devonian.api.events.GuiKeyDownEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.utils.BasicState
import org.lwjgl.sdl.SDLKeycode

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
        SDLKeycode.SDLK_1,
        SDLKeycode.SDLK_2,
        SDLKeycode.SDLK_3,
        SDLKeycode.SDLK_4,
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