package com.github.synnerz.devonian.features.misc

import com.github.synnerz.devonian.api.ItemUtils
import com.github.synnerz.devonian.api.events.BlockInteractEvent
import com.github.synnerz.devonian.features.Feature
import net.minecraft.item.Items

object PreventPlacingPlayerHeads : Feature("preventPlacingPlayerHeads") {
    override fun initialize() {
        on<BlockInteractEvent> { event ->
            if (minecraft.world?.getBlockState(event.pos) == null) return@on
            if (ItemUtils.skyblockId(event.itemStack) == null || event.itemStack.item != Items.PLAYER_HEAD) return@on

            event.cancel()
        }
    }
}