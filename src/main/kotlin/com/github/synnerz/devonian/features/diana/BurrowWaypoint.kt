package com.github.synnerz.devonian.features.diana

import com.github.synnerz.barrl.Context
import com.github.synnerz.devonian.api.events.PacketReceivedEvent
import com.github.synnerz.devonian.api.events.PacketSentEvent
import com.github.synnerz.devonian.api.events.RenderWorldEvent
import com.github.synnerz.devonian.features.Feature
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket
import java.awt.Color
import kotlin.math.floor

object BurrowWaypoint : Feature(
    "burrowWaypoint",
    "Adds a waypoint with the type of burrow whenever the particles are detected",
    "Diana",
    "hub"
) {
    private val SETTING_START_COLOR = Color.GREEN
    private val SETTING_MOB_COLOR = Color.RED
    private val SETTING_TREAURE_COLOR = Color.YELLOW

    override fun initialize() {
        on<PacketReceivedEvent> { event ->
            val packet = event.packet
            if (packet !is ClientboundLevelParticlesPacket || packet.yDist != 0.1f) return@on
            val particle = packet.particle.type
            val waypointType = when {
                particle == ParticleTypes.ENCHANTED_HIT &&
                packet.count == 4 &&
                packet.xDist == 0.5f &&
                packet.zDist == 0.5f -> BurrowManager.BurrowType.START

                particle == ParticleTypes.CRIT &&
                packet.count == 3 &&
                packet.xDist == 0.5f &&
                packet.zDist == 0.5f -> BurrowManager.BurrowType.MOB

                particle == ParticleTypes.DRIPPING_LAVA &&
                packet.count == 2 &&
                packet.xDist == 0.35f &&
                packet.zDist == 0.35f -> BurrowManager.BurrowType.TREASURE

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
            if (packet !is ServerboundPlayerActionPacket) return@on
            val action = packet.action
            if (action != ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK) return@on

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
                        else -> Color(0, true)
                    },
                    it.type.displayName,
                    increase = true,
                    phase = true
                )
            }
        }
    }
}