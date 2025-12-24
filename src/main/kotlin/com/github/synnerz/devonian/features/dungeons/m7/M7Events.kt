package com.github.synnerz.devonian.features.dungeons.m7

import com.github.synnerz.devonian.api.dungeon.Stages
import com.github.synnerz.devonian.api.events.Event
import com.github.synnerz.devonian.api.events.EventBus
import com.github.synnerz.devonian.api.events.PacketReceivedEvent
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket

object M7Events {
    class DragonSpawned(val dragon: M7Dragon, val isHigh: Boolean) : Event()
    class DragonSpawned2(val dragon: M7Dragon, val isHigh: Boolean) : Event()

    fun init() {
        EventBus.on<PacketReceivedEvent> { event ->
            val packet = event.packet as? ClientboundLevelParticlesPacket ?: return@on

            if (packet.particle.type != ParticleTypes.FLAME) return@on
            if (packet.count != 20) return@on
            if (packet.xDist != 2f) return@on
            if (packet.yDist != 3f) return@on
            if (packet.zDist != 2f) return@on
            if (!packet.alwaysShow()) return@on
            if (!packet.isOverrideLimiter) return@on

            val x = packet.x.toInt()
            val y = packet.y.toInt()
            val z = packet.z.toInt()
            val isHigh = when (y) {
                19 -> false
                27 -> true
                else -> return@on
            }

            val dragon = M7Dragon.entries.find { it.chin.x == x && it.chin.z == z } ?: return@on

            DragonSpawned(dragon, isHigh).post()
        }.setEnabled(Stages.WitherKing.isActiveState)
    }
}