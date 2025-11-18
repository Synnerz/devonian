package com.github.synnerz.devonian.features.inventory

import com.github.synnerz.devonian.api.ItemUtils
import com.github.synnerz.devonian.api.ScreenUtils
import com.github.synnerz.devonian.api.events.GuiSlotClickEvent
import com.github.synnerz.devonian.features.Feature
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen

object MiddleClickGui : Feature(
    "middleClickGui",
    "Cancels your left clicks and turns it into a middle clicks on certain guis"
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

    override fun initialize() {
        on<GuiSlotClickEvent> { event ->
            if (event.slot == null || event.mbtn != 0 || !event.slot.hasItem()) return@on
            val stack = event.slot.item
            if (event.slot.container == minecraft.player?.inventory) return@on
            val screen = minecraft.screen ?: return@on
            val screenName = (screen as AbstractContainerScreen<*>).title?.string ?: return@on
            if (ItemUtils.skyblockId(stack) != null) return@on
            if (avoidGuis.any { screenName.startsWith(it) }) return@on
            if (stack.itemName.string == "Reforge Item" || stack.itemName.string == "Salvage Item") return@on

            event.cancel()
            ScreenUtils.click(event.slotId, false, "MIDDLE")
        }
    }
}