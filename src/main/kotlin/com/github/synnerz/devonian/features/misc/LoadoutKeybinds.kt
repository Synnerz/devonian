package com.github.synnerz.devonian.features.misc

import com.github.synnerz.devonian.api.ScreenUtils
import com.github.synnerz.devonian.api.events.GuiClickEvent
import com.github.synnerz.devonian.api.events.GuiKeyDownEvent
import com.github.synnerz.devonian.features.Feature

object LoadoutKeybinds : Feature(
    "loadoutKeybinds",
    "Whenever inside of loadout gui it will allow you to press the hotbar 1-9 (a for previous page, d for next page) keys to switch through your loadouts",
    subcategory = "General",
) {
    private val validSlots = listOf(
        14, 15, 16,
        23, 24, 25,
        32, 33, 34,
        41, 42, 43, // currently ignored
    )
    private val previousPage = 17
    private val nextPage = 44

    override fun initialize() {
        on<GuiKeyDownEvent> { event ->
            val screen = event.screen
            if (!screen.title.string.endsWith(") Loadouts")) return@on

            minecraft.options.keyHotbarSlots.forEachIndexed { idx, v ->
                if (!v.matches(event.event)) return@forEachIndexed

                event.cancel()
                ScreenUtils.click(validSlots[idx.coerceIn(0, 8)])

                return@on
            }
            if (minecraft.options.keyRight.matches(event.event)) {
                ScreenUtils.click(nextPage)
                return@on
            }
            if (!minecraft.options.keyLeft.matches(event.event)) return@on

            ScreenUtils.click(previousPage)
        }

        on<GuiClickEvent> { event ->
            if (!event.state) return@on
            val screen = event.screen
            if (!screen.title.string.endsWith(") Loadouts")) return@on

            minecraft.options.keyHotbarSlots.forEachIndexed { idx, v ->
                if (!v.matchesMouse(event.event)) return@forEachIndexed

                event.cancel()
                ScreenUtils.click(validSlots[idx.coerceIn(0, 8)])

                return@on
            }
        }
    }
}