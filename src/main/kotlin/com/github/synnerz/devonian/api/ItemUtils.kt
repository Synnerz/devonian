package com.github.synnerz.devonian.api

import net.minecraft.core.component.DataComponents
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.item.ItemStack

object ItemUtils {
    @SuppressWarnings("deprecation")
    fun skyblockId(itemStack: ItemStack): String? {
        val nbt = itemStack.get(DataComponents.CUSTOM_DATA)?.unsafe ?: return null
        val itemId = nbt.getString("id")
        if (itemId.isEmpty) return null

        return itemId.get()
    }

    fun extraAttributes(itemStack: ItemStack): CompoundTag? {
        return itemStack.get(DataComponents.CUSTOM_DATA)?.unsafe
    }

    fun uuid(itemStack: ItemStack): String? {
        val uuid = extraAttributes(itemStack)?.getString("uuid") ?: return null
        if (uuid.isEmpty) return null

        return uuid.get()
    }

    fun lore(itemStack: ItemStack): List<String>? {
        val lore = itemStack.get(DataComponents.LORE)?.lines ?: return null

        return lore.map { it.string }
    }
}