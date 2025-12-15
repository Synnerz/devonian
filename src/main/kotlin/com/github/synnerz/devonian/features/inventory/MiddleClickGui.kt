package com.github.synnerz.devonian.features.inventory

import com.github.synnerz.devonian.api.ItemUtils
import com.github.synnerz.devonian.api.ScreenUtils
import com.github.synnerz.devonian.api.events.PickupItemInventoryEvent
import com.github.synnerz.devonian.features.Feature

object MiddleClickGui : Feature(
    "middleClickGui",
    "Cancels your left clicks and turns it into a middle clicks on certain guis",
    subcategory = "Inventory",
) {
    val avoidGuis = mutableListOf(
        "Wardrobe",
        "Drill Anvil",
        "Anvil",
        "Storage",
        "The Hex",
        "Composter",
        "Auctions",
        "Abiphone",
        "Chest",
        "Large Chest",
    )

    val avoidItems = mutableSetOf(
        "Reforge Item",
        "Salvage Item",
    )

    override fun initialize() {
        on<PickupItemInventoryEvent> { event ->
            val slot = event.slot
            if (slot.container === minecraft.player?.inventory) return@on

            val stack = slot.item
            if (stack.isEmpty) return@on
            if (ItemUtils.skyblockId(stack) != null) return@on

            val screenName = event.screen.title?.string ?: return@on
            if (avoidGuis.any { screenName.startsWith(it) }) return@on

            val itemName = stack.itemName.string
            if (avoidItems.contains(itemName)) return@on

            event.cancel()
            ScreenUtils.click(slot.index, false, "MIDDLE")
        }
    }
}