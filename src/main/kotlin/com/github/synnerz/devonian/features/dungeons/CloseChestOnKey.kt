package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.devonian.api.dungeon.Dungeons
import com.github.synnerz.devonian.api.dungeon.Stages
import com.github.synnerz.devonian.api.events.GuiKeyDownEvent
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

    private val keybinds get() = listOf(
        minecraft.options.keyUp, // w
        minecraft.options.keyLeft, // a
        minecraft.options.keyRight, // d
        minecraft.options.keyDown, // s
        minecraft.options.keyShift, // shift
    )

    override fun initialize() {
        on<GuiKeyDownEvent> { event ->
            if (event.screen !is AbstractContainerScreen<*>) return@on

            val container = event.screen
            if (container.title.string != "Chest") return@on
            val containerId = container.menu.containerId

            keybinds.forEach {
                if (containerId == -1) return@on
                if (!it.matches(event.event)) return@forEach

                event.cancel()
                minecraft.connection?.send(ServerboundContainerClosePacket(containerId))
                minecraft.setScreen(null)
                return@on
            }
        }
    }
}