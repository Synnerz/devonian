package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.devonian.events.PacketReceivedEvent
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.utils.Scheduler
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityType
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket

object HideNoStarTag : Feature("hideNoStarTag", "catacombs") {
    private val blazeHealthRegex = "^\\[Lv15\\] Blaze [\\d,]+\\/([\\d,]+)❤\$".toRegex()
    private val noStarTagRegex = "^(?:\\[Lv\\d+\\] )?[\\w ]+ [\\d,.]+\\w(?:\\/[\\d,.]+\\w)?❤\$".toRegex()

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
                if (name.matches(blazeHealthRegex) || !name.matches(noStarTagRegex)) return@scheduleTask

                world.removeEntity(entityId, Entity.RemovalReason.DISCARDED)
            }
        }
    }
}