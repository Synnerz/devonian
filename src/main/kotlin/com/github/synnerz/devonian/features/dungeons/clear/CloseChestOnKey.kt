package com.github.synnerz.devonian.features.dungeons.clear

import com.github.synnerz.devonian.api.dungeon.Stages
import com.github.synnerz.devonian.api.events.GuiOpenEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.utils.BasicState
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket

object CloseChestOnKey : Feature(
    "closeChestOnKey",
    "Closes a secret chest whenever you hit WASD (or rather moving) and shift keys",
    Categories.DUNGEONS,
    "catacombs",
    subcategory = "QOL"
) {
    override fun createRequirements(): List<BasicState<Boolean>?> {
        return super.createRequirements() + listOf(Stages.Clear.isActiveState)
    }

    override fun initialize() {
        on<GuiOpenEvent> { event ->
            if (event.screen !is AbstractContainerScreen<*>) return@on

            val container = event.screen
            if (container.title.string != "Chest") return@on
            val containerId = container.menu.containerId

                if (containerId == -1) return@on

                event.cancel()
                minecraft.connection?.send(ServerboundContainerClosePacket(containerId))
                minecraft.setScreen(null)
                return@on
        }
    }
}