package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.devonian.api.events.*
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.utils.math.MathUtils
import com.github.synnerz.devonian.utils.render.Render3DImmediate
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.network.protocol.common.ClientboundPingPacket
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket
import java.awt.Color
import java.util.BitSet
import java.util.concurrent.ConcurrentHashMap
import kotlin.Boolean
import kotlin.Double
import kotlin.Int
import kotlin.collections.ArrayDeque
import kotlin.collections.find
import kotlin.collections.forEach
import kotlin.collections.forEachIndexed
import kotlin.collections.getOrPut
import kotlin.collections.mutableListOf
import kotlin.math.abs
import kotlin.math.sqrt

object CustomMageBeam : Feature(
    "customMageBeam",
    "(has the unfortunate side effect of delaying all non-mage beam firework particles by 1 tick)",
    Categories.DUNGEONS,
    "catacombs",
    subcategory = "QOL"
) {
    private val SETTING_ONLY_CANCEL_PARTICLES = addSwitch(
        "onlyCancelParticles",
        false,
        "Whether to just cancel the particles and not render a custom line for the beam",
        "Only Cancel Particles"
    )
    private val SETTING_LINE_WIDTH = addSlider(
        "lineWidth",
        2.0,
        1.0, 10.0,
        "",
        "Line Width",
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

    private val beamParticles = ConcurrentHashMap<Int, ArrayDeque<ClientboundLevelParticlesPacket>>()
    private var currId = 0
    private val renderedBeams = mutableListOf<MageBeam>()

    private data class MageBeam(
        val x0: Double, val y0: Double, val z0: Double,
        var x1: Double, var y1: Double, var z1: Double,
        var tick: Int,
    )

    override fun initialize() {
        on<PacketReceivedEvent> { event ->
            val packet = event.packet

            if (packet is ClientboundPingPacket) {
                val id = packet.id
                if (id < 0) currId = id
                return@on
            }

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

            beamParticles.getOrPut(currId) { ArrayDeque() }.add(packet)
            event.cancel()
        }

        on<ClientThreadServerTickEvent> { event ->
            val tick = EventBus.serverTicks()
            renderedBeams.removeIf { tick - it.tick > SETTING_TIME_TO_FADE.get() / 0.05 }

            val parts = beamParticles.remove(event.action + 1) ?: return@on
            if (parts.isEmpty()) return@on

            val newBeams = mutableListOf<MageBeam>()
            val dontSend = BitSet(parts.size)
            for (iter in 0 .. 1) {
                val step = if (iter == 0) 1 else 9

                var i = 0
                while (i < parts.size - step * 2) {
                    if (dontSend.get(i) || dontSend.get(i + step) || dontSend.get(i + step * 2)) {
                        i++
                        continue
                    }
                    val p0x = parts[i + 0].x
                    val p0y = parts[i + 0].y
                    val p0z = parts[i + 0].z
                    val p1x = parts[i + step].x
                    val p1y = parts[i + step].y
                    val p1z = parts[i + step].z
                    val d01 = sqrt(
                        (p0x - p1x) * (p0x - p1x) +
                        (p0y - p1y) * (p0y - p1y) +
                        (p0z - p1z) * (p0z - p1z)
                    )
                    if (
                        d01 +
                        sqrt(
                            (p1x - parts[i + step * 2].x) * (p1x - parts[i + step * 2].x) +
                            (p1y - parts[i + step * 2].y) * (p1y - parts[i + step * 2].y) +
                            (p1z - parts[i + step * 2].z) * (p1z - parts[i + step * 2].z)
                        ) >
                        sqrt(
                            (p0x - parts[i + step * 2].x) * (p0x - parts[i + step * 2].x) +
                            (p0y - parts[i + step * 2].y) * (p0y - parts[i + step * 2].y) +
                            (p0z - parts[i + step * 2].z) * (p0z - parts[i + step * 2].z)
                        ) +
                        1e-3
                    ) {
                        i++
                        continue
                    }

                    val arrX = mutableListOf(parts[i + 0].x, parts[i + step].x, parts[i + step * 2].x)
                    val arrY = mutableListOf(parts[i + 0].y, parts[i + step].y, parts[i + step * 2].y)
                    val arrZ = mutableListOf(parts[i + 0].z, parts[i + step].z, parts[i + step * 2].z)

                    val start = i
                    i += step * 3
                    var count = 0
                    while (i < parts.size) {
                        if (dontSend.get(i)) break

                        val x = parts[i].x
                        val y = parts[i].y
                        val z = parts[i].z

                        if (
                            d01 +
                            sqrt(
                                (p1x - x) * (p1x - x) +
                                (p1y - y) * (p1y - y) +
                                (p1z - z) * (p1z - z)
                            ) >
                            sqrt(
                                (p0x - x) * (p0x - x) +
                                (p0y - y) * (p0y - y) +
                                (p0z - z) * (p0z - z)
                            ) +
                            1e-2
                        ) break

                        arrX.add(x)
                        arrY.add(y)
                        arrZ.add(z)
                        count++
                        i += step
                    }

                    val end = i
                    if (step != 1) i = start + 1
                    if (count < 6) continue

                    if (step == 1) dontSend.set(start, end)
                    else for (j in start until end step step) dontSend.set(j)

                    val fit = MathUtils.fitLine3D(arrX, arrY, arrZ)
                    val x1 = fit[0][0]
                    val y1 = fit[1][0]
                    val z1 = fit[2][0]
                    val vx = fit[0][1]
                    val vy = fit[1][1]
                    val vz = fit[2][1]

                    val x0 = parts[start].x
                    val y0 = parts[start].y
                    val z0 = parts[start].z
                    val u0 = -((x1 - x0) * vx + (y1 - y0) * vy + (z1 - z0) * vz) / (vx * vx + vy * vy + vz * vz)
                    val gx0 = x1 + vx * u0
                    val gy0 = y1 + vy * u0
                    val gz0 = z1 + vz * u0

                    val xf = parts[end - step].x
                    val yf = parts[end - step].y
                    val zf = parts[end - step].z
                    val uf = -((x1 - xf) * vx + (y1 - yf) * vy + (z1 - zf) * vz) / (vx * vx + vy * vy + vz * vz)
                    val gxf = x1 + vx * uf
                    val gyf = y1 + vy * uf
                    val gzf = z1 + vz * uf

                    newBeams.add(MageBeam(gx0, gy0, gz0, gxf, gyf, gzf, tick))
                }
            }

            newBeams.forEach { beam ->
                val (x0, y0, z0, x1, y1, z1, tick) = beam
                val ux = x1 - x0
                val uy = y1 - y0
                val uz = z1 - z0
                val matching = renderedBeams.find {
                    val vx = it.x1 - it.x0
                    val vy = it.y1 - it.y0
                    val vz = it.z1 - it.z0

                    val LinfCross = abs(uy * vz - uz * vy) + abs(uz * vx - ux * vz) + abs(ux * vy - uy * vx)
                    if (LinfCross > 1e-2) return@find false

                    if (ux * vx + uy * vy + uz * vz < 0.0) return@find false

                    val u = -((x0 - it.x0) * ux + (y0 - it.y0) * uy + (z0 - it.z0) * uz) / (ux * ux + uy * uy + uz * uz)
                    val ix = x0 + ux * u
                    val iy = y0 + uy * u
                    val iz = z0 + uz * u
                    return@find (
                        abs(ix - it.x0) +
                        abs(iy - it.y0) +
                        abs(iz - it.z0)
                    ) < 1e-1
                }

                if (matching == null || tick - matching.tick > 5) renderedBeams.add(beam)
                else {
                    matching.x1 = x1
                    matching.y1 = y1
                    matching.z1 = z1
                    matching.tick = tick
                }
            }

            val conn = minecraft.connection ?: return@on
            parts.forEachIndexed { i, v ->
                if (dontSend.get(i)) return@forEachIndexed

                val x = v.x
                val y = v.y
                val z = v.z

                val match = renderedBeams.find {
                    val x0 = it.x0
                    val y0 = it.y0
                    val z0 = it.z0
                    val vx = it.x1 - x0
                    val vy = it.y1 - y0
                    val vz = it.z1 - z0
                    val u = -((x0 - x) * vx + (y0 - y) * vy + (z0 - z) * vz) / (vx * vx + vy * vy + vz * vz)
                    val gx = x0 + vx * u
                    val gy = y0 + vy * u
                    val gz = z0 + vz * u
                    return@find (
                        (x - gx) * (x - gx) +
                        (y - gy) * (y - gy) +
                        (z - gz) * (z - gz)
                    ) < 1.0
                }

                if (match == null) v.handle(conn)
            }
        }

        on<RenderWorldEvent> {
            val c0 = SETTING_START_COLOR.getColor()
            val c1 = SETTING_END_COLOR.getColor()
            Render3DImmediate.renderLines(c0.alpha == 255 && c1.alpha == 255, SETTING_LINE_WIDTH.get()) {
                renderedBeams.forEach {
                    submit(
                        it.x0, it.y0, it.z0,
                        it.x1, it.y1, it.z1,
                        c0, c1,
                    )
                }
            }
        }.setEnabled(SETTING_ONLY_CANCEL_PARTICLES.state.map(Boolean::not))
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        beamParticles.clear()
        renderedBeams.clear()
    }
}