package com.github.synnerz.devonian.features.inventory

import com.github.synnerz.devonian.api.events.TickEvent
import com.github.synnerz.devonian.features.Feature
import kotlinx.atomicfu.atomic
import net.minecraft.client.gui.screens.Screen

object NoCursorReset : Feature(
    "noCursorReset",
    "Avoids resetting your cursor whenever navigating guis",
    subcategory = "Inventory",
) {
    var lastScreen: Screen? = null
    var resetCursor = atomic(true)

    override fun initialize() {
        on<TickEvent> {
            val screen = minecraft.screen
            if (lastScreen == null && screen != null) {
                resetCursor.value = true
                lastScreen = screen
                return@on
            }
            if (lastScreen != null && screen != null && lastScreen != screen) {
                resetCursor.value = false
                lastScreen = screen
                return@on
            }
            if (lastScreen != null && screen == null) {
                resetCursor.value = true
                lastScreen = null
                return@on
            }
        }
    }

    fun shouldReset(): Boolean {
        if (!isEnabled()) return true
        return resetCursor.value
    }
}