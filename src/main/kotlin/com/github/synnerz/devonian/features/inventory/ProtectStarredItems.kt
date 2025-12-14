package com.github.synnerz.devonian.features.inventory

import com.github.synnerz.devonian.api.ItemUtils
import com.github.synnerz.devonian.features.Feature
import net.minecraft.world.item.ItemStack

object ProtectStarredItems : Feature("protectStarredItems", subcategory = "Inventory") {
    override fun initialize() {
        on<PreventItem.SlotEvent> { event ->
            if (!event.losesItem) return@on

            if (!isStarred(event.item)) return@on

            event.cancel("StarredItems")
        }
    }

    fun isStarred(item: ItemStack): Boolean {
        if (item.isEmpty) return false
        val data = ItemUtils.extraAttributes(item) ?: return false
        val stars = data.getInt("upgrade_level")
        return stars.isPresent && stars.get() > 0
    }
}