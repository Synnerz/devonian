package com.github.synnerz.devonian.features.misc

import com.github.synnerz.devonian.api.ItemUtils
import com.github.synnerz.devonian.api.events.BlockInteractEvent
import com.github.synnerz.devonian.features.Feature

object PreventPlacingWeapons : Feature("preventPlacingWeapons") {
    val weaponIds = mutableListOf(
        "FLOWER_OF_TRUTH",
        "BOUQUET_OF_LIES",
        "MOODY_GRAPPLESHOT",
        "BAT_WAND",
        "STARRED_BAT_WAND",
        "WEIRD_TUBA",
        "WEIRDER_TUBA",
        "PUMPKIN_LAUNCHER",
        "FIRE_FREEZE_STAFF"
    )

    override fun initialize() {
        on<BlockInteractEvent> { event ->
            if (minecraft.world?.getBlockState(event.pos) == null) return@on
            val itemId = ItemUtils.skyblockId(event.itemStack) ?: return@on
            if (!weaponIds.any { it == itemId }) return@on

            event.cancel()
        }
    }
}