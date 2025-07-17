package com.github.synnerz.devonian.features.misc

import com.github.synnerz.devonian.events.GuiSlotClickEvent
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.utils.ItemUtils
import com.github.synnerz.devonian.utils.ScreenUtils
import net.minecraft.client.gui.screen.ingame.HandledScreen

// FIXME: whenever left clicking an item into a chest it does not register
//  due to it being changed to middle click
object MiddleClickGui : Feature("middleClickGui") {
    val avoidGuis = mutableListOf(
        "Wardrobe",
        "Drill Anvil",
        "Anvil",
        "Storage",
        "The Hex",
        "Composter",
        "Auctions",
        "Abiphone"
    )

    override fun initialize() {
        on<GuiSlotClickEvent> { event ->
            if (event.slot == null || event.mbtn != 0) return@on
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