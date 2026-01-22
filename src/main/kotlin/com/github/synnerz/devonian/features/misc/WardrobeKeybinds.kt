package com.github.synnerz.devonian.features.misc

import com.github.synnerz.devonian.api.ScreenUtils
import com.github.synnerz.devonian.api.events.GuiKeyDownEvent
import com.github.synnerz.devonian.features.Feature

object WardrobeKeybinds : Feature(
    "wardrobeKeybinds",
    "Whenever inside of wardrobe gui it will allow you to press the hotbar 1-9 keys to switch through your wardrobes.",
    subcategory = "General",
) {
    override fun initialize() {
        on<GuiKeyDownEvent> { event ->
            val screen = event.screen
            if (!screen.title.string.startsWith("Wardrobe (")) return@on

            minecraft.options.keyHotbarSlots.forEachIndexed { i, v ->
                if (!v.matches(event.event)) return@forEachIndexed
                val wardrobeSlot = 36 + i

                event.cancel()
                ScreenUtils.click(wardrobeSlot.coerceIn(36, 44))

                return@on
            }
        }
    }
}