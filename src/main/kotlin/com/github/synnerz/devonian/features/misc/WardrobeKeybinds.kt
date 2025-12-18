package com.github.synnerz.devonian.features.misc

import com.github.synnerz.devonian.api.ScreenUtils
import com.github.synnerz.devonian.api.events.GuiKeyDownEvent
import com.github.synnerz.devonian.features.Feature
import org.lwjgl.glfw.GLFW

object WardrobeKeybinds : Feature(
    "wardrobeKeybinds",
    "Whenever inside of wardrobe gui it will allow you to press 1-9 keys to switch through your wardrobes",
    subcategory = "General"
) {
    // TODO: make these customizable
    private val keybindList = listOf(
        GLFW.GLFW_KEY_1,
        GLFW.GLFW_KEY_2,
        GLFW.GLFW_KEY_3,
        GLFW.GLFW_KEY_4,
        GLFW.GLFW_KEY_5,
        GLFW.GLFW_KEY_6,
        GLFW.GLFW_KEY_7,
        GLFW.GLFW_KEY_8,
        GLFW.GLFW_KEY_9,
    )

    override fun initialize() {
        on<GuiKeyDownEvent> { event ->
            val screen = event.screen
            if (!screen.title.string.startsWith("Wardrobe (")) return@on

            keybindList.forEach {
                if (event.key != it) return@forEach
                val slotIdx = it - 48
                val wardrobeSlot = 35 + slotIdx

                event.cancel()
                ScreenUtils.click(wardrobeSlot.coerceIn(36, 44))

                return@on
            }
        }
    }
}