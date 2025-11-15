package com.github.synnerz.devonian.features.inventory

import com.github.synnerz.devonian.api.ItemUtils
import com.github.synnerz.devonian.api.ScreenUtils
import com.github.synnerz.devonian.api.events.GuiSlotClickEvent
import com.github.synnerz.devonian.features.Feature
import net.minecraft.client.gui.screen.ingame.HandledScreen

object MiddleClickGui : Feature("middleClickGui") {
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
            if (event.slot == null || event.mbtn != 0 || !event.slot.hasStack()) return@on
            val stack = event.slot.stack
            if (event.slot.inventory == minecraft.player?.inventory) return@on
            val screen = minecraft.currentScreen ?: return@on
            val screenName = (screen as HandledScreen<*>).title?.string ?: return@on
            if (ItemUtils.skyblockId(stack) != null) return@on
            if (avoidGuis.any { screenName.startsWith(it) }) return@on
            if (stack.name.string == "Reforge Item" || stack.name.string == "Salvage Item") return@on

            event.cancel()
            ScreenUtils.click(event.slotId, false, "MIDDLE")
        }
    }
}