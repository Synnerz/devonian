package com.github.synnerz.devonian.features.misc.hiders

import com.github.synnerz.devonian.api.Scheduler
import com.github.synnerz.devonian.api.events.EntityDataEvent
import com.github.synnerz.devonian.features.Feature
import net.minecraft.core.component.DataComponents
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityTypes
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import java.util.*

object HideCheapCoins : Feature(
    "hideCheapCoins",
    "Prevents coins from coin talismans from rendering.",
    subcategory = "Hiders",
) {
    private val coinUUID = UUID.fromString("b330b74f-2e3b-3fb6-9143-a1f0e63fad59")

    override fun initialize() {
        on<EntityDataEvent> { event ->
            if (event.type != EntityTypes.ITEM) return@on

            event.data.forEach {
                if (it.id != 8) return@forEach

                val stack = it.value as? ItemStack ?: return@on
                if (stack.isEmpty) return@on
                if (stack.item !== Items.PLAYER_HEAD) return@on

                val prof = stack.get(DataComponents.PROFILE) ?: return@on
                if (prof.partialProfile().id == coinUUID) Scheduler.scheduleAfterPacket {
                    minecraft.level?.removeEntity(event.entityId, Entity.RemovalReason.DISCARDED)
                }
            }
        }
    }
}