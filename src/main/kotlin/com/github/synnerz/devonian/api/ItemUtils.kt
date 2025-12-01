package com.github.synnerz.devonian.api

import com.github.synnerz.devonian.utils.StringUtils.colorCodes
import net.minecraft.core.component.DataComponents
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.item.ItemStack

object ItemUtils {
    fun skyblockId(itemStack: ItemStack): String? {
        val nbt = extraAttributes(itemStack) ?: return null
        val itemId = nbt.getString("id")
        if (itemId.isEmpty) return null

        return itemId.get()
    }

    fun extraAttributes(itemStack: ItemStack): CompoundTag? {
        return itemStack.get(DataComponents.CUSTOM_DATA)?.copyTag()
    }

    fun uuid(itemStack: ItemStack): String? {
        val uuid = extraAttributes(itemStack)?.getString("uuid") ?: return null
        if (uuid.isEmpty) return null

        return uuid.get()
    }

    fun lore(itemStack: ItemStack, colorCodes: Boolean = false): List<String>? {
        val lore = itemStack.get(DataComponents.LORE)?.lines ?: return null

        if (colorCodes) return lore.map { it.colorCodes() }

        return lore.map { it.string }
    }
}