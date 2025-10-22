package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.devonian.api.events.PacketReceivedEvent
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.api.Scheduler
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityType
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket

object RemoveDamageTag : Feature("removeDamageTag", "catacombs") {
    private var damageTagRegex = "^.?\\d[\\d,.]+.*?\$".toRegex()

    override fun initialize() {
        on<PacketReceivedEvent> { event ->
            val packet = event.packet
            if (packet !is EntitySpawnS2CPacket) return@on
            if (packet.entityType !== EntityType.ARMOR_STAND) return@on

            Scheduler.scheduleTask {
                val world = minecraft.world ?: return@scheduleTask
                val entityId = packet.entityId
                val entity = world.getEntityById(entityId) ?: return@scheduleTask
                val name = entity.customName?.string ?: return@scheduleTask
                if (!name.matches(damageTagRegex)) return@scheduleTask

                world.removeEntity(entityId, Entity.RemovalReason.DISCARDED)
            }
        }
    }
}