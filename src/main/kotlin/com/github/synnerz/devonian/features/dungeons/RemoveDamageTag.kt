package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.devonian.api.Scheduler
import com.github.synnerz.devonian.api.events.PacketNameChangeEvent
import com.github.synnerz.devonian.features.Feature
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType

object RemoveDamageTag : Feature(
    "removeDamageTag",
    "Removes the damage tags created by you or others.",
    "Dungeons",
    "catacombs"
) {
    private var damageTagRegex = "^.?\\d[\\d,.]+.*?$".toRegex()

    override fun initialize() {
        on<PacketNameChangeEvent> { event ->
            if (event.type !== EntityType.ARMOR_STAND) return@on

            val world = minecraft.level ?: return@on

            if (!event.name.string.matches(damageTagRegex)) return@on

            Scheduler.scheduleTask { world.removeEntity(event.entityId, Entity.RemovalReason.DISCARDED) }
        }
    }
}