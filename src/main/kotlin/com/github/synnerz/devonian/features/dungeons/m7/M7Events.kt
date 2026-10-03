package com.github.synnerz.devonian.features.dungeons.m7

import com.github.synnerz.devonian.Devonian
import com.github.synnerz.devonian.api.Scheduler
import com.github.synnerz.devonian.api.dungeon.DungeonClass
import com.github.synnerz.devonian.api.dungeon.Dungeons
import com.github.synnerz.devonian.api.dungeon.Stages
import com.github.synnerz.devonian.api.events.*
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket
import net.minecraft.world.entity.EntityTypes
import net.minecraft.world.entity.boss.enderdragon.EnderDragon
import java.util.*
import kotlin.math.abs

object M7Events {
    @Threaded class DragonParticles(val dragon: M7DragonSpawn) : Event
    class ActiveDragonChanged(val dragon: M7DragonSpawn) : Event
    class DragonSpawned(val dragon: M7DragonSpawn, val ent: EnderDragon) : Event
    class DragonDeath(val dragon: M7Dragon, val ent: EnderDragon) : Event

    val cooldown = EnumMap<M7Dragon, Int>(M7Dragon::class.java)
    var count = 0
    val queuedDrags = mutableListOf<M7DragonSpawn>()
    val aliveDrags = mutableListOf<Pair<M7DragonSpawn, EnderDragon>>()

    fun init() {
        EventBus.on<PacketReceivedEvent> { event ->
            when (val packet = event.packet) {
                is ClientboundLevelParticlesPacket -> {
                    if (packet.particle.type != ParticleTypes.FLAME) return@on
                    if (packet.count != 20) return@on
                    if (packet.xDist != 2f) return@on
                    if (packet.yDist != 3f) return@on
                    if (packet.zDist != 2f) return@on
                    if (packet.xMaxSpeed != 0f) return@on
                    if (!packet.alwaysShow()) return@on
                    if (!packet.overrideLimiter) return@on

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
                    if (cd != -1 && tick < cd) return@on

                    cooldown[dragon] = tick + 100

                    val c = count++
                    if (c == 0 && Dungeons.selfPlayer.role == DungeonClass.Healer) return@on
                    if (c == 1 && Dungeons.selfPlayer.role == DungeonClass.Tank) return@on

                    val drag = M7DragonSpawn(dragon, isHigh, tick + 100)
                    DragonParticles(drag).post()
                    println("DEBUGPRINT PARTICLES $tick")

                    Scheduler.scheduleTask {
                        queuedDrags.add(drag)
                        if (queuedDrags.size == 1) ActiveDragonChanged(drag).post()
                    }
                }

                is ClientboundAddEntityPacket -> {
                    if (packet.type != EntityTypes.ENDER_DRAGON) return@on

                    val type = M7Dragon.entries.minBy {
                        abs(it.path[0].x - packet.x) +
                        abs(it.path[0].y - packet.y) +
                        abs(it.path[0].z - packet.z)
                    }

                    Scheduler.scheduleAfterPacket {
                        val ent = Devonian.minecraft.level?.getEntity(packet.id) as? EnderDragon? ?: return@scheduleAfterPacket
                        val drag = queuedDrags.find { it.type == type } ?: return@scheduleAfterPacket

                        DragonSpawned(drag, ent).post()
                        println("DEBUGPRINT SPAWN ${EventBus.serverTicks()}")
                        aliveDrags.add(drag to ent)
                    }
                }
            }
        }.setEnabled(Stages.WitherKing.isActiveState)

        EventBus.on<TickEvent> {
            aliveDrags.removeIf { (drag, ent) ->
                if (ent.isRemoved) {
                    queuedDrags.remove(drag)
                    return@removeIf true
                }
                if (ent.dragonDeathTime <= 0) return@removeIf false

                DragonDeath(drag.type, ent)
                val f = queuedDrags.firstOrNull()
                queuedDrags.remove(drag)
                if (f != null) queuedDrags.firstOrNull()?.let {
                    ActiveDragonChanged(it).post()
                }

                return@removeIf true
            }
        }.setEnabled(Stages.WitherKing.isActiveState)

        EventBus.on<WorldChangeEvent> {
            cooldown.clear()
            count = 0
            queuedDrags.clear()
            aliveDrags.clear()
        }
    }
}