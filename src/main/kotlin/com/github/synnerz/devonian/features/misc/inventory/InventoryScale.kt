package com.github.synnerz.devonian.features.misc.inventory

import com.github.synnerz.devonian.api.events.GuiScaleEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import net.minecraft.client.gui.screens.inventory.ContainerScreen
import net.minecraft.client.gui.screens.inventory.InventoryScreen
import kotlin.math.roundToInt

object InventoryScale : Feature(
    "inventoryScale",
    "Changes the scale of inventory related guis whenever opening them.",
    Categories.VANILLA_TWEAKS,
    subcategory = "Container",
) {
    private val SETTING_SCALE = addSlider(
        "scale",
        1.0,
        0.0, 5.0,
        "0 = auto",
        "Inventory Scale Amount"
    )

    override fun initialize() {
        on<GuiScaleEvent> { event ->
            if (event.screen !is ContainerScreen && event.screen !is InventoryScreen) return@on

            event.setScale(SETTING_SCALE.get().roundToInt())
        }.prio = 1
    }

    fun getScale() = SETTING_SCALE.get().roundToInt()
}