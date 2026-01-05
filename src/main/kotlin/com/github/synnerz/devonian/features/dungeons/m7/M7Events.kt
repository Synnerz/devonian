package com.github.synnerz.devonian.features.dungeons.m7

import com.github.synnerz.devonian.api.dungeon.Stages
import com.github.synnerz.devonian.api.events.*
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket
import java.util.*

object M7Events {
    @Threaded class DragonSpawned(val dragon: M7Dragon, val isHigh: Boolean) : Event()
    @Threaded class DragonSpawned2(val dragon: M7Dragon, val isHigh: Boolean) : Event()

    val cooldown = EnumMap<M7Dragon, Int>(M7Dragon::class.java)

    fun init() {
        EventBus.on<PacketReceivedEvent> { event ->
            val packet = event.packet as? ClientboundLevelParticlesPacket ?: return@on

            if (packet.particle.type != ParticleTypes.FLAME) return@on
            if (packet.count != 20) return@on
            if (packet.xDist != 2f) return@on
            if (packet.yDist != 3f) return@on
            if (packet.zDist != 2f) return@on
            if (packet.maxSpeed != 0f) return@on
            if (!packet.alwaysShow()) return@on
            if (!packet.isOverrideLimiter) return@on

            val x = packet.x.toInt()
            val y = packet.y.toInt()
            val z = packet.z.toInt()
            if (packet.x % 1 != 0.0 || packet.z % 1 != 0.0) return@on
            val isHigh = when (y) {
                19 -> false
                27 -> true
                else -> return@on
            }

            val dragon = M7Dragon.entries.find { it.particleX == x && it.particleZ == z } ?: return@on
            val tick = EventBus.serverTicks()
            val cd = cooldown.getOrElse(dragon) { -1 }
            if (cd != -1 && tick - cd < 100) return@on

            cooldown[dragon] = tick
            DragonSpawned(dragon, isHigh).post()
        }.setEnabled(Stages.WitherKing.isActiveState)

        EventBus.on<WorldChangeEvent> {
            cooldown.clear()
        }
    }
}