package com.github.synnerz.devonian.api

import com.github.synnerz.devonian.Devonian
import com.github.synnerz.devonian.mixin.accessor.AbstractContainerScreenAccessor
import com.github.synnerz.devonian.utils.render.Render2D
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.client.gui.screens.inventory.ContainerScreen
import net.minecraft.client.gui.screens.inventory.InventoryScreen
import net.minecraft.world.inventory.ContainerInput
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack

object ScreenUtils {
    @JvmOverloads
    fun click(slot: Int, shift: Boolean = false, button: String = "LEFT") {
        val minecraft = Devonian.minecraft
        val screen = minecraft.screen ?: return
        val windowId = (screen as AbstractContainerScreen<*>).menu.containerId
        val clickMode = when {
            button == "MIDDLE" -> ContainerInput.CLONE
            shift -> ContainerInput.QUICK_MOVE
            else -> ContainerInput.PICKUP
        }
        val clickButton = when (button) {
            "LEFT" -> 0
            "RIGHT" -> 1
            "MIDDLE" -> 2
            else -> 0
        }

        minecraft.player?.let {
            minecraft.gameMode?.handleContainerInput(
                windowId,
                slot, clickButton, clickMode, it
            )
        }
    }

    fun cursorStack(screen: Screen): ItemStack? = cursorSlot(screen)?.item

    fun cursorSlot(screen: Screen): Slot? {
        if (!(screen is InventoryScreen || screen is ContainerScreen)) return null
        val accessor = screen as AbstractContainerScreenAccessor

        return accessor.getSlotAtPos(
            Render2D.Mouse.x,
            Render2D.Mouse.y,
        )
    }
}