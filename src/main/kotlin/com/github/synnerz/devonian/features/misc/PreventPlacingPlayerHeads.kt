package com.github.synnerz.devonian.features.misc

import com.github.synnerz.devonian.api.ItemUtils
import com.github.synnerz.devonian.api.events.BlockInteractEvent
import com.github.synnerz.devonian.features.Feature
import net.minecraft.world.item.Items

object PreventPlacingPlayerHeads : Feature(
    "preventPlacingPlayerHeads",
    "Stops Player Heads from being placeable.",
    subcategory = "Tweaks",
) {
    override fun initialize() {
        on<BlockInteractEvent> { event ->
            if (minecraft.level?.getBlockState(event.pos) == null) return@on
            val itemStack = event.itemStack
            if (ItemUtils.skyblockId(itemStack) == null || itemStack.item != Items.PLAYER_HEAD) return@on
            val lore = ItemUtils.lore(itemStack) ?: return@on

            if (lore.any { it.contains("RIGHT CLICK") || it.contains("Right-click") })
                event.cancel()
        }
    }
}