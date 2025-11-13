package com.github.synnerz.devonian.features.diana

import com.github.synnerz.barrl.Context
import com.github.synnerz.devonian.api.events.PacketReceivedEvent
import com.github.synnerz.devonian.api.events.PacketSentEvent
import com.github.synnerz.devonian.api.events.RenderWorldEvent
import com.github.synnerz.devonian.features.WorldFeature
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket
import net.minecraft.network.packet.s2c.play.ParticleS2CPacket
import net.minecraft.particle.ParticleTypes
import java.awt.Color
import kotlin.math.floor

object BurrowWaypoint : WorldFeature("burrowWaypoint", "hub") {
    private val SETTING_START_COLOR = Color.GREEN
    private val SETTING_MOB_COLOR = Color.RED
    private val SETTING_TREAURE_COLOR = Color.YELLOW

    override fun initialize() {
        on<PacketReceivedEvent> { event ->
            val packet = event.packet
            if (packet !is ParticleS2CPacket || packet.offsetY != 0.1f) return@on
            val particle = packet.parameters.type
            val waypointType = when {
                particle == ParticleTypes.ENCHANTED_HIT &&
                packet.count == 4 &&
                packet.offsetX == 0.5f &&
                packet.offsetZ == 0.5f -> BurrowManager.BurrowType.START

                particle == ParticleTypes.CRIT &&
                packet.count == 3 &&
                packet.offsetX == 0.5f &&
                packet.offsetZ == 0.5f -> BurrowManager.BurrowType.MOB

                particle == ParticleTypes.DRIPPING_LAVA &&
                packet.count == 2 &&
                packet.offsetX == 0.35f &&
                packet.offsetZ == 0.35f -> BurrowManager.BurrowType.TREASURE

                else -> null
            }
            if (waypointType == null) return@on

            BurrowManager.addBurrow(
                waypointType,
                floor(packet.x),
                floor(packet.y) - 1,
                floor(packet.z)
            )
        }

        on<PacketSentEvent> { event ->
            val packet = event.packet
            if (packet !is PlayerActionC2SPacket) return@on
            val action = packet.action
            if (action != PlayerActionC2SPacket.Action.START_DESTROY_BLOCK) return@on

            BurrowManager.digBurrow(packet.pos)
        }

        on<RenderWorldEvent> {
            BurrowManager.burrows.forEach {
                if (!it.type.empirical) return@forEach

                Context.Immediate?.renderWaypoint(
                    it.x, it.y, it.z,
                    when (it.type) {
                        BurrowManager.BurrowType.START -> SETTING_START_COLOR
                        BurrowManager.BurrowType.MOB -> SETTING_MOB_COLOR
                        BurrowManager.BurrowType.TREASURE -> SETTING_TREAURE_COLOR
                        else -> Color(0)
                    },
                    it.type.displayName,
                    increase = true,
                    phase = true
                )
            }
        }
    }
}