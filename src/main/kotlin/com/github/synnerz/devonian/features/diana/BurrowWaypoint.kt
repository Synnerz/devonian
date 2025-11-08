package com.github.synnerz.devonian.features.diana

import com.github.synnerz.barrl.Context
import com.github.synnerz.devonian.api.Scheduler
import com.github.synnerz.devonian.api.events.PacketReceivedEvent
import com.github.synnerz.devonian.api.events.PacketSentEvent
import com.github.synnerz.devonian.api.events.RenderWorldEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.features.WorldFeature
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket
import net.minecraft.network.packet.s2c.play.ParticleS2CPacket
import net.minecraft.particle.ParticleTypes
import net.minecraft.util.math.BlockPos
import java.awt.Color
import kotlin.math.floor

object BurrowWaypoint : WorldFeature("burrowWaypoint", "hub") {
    private val waypoints = mutableListOf<Burrow>()

    data class Burrow(val name: String, val x: Int, val y: Int, val z: Int, val color: Color) {
        fun eqBp(bp: BlockPos): Boolean {
            return bp.x == x && bp.y == y - 1 && bp.z == z
        }
    }

    enum class BurrowTypes(val displayName: String, val color: Color) {
        START("§aStart", Color.GREEN),
        MOB("§cMob", Color.RED),
        TREASURE("§eTreasure", Color.YELLOW)
    }

    override fun initialize() {
        on<PacketReceivedEvent> { event ->
            val packet = event.packet
            if (packet !is ParticleS2CPacket || packet.offsetY != 0.1f) return@on
            val particle = packet.parameters.type
            val waypointType = when {
                particle == ParticleTypes.ENCHANTED_HIT &&
                        packet.count == 4 &&
                        packet.offsetX == 0.5f &&
                        packet.offsetZ == 0.5f -> BurrowTypes.START

                particle == ParticleTypes.CRIT &&
                        packet.count == 3 &&
                        packet.offsetX == 0.5f &&
                        packet.offsetZ == 0.5f -> BurrowTypes.MOB

                particle == ParticleTypes.DRIPPING_LAVA &&
                        packet.count == 2 &&
                        packet.offsetX == 0.35f &&
                        packet.offsetZ == 0.35f -> BurrowTypes.TREASURE

                else -> null
            }
            if (waypointType == null) return@on
            val burrowToAdd = Burrow(
                waypointType.displayName,
                floor(packet.x).toInt(),
                floor(packet.y).toInt(),
                floor(packet.z).toInt(),
                waypointType.color
            )
            if (waypoints.contains(burrowToAdd)) return@on

            waypoints.add(burrowToAdd)
        }

        on<PacketSentEvent> { event ->
            val packet = event.packet
            if (packet !is PlayerActionC2SPacket) return@on
            val action = packet.action
            if (action != PlayerActionC2SPacket.Action.START_DESTROY_BLOCK) return@on

            Scheduler.scheduleTask(20) {
                waypoints.removeIf {
                    return@removeIf it.eqBp(packet.pos)
                }
            }
        }

        on<RenderWorldEvent> {
            waypoints.forEach {
                Context.Immediate?.renderWaypoint(
                    it.x.toDouble(), it.y - 1.0, it.z.toDouble(),
                    it.color,
                    it.name,
                    increase = true,
                    phase = true
                )
            }
        }
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        waypoints.clear()
    }
}