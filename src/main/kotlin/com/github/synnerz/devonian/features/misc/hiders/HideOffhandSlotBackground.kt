package com.github.synnerz.devonian.features.misc.hiders

import com.github.synnerz.devonian.api.events.RenderSlotEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import net.minecraft.world.inventory.InventoryMenu

object HideOffhandSlotBackground : Feature(
    "hideOffhandSlotBackground",
    "in inventory",
    Categories.VANILLA_TWEAKS,
    subcategory = "Hider",
) {
    override fun initialize() {
        on<RenderSlotEvent> { event ->
            if (event.slot.noItemIcon == InventoryMenu.EMPTY_ARMOR_SLOT_SHIELD) event.cancel()
        }
    }
}