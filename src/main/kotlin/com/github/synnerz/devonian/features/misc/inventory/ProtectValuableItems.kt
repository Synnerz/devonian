package com.github.synnerz.devonian.features.misc.inventory

import com.github.synnerz.devonian.api.ItemUtils
import com.github.synnerz.devonian.api.SkyblockPrices
import com.github.synnerz.devonian.features.Feature
import com.mojang.blaze3d.platform.InputConstants
import org.lwjgl.glfw.GLFW

object ProtectValuableItems : Feature(
    "protectValuableItems",
    "Protects items above a threshold from being sellable/droppable (holding LSHIFT overrides this)",
    subcategory = "Inventory",
    searchTags = setOf("prevent"),
) {
    private val SETTING_THRESHOLD = addSlider(
        "threshold",
        100.0,
        50.0, 1000.0,
        "The price threshold DO NOTE the value here is multiplied by a thousand so lowest is 50k",
        "Price Threshold"
    )

    override fun initialize() {
        on<PreventItem.SlotEvent> { event ->
            if (!event.losesItem) return@on
            val itemStack = event.item
            val sbId = ItemUtils.skyblockId(itemStack) ?: return@on
            val price = SkyblockPrices.buyPrice(sbId)
            if (price < SETTING_THRESHOLD.get() * 1000f) return@on
            if (InputConstants.isKeyDown(minecraft.window, GLFW.GLFW_KEY_RIGHT_SHIFT)) return@on

            event.cancel("ValuableItems")
        }
    }
}