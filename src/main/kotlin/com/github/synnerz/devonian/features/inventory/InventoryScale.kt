package com.github.synnerz.devonian.features.inventory

import com.github.synnerz.devonian.api.events.GuiCloseEvent
import com.github.synnerz.devonian.api.events.GuiOpenEvent
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.features.dungeons.CustomTerminalScale
import net.minecraft.client.gui.screens.inventory.ContainerScreen
import net.minecraft.client.gui.screens.inventory.InventoryScreen
import kotlin.math.roundToInt

object InventoryScale : Feature(
    "inventoryScale",
    "Changes the scale of inventory related guis whenever opening them",
    subcategory = "Inventory"
) {
    private val SETTING_SCALE = addSlider(
        "scale",
        1.0,
        0.0, 5.0,
        "0 = auto",
        "Inventory Scale Amount"
    )
    private var oldScale = -1

    override fun initialize() {
        on<GuiOpenEvent> { event ->
            val screen = event.screen
            if (screen is ContainerScreen || screen is InventoryScreen) {
                val title = screen.title.string
                if (CustomTerminalScale.shouldScale(title)) return@on
                val guiScale = minecraft.options.guiScale()

                oldScale = guiScale.get()
                guiScale.set(SETTING_SCALE.get().roundToInt())
            }
        }

        on<GuiCloseEvent> { event ->
            val screen = event.screen
            if (screen is ContainerScreen || screen is InventoryScreen) {
                if (oldScale == -1) return@on

                minecraft.options.guiScale().set(oldScale)
                oldScale = -1
            }
        }
    }
}