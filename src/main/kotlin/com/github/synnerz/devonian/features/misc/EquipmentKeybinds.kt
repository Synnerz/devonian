package com.github.synnerz.devonian.features.misc

import com.github.synnerz.devonian.api.ScreenUtils
import com.github.synnerz.devonian.api.events.GuiClickEvent
import com.github.synnerz.devonian.api.events.GuiKeyDownEvent
import com.github.synnerz.devonian.features.Feature

object EquipmentKeybinds : Feature(
    "equipmentKeybinds",
    "Whenever inside of equipment gui it will allow you to press the hotbar 1-9 keys to switch through your equipments.",
    subcategory = "General",
) {
    private val previousPage = 45
    private val nextPage = 53

    override fun initialize() {
        on<GuiKeyDownEvent> { event ->
            val screen = event.screen
            if (!screen.title.string.endsWith(") Equipment Sets")) return@on

            minecraft.options.keyHotbarSlots.forEachIndexed { idx, v ->
                if (!v.matches(event.event)) return@forEachIndexed

                event.cancel()
                ScreenUtils.click((36 + idx).coerceIn(36, 44))

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
            if (!screen.title.string.endsWith(") Equipment Sets")) return@on

            minecraft.options.keyHotbarSlots.forEachIndexed { idx, v ->
                if (!v.matchesMouse(event.event)) return@forEachIndexed
                event.cancel()
                ScreenUtils.click((36 + idx).coerceIn(36, 44))

                return@on
            }
        }
    }
}