package com.github.synnerz.devonian.features.misc

import com.github.synnerz.devonian.api.ScreenUtils
import com.github.synnerz.devonian.api.events.GuiClickEvent
import com.github.synnerz.devonian.api.events.GuiKeyDownEvent
import com.github.synnerz.devonian.features.Feature

object WardrobeKeybinds : Feature(
    "wardrobeKeybinds",
    "Whenever inside of wardrobe gui it will allow you to press the hotbar 1-9 keys to switch through your wardrobes.",
    subcategory = "General",
) {
    private val previousPage = 45
    private val nextPage = 53

    override fun initialize() {
        on<GuiKeyDownEvent> { event ->
            val screen = event.screen
            if (!screen.title.string.endsWith(") Armor Sets")) return@on

            minecraft.options.keyHotbarSlots.forEachIndexed { i, v ->
                if (!v.matches(event.event)) return@forEachIndexed
                val wardrobeSlot = 36 + i

                event.cancel()
                ScreenUtils.click(wardrobeSlot.coerceIn(36, 44))

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
            if (!screen.title.string.endsWith(") Armor Sets")) return@on

            minecraft.options.keyHotbarSlots.forEachIndexed { i, v ->
                if (!v.matchesMouse(event.event)) return@forEachIndexed
                val wardrobeSlot = 36 + i

                event.cancel()
                ScreenUtils.click(wardrobeSlot.coerceIn(36, 44))

                return@on
            }
        }
    }
}