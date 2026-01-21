package com.github.synnerz.devonian.features.misc.inventory

import com.github.synnerz.devonian.api.dungeon.Dungeons
import com.github.synnerz.devonian.api.events.DropItemEvent
import com.github.synnerz.devonian.features.Feature

object PreventDroppingHotbar : Feature(
    "preventDroppingHotbar",
    "",
    subcategory = "Inventory",
) {
    override fun initialize() {
        on<PreventItem.SlotEvent> { event ->
            val evn = event.underlying as? DropItemEvent ?: return@on
            if (evn.willDropInDungeons) return@on
            if (Dungeons.started.value) return@on
            event.cancel("DropHotbar")
        }
    }
}