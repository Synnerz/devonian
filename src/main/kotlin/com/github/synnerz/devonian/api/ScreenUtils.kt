package com.github.synnerz.devonian.api

import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.screen.slot.SlotActionType

object ScreenUtils {
    @JvmOverloads
    fun click(slot: Int, shift: Boolean = false, button: String = "LEFT") {
        val minecraft = MinecraftClient.getInstance()
        val screen = minecraft.currentScreen ?: return
        val windowId = (screen as HandledScreen<*>).screenHandler?.syncId ?: return
        val clickMode = when {
            button == "MIDDLE" -> SlotActionType.CLONE
            shift -> SlotActionType.QUICK_MOVE
            else -> SlotActionType.PICKUP
        }
        val clickButton = when (button) {
            "LEFT" -> 0
            "RIGHT" -> 1
            "MIDDLE" -> 2
            else -> 0
        }

        minecraft.interactionManager?.clickSlot(
            windowId,
            slot, clickButton, clickMode, minecraft.player
        )
    }
}