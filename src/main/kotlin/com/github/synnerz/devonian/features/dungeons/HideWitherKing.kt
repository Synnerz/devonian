package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.devonian.api.Scheduler
import com.github.synnerz.devonian.api.dungeon.Dungeons
import com.github.synnerz.devonian.api.dungeon.FloorType
import com.github.synnerz.devonian.api.events.PacketReceivedEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType

object HideWitherKing : Feature(
    "hideWitherKing",
    "",
    Categories.DUNGEONS,
    "catacombs",
    subcategory = "Hiders",
) {
    override fun initialize() {
        on<PacketReceivedEvent> { event ->
            if (Dungeons.floor != FloorType.M7) return@on
            if (!Dungeons.inBoss.value) return@on
            when (val packet = event.packet) {
                is ClientboundAddEntityPacket -> {
                    if (packet.type != EntityType.ARMOR_STAND) return@on
                    if (packet.y !in 9.0 .. 25.0 || packet.z > 45.0) return@on
                    Scheduler.scheduleTask(2) {
                        minecraft.level?.removeEntity(packet.id, Entity.RemovalReason.DISCARDED)
                    }
                }

                is ClientboundLevelParticlesPacket -> {
                    if (packet.y !in 9.0 .. 25.0 || packet.z > 45.0) return@on
                    if (packet.particle.type != ParticleTypes.WITCH) return@on
                    event.cancel()
                }
            }
        }
    }
}