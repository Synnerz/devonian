package com.github.synnerz.devonian.utils

import net.minecraft.component.DataComponentTypes
import net.minecraft.item.ItemStack

object ItemUtils {
    @SuppressWarnings("deprecation")
    fun skyblockId(itemStack: ItemStack): String? {
        val nbt = itemStack.get(DataComponentTypes.CUSTOM_DATA)?.nbt ?: return null
        val itemId = nbt.getString("id")
        if (itemId.isEmpty) return null

        return itemId.get()
    }
}