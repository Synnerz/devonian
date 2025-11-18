package com.github.synnerz.devonian.api

import com.github.synnerz.devonian.Devonian
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.world.inventory.ClickType

object ScreenUtils {
    @JvmOverloads
    fun click(slot: Int, shift: Boolean = false, button: String = "LEFT") {
        val minecraft = Devonian.minecraft
        val screen = minecraft.screen ?: return
        val windowId = (screen as AbstractContainerScreen<*>).menu?.containerId ?: return
        val clickMode = when {
            button == "MIDDLE" -> ClickType.CLONE
            shift -> ClickType.QUICK_MOVE
            else -> ClickType.PICKUP
        }
        val clickButton = when (button) {
            "LEFT" -> 0
            "RIGHT" -> 1
            "MIDDLE" -> 2
            else -> 0
        }

        minecraft.player?.let {
            minecraft.gameMode?.handleInventoryMouseClick(
                windowId,
                slot, clickButton, clickMode, it
            )
        }
    }
}