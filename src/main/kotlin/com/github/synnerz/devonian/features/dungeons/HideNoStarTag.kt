package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.devonian.api.Scheduler
import com.github.synnerz.devonian.api.events.PacketReceivedEvent
import com.github.synnerz.devonian.features.Feature
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType

object HideNoStarTag : Feature(
    "hideNoStarTag",
    "Hides name tag of mobs that do not have star in their name tag",
    "Dungeons",
    "catacombs"
) {
    private val blazeHealthRegex = "^\\[Lv15] . Blaze [\\d,]+/([\\d,]+)❤$".toRegex()
    private val noStarTagRegex = "^(?:\\[Lv\\d+] )?(?:[༕⛏\uD83E\uDDB4☠]+)?[\\w ]+ [\\d,.]+\\w(?:/[\\d,.]+\\w)?❤$".toRegex()

    override fun initialize() {
        on<PacketReceivedEvent> { event ->
            val packet = event.packet
            if (packet !is ClientboundAddEntityPacket) return@on
            if (packet.type !== EntityType.ARMOR_STAND) return@on

            Scheduler.scheduleTask {
                val world = minecraft.level ?: return@scheduleTask
                val entityId = packet.id
                val entity = world.getEntity(entityId) ?: return@scheduleTask
                val name = entity.customName?.string ?: return@scheduleTask
                if (name.matches(blazeHealthRegex) || !name.matches(noStarTagRegex)) return@scheduleTask

                world.removeEntity(entityId, Entity.RemovalReason.DISCARDED)
            }
        }
    }
}