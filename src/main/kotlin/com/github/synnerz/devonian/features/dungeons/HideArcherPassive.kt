package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.devonian.api.Scheduler
import com.github.synnerz.devonian.api.events.EntityDataEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityTypes
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items

object HideArcherPassive : Feature(
    "hideArcherPassive",
    "bonemeal go spin",
    Categories.DUNGEONS,
    "catacombs",
    subcategory = "Hiders",
) {
    override fun initialize() {
        on<EntityDataEvent> { event ->
            if (event.type != EntityTypes.ITEM) return@on

            event.data.forEach {
                if (it.id != 8) return@forEach
                val stack = it.value as? ItemStack ?: return@on
                if (stack.isEmpty) return@on
                if (stack.item === Items.BONE_MEAL) Scheduler.scheduleAfterPacket {
                    minecraft.level?.removeEntity(event.entityId, Entity.RemovalReason.DISCARDED)
                }
            }
        }
    }
}