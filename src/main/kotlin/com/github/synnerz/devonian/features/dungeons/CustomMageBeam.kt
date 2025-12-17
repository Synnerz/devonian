package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.devonian.api.events.EventBus
import com.github.synnerz.devonian.api.events.PacketReceivedEvent
import com.github.synnerz.devonian.api.events.RenderWorldEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.features.dungeons.solvers.BlazeSolver
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket
import net.minecraft.world.phys.Vec3
import java.awt.Color
import java.util.concurrent.CopyOnWriteArrayList

object CustomMageBeam : Feature(
    "customMageBeam",
    "",
    Categories.DUNGEONS,
    "catacombs",
    subcategory = "QOL"
) {
    // TODO: make this a nested list so it can support more than 1 beam being shown
    private val SETTING_ONLY_CANCEL_PARTICLES = addSwitch(
        "onlyCancelParticles",
        false,
        "Whether to just cancel the particles and not render a custom line for the beam",
        "Only Cancel Particles"
    )
    private val SETTING_START_COLOR = addColorPicker(
        "startColor",
        Color.CYAN.rgb,
        "The starting color for the line",
        "Beam Start Color"
    )
    private val SETTING_END_COLOR = addColorPicker(
        "endColor",
        Color.CYAN.rgb,
        "The ending color for the line",
        "Beam End Color"
    )
    private val SETTING_TIME_TO_FADE = addSlider(
        "timeToFade",
        3.0,
        0.0, 5.0,
        "The time until the rendering line will disappear",
        "Beam Time To Fade"
    )
    private val fireworkList = CopyOnWriteArrayList<FireworkParticleData>()
    private var gatheredAt = -1

    data class FireworkParticleData(val x: Double, val y: Double, val z: Double, val gatheredAt: Int)

    override fun initialize() {
        on<PacketReceivedEvent> { event ->
            val packet = event.packet
            if (packet !is ClientboundLevelParticlesPacket) return@on
            val particle = packet.particle
            if (particle.type != ParticleTypes.FIREWORK) return@on
            if (
                packet.count != 1 ||
                packet.xDist != 0f ||
                packet.yDist != 0f ||
                packet.zDist != 0f ||
                !packet.isOverrideLimiter ||
                !packet.alwaysShow()
            ) return@on

            if (gatheredAt != -1 && EventBus.serverTicks() - gatheredAt > 4) {
                fireworkList.clear()
                gatheredAt = -1
                return@on
            }

            event.cancel()
            if (SETTING_ONLY_CANCEL_PARTICLES.get()) return@on
            fireworkList.add(FireworkParticleData(packet.x, packet.y, packet.z, EventBus.serverTicks()))
            gatheredAt = EventBus.serverTicks()
        }

        on<RenderWorldEvent> { event ->
            if (SETTING_ONLY_CANCEL_PARTICLES.get()) return@on
            val data1 = fireworkList.firstOrNull() ?: return@on
            val data2 = fireworkList.lastOrNull() ?: return@on
            if (data1 == data2) return@on
            if (EventBus.serverTicks() - data2.gatheredAt > (SETTING_TIME_TO_FADE.get() / 0.05)) {
                fireworkList.clear()
                gatheredAt = -1
                return@on
            }

            BlazeSolver.renderLine(
                Vec3(data1.x, data1.y, data1.z),
                Vec3(data2.x, data2.y, data2.z),
                SETTING_START_COLOR.getColor(),
                SETTING_END_COLOR.getColor(),
                event.ctx
            )
        }
    }
}