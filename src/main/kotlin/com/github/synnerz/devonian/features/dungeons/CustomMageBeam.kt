package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.devonian.api.events.EventBus
import com.github.synnerz.devonian.api.events.PacketReceivedEvent
import com.github.synnerz.devonian.api.events.RenderWorldEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent
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
    private val beamsData = CopyOnWriteArrayList<BeamParent>()

    data class BeamParent(val particles: CopyOnWriteArrayList<BeamChild> = CopyOnWriteArrayList(), var lastTick: Int = -1)

    data class BeamChild(val x: Double, val y: Double, val z: Double)

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
                !packet.alwaysShow() ||
                packet.maxSpeed != 0f
            ) return@on

            if (SETTING_ONLY_CANCEL_PARTICLES.get()) {
                event.cancel()
                return@on
            }

            val ticks = EventBus.serverTicks()
            val latest = beamsData.lastOrNull()

            event.cancel()

            if (latest != null && ticks < latest.lastTick + 3) {
                if (latest.particles.size == 7) latest.particles.removeLastOrNull()

                latest.particles.add(BeamChild(packet.x, packet.y, packet.z))
                latest.lastTick = ticks
                return@on
            }

            val beam = BeamParent()
            beam.particles.add(BeamChild(packet.x, packet.y, packet.z))
            beam.lastTick = ticks
            beamsData.add(beam)
        }

        on<RenderWorldEvent> { event ->
            if (SETTING_ONLY_CANCEL_PARTICLES.get()) return@on

            for (data in beamsData) {
                val particles = data.particles

                val current = particles.firstOrNull() ?: continue
                val next = particles.lastOrNull() ?: continue

                BlazeSolver.renderLine(
                    Vec3(current.x, current.y, current.z),
                    Vec3(next.x, next.y, next.z),
                    SETTING_START_COLOR.getColor(),
                    SETTING_END_COLOR.getColor(),
                    event.ctx
                )

                if (EventBus.serverTicks() - data.lastTick > (SETTING_TIME_TO_FADE.get() / 0.05)) {
                    beamsData.remove(data)
                }
            }
        }
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        beamsData.clear()
    }
}