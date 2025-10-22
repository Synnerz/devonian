package com.github.synnerz.devonian.api

import net.minecraft.component.DataComponentTypes
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound

object ItemUtils {
    @SuppressWarnings("deprecation")
    fun skyblockId(itemStack: ItemStack): String? {
        val nbt = itemStack.get(DataComponentTypes.CUSTOM_DATA)?.nbt ?: return null
        val itemId = nbt.getString("id")
        if (itemId.isEmpty) return null

        return itemId.get()
    }

    fun extraAttributes(itemStack: ItemStack): NbtCompound? {
        return itemStack.get(DataComponentTypes.CUSTOM_DATA)?.nbt
    }

    fun uuid(itemStack: ItemStack): String? {
        val uuid = extraAttributes(itemStack)?.getString("uuid") ?: return null
        if (uuid.isEmpty) return null

        return uuid.get()
    }

    fun lore(itemStack: ItemStack): List<String>? {
        val lore = itemStack.get(DataComponentTypes.LORE)?.lines ?: return null

        return lore.map { it.string }
    }
}