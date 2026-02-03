package com.github.synnerz.devonian.features.dungeons.m7

import com.github.synnerz.devonian.api.Scheduler
import com.github.synnerz.devonian.api.dungeon.Dungeons
import com.github.synnerz.devonian.api.dungeon.FloorType
import com.github.synnerz.devonian.api.dungeon.Stages
import com.github.synnerz.devonian.api.events.PacketReceivedEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.utils.BasicState
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
    subcategory = "M7",
) {
    override fun createRequirements(): List<BasicState<Boolean>?> {
        return super.createRequirements() + listOf(Stages.F7.isActiveState, Dungeons.floorState.map { it == FloorType.M7 })
    }

    override fun initialize() {
        on<PacketReceivedEvent> { event ->
            when (val packet = event.packet) {
                is ClientboundAddEntityPacket -> {
                    if (packet.type != EntityType.ARMOR_STAND) return@on
                    if (packet.y !in 9.0 .. 25.0 || packet.z > 45.0) return@on
                    Scheduler.scheduleAfterPacket {
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