package com.github.synnerz.devonian.features.misc

import com.github.synnerz.devonian.api.ItemUtils
import com.github.synnerz.devonian.api.events.*
import com.github.synnerz.devonian.hud.texthud.TextHudFeature
import com.github.synnerz.devonian.mixin.accessor.ParticleAccessor
import com.github.synnerz.devonian.utils.BoundingBox
import com.github.synnerz.devonian.utils.StringUtils
import net.minecraft.client.gui.screens.inventory.InventoryScreen
import net.minecraft.core.component.DataComponents
import net.minecraft.core.particles.DustParticleOptions
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.item.Items
import org.joml.Quaternionf
import org.joml.Vector3f
import java.awt.Color
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.ConcurrentSkipListSet
import kotlin.math.*

object Deployables : TextHudFeature(
    "deployables",
    "Show the current deployable you are being buffed by.",
    subcategory = "General",
) {
    private val SETTING_HUD = addSwitch(
        "hud",
        true,
        "",
        "Deployable HUD",
    )
    private val SETTING_COLOR_TIMER = addSwitch(
        "colorTimer",
        false,
        "color timer based on time remaining",
        "Color Deployable Timer",
    )
    private val SETTING_PARTICLES = addSelection(
        "particles",
        1,
        listOf("Default", "Custom", "None"),
        "",
        "Deployable Particles",
    )

    private val jalapenoStats = listOf(
        "§9+5 ☠",
        "§9+1 ☣",
    )

    override fun getBounds(): BoundingBox {
        val bounds = super.getBounds()
        val d = 20.0 * scale
        return BoundingBox(
            bounds.x - d,
            bounds.y + (bounds.h - d) * 0.5,
            bounds.w + d,
            d
        )
    }

    override fun getEditText(): List<String> = listOf("&e42s")

    private val orbs = ConcurrentSkipListSet<DeployableInstance>()
    private var activeOrb: DeployableInstance? = null
    private val orbIds = ConcurrentLinkedQueue<Triple<Int, Int, DeployableInstance>>()
    private val checked = mutableSetOf<Int>()

    override fun initialize() {
        on<TickEvent> {
            if (SETTING_PARTICLES.getCurrent() == "Custom") {
                orbs.forEach {
                    it.spawner.swapPositions()
                    it.spawnParticles()
                }
            }

            var l = orbIds.size
            while (--l >= 0) {
                val (ttl, id, orb) = orbIds.poll() ?: break
                val ent = minecraft.level?.getEntity(id) as? ArmorStand?
                if (ent !== null) {
                    orb.ent = ent
                    orb.y = ent.y + (if (orb.type.isFlare) 0.0 else 1.0)
                    orbs.add(orb)
                } else if (ttl > 0) orbIds.offer(Triple(ttl - 1, id, orb))
            }

            val player = minecraft.player ?: return@on
            activeOrb = orbs.firstOrNull {
                val e = it.ent ?: return@firstOrNull false
                (player.x - e.x).pow(2) +
                (player.y - e.y).pow(2) +
                (player.z - e.z).pow(2) < it.type.rangeSq
            }
            activeOrb?.let {
                setLine(
                    "%s%.0fs".format(
                        if (SETTING_COLOR_TIMER.get()) StringUtils.colorForNumber(it.ttl, it.type.duration)
                        else "&e",
                        ceil(it.ttl / 20.0)
                    )
                )
            }
        }

        on<ClientThreadServerTickEvent> {
            orbs.removeIf {
                --it.ttl <= 0 ||
                !(it.ent?.isAlive ?: false)
            }
        }

        on<RenderOverlayEvent> { event ->
            if (!SETTING_HUD.get()) return@on
            if (activeOrb == null) return@on

            draw(event.ctx)

            val ent = activeOrb?.ent ?: return@on
            val bounds = getBounds()

            val yaw = (System.currentTimeMillis() % 1750L) / 1750.0 * 2.0 * PI
            val entRotation = Quaternionf().rotateZ(PI.toFloat())
            val cameraRotation = Quaternionf()
                .rotateX(-0.38397244f)
                .rotateY(yaw.toFloat())
            entRotation.mul(cameraRotation)

            val f1 = ent.yBodyRot
            val f2 = ent.yRot
            val f3 = ent.xRot
            val f4 = ent.yHeadRotO
            val f5 = ent.yHeadRot

            ent.yBodyRot = 0f
            ent.yRot = 0f
            ent.xRot = 0f
            ent.yHeadRotO = 0f
            ent.yHeadRot = 0f

            val scale = 0.8f
            val entScale = ent.scale
            val renderScale = scale * bounds.h.toFloat() / entScale
            val offset = Vector3f(0f, 2f * entScale * scale, 0f)
            val state = InventoryScreen.extractRenderState(ent)

            event.ctx.entity(
                state,
                renderScale,
                offset,
                entRotation,
                cameraRotation,
                bounds.x.toInt(),
                bounds.y.toInt(),
                ceil(bounds.x + bounds.h).toInt(),
                ceil(bounds.y + bounds.h).toInt(),
            )

            ent.yBodyRot = f1
            ent.yRot = f2
            ent.xRot = f3
            ent.yHeadRotO = f4
            ent.yHeadRot = f5
        }

        on<EntityEquipmentEvent> { event ->
            if (event.type != EntityType.ARMOR_STAND) return@on

            if (!checked.add(event.entityId)) return@on

            if (event.slots.size != 1) return@on
            val change = event.slots.firstOrNull() ?: return@on
            if (change.first != EquipmentSlot.HEAD) return@on

            val item = change.second ?: return@on
            if (item.isEmpty) return@on

            if (item.item != Items.PLAYER_HEAD) return@on

            val type: Deployable?

            val data = ItemUtils.extraAttributes(item)
            if (data !== null) {
                val id = data.getString("id")
                if (id.isEmpty) return@on
                type = Deployable.fromSbId(id.get())
            } else {
                val prof = item.get(DataComponents.PROFILE) ?: return@on
                type = Deployable.fromTexId(prof.partialProfile().id)
            }

            if (type == null) return@on
            orbIds.add(
                Triple(
                    10,
                    event.entityId,
                    DeployableInstance(
                        type,
                        null,
                        type.duration,
                        type.stats + jalapenoStats,
                        0.0,
                    )
                )
            )
        }

        on<PacketReceivedEvent> { event ->
            val packet = event.packet as? ClientboundLevelParticlesPacket ?: return@on
            if (SETTING_PARTICLES.getCurrent() == "Default") return@on

            if (!packet.alwaysShow()) return@on
            if (!packet.isOverrideLimiter) return@on

            when (packet.particle.type) {
                ParticleTypes.HAPPY_VILLAGER ->
                    if (
                        packet.maxSpeed == 0f &&
                        when (packet.count) {
                            1 ->
                                packet.xDist == 0f &&
                                packet.yDist == 0f &&
                                packet.zDist == 0f &&
                                orbs.any {
                                    val e = it.ent ?: return@any false
                                    abs(e.x - packet.x) < 2.0 &&
                                    abs(e.y + 1.0 - packet.y) < 2.0 &&
                                    abs(e.z - packet.z) < 2.0
                                }

                            4 ->
                                packet.xDist == 0.3f &&
                                packet.yDist == 0.2f &&
                                packet.zDist == 0.3f

                            else -> false
                        }
                    ) event.cancel()

                ParticleTypes.DUST -> {
                    if (packet.count != 0) return@on
                    if (packet.maxSpeed != 1f) return@on

                    val options = packet.particle as? DustParticleOptions ?: return@on
                    if (options.scale != 1f) return@on
                    val r = (options.color.x * 255f).roundToInt()
                    val g = (options.color.y * 255f).roundToInt()
                    val b = (options.color.z * 255f).roundToInt()
                    val c = (r shl 16) or (g shl 8) or b
                    if (Deployable.DUST_COLORS.contains(c)) event.cancel()
                }

                ParticleTypes.FLAME -> if (
                    packet.count == 11 &&
                    packet.maxSpeed == 0.35f &&
                    packet.xDist == 1f &&
                    packet.yDist == 1f &&
                    packet.zDist == 1f
                ) event.cancel()
            }
        }
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        orbs.clear()
        activeOrb = null
        orbIds.clear()
        checked.clear()
    }

    enum class Deployable(
        val sbId: String?,
        val texId: UUID?,
        val duration: Int,
        val range: Int,
        val stats: List<String>,
        val prio: Int,
        val dustColors: Set<Int> = setOf(),
    ) {
        RADIANT(
            "RADIANT_POWER_ORB", null,
            20 * 30, 18,
            listOf("§c+1% ❤/s"),
            6,
        ) {
            override fun addParts(
                t: Int,
                x: Double,
                y: Double,
                z: Double,
                actualY: Double,
                spawn: (id: Int, x: Double, y: Double, z: Double, color: Int, ttl: Int) -> Unit
            ) {
                for (i in 0 until 10) {
                    val a = 2.0 * PI * i / 10.0 + t * 0.2
                    val dx = cos(a)
                    val dz = sin(a)
                    spawn(
                        i,
                        x + dx, (actualY - y) * 1.5 + y, z + dz,
                        0x33CC28,
                        20,
                    )
                }
            }
        },
        MANA_FLUX(
            "MANA_FLUX_POWER_ORB", null,
            20 * 30, 18,
            listOf(
                "§b+50% ✎",
                "§c+2% ❤/s",
                "§c+10 ❁",
            ),
            4,
            setOf(0x6CC7EB),
        ) {
            override fun addParts(
                t: Int,
                x: Double,
                y: Double,
                z: Double,
                actualY: Double,
                spawn: (id: Int, x: Double, y: Double, z: Double, color: Int, ttl: Int) -> Unit
            ) {
                val y = y - 1
                var t = t % 100
                if (t < 69) {
                    t *= 4
                    val k = 0.01
                    for (i in 0 until 5) {
                        val o = 2.0 * PI * i / 5.0
                        spawn(
                            i,
                            2.0 * cos(t + o) / cosh(k * t) + x,
                            2.0 * tanh(k * t) + y,
                            2.0 * sin(t + o) / cosh(k * t) + z,
                            0x007FFF,
                            if (t > 50) 10 else 20,
                        )
                    }
                } else if (t < 80) {
                    t -= 69
                    spawn(
                        5,
                        x,
                        -0.02 * t * t + 2.0 + y,
                        z,
                        0x007FFF,
                        10,
                    )
                } else {
                    val r = 2.0 * (t - 80) / 20.0
                    val n = 30
                    for (i in 0 until n) {
                        val a = 2.0 * PI * i / n + Math.random() / 20.0
                        spawn(
                            i + 6,
                            r * cos(a) + x,
                            y,
                            r * sin(a) + z,
                            0x007FFF,
                            5,
                        )
                    }
                }
            }
        },
        OVERFLUX(
            "OVERFLUX_POWER_ORB", null,
            20 * 60, 18,
            listOf(
                "§b+100% ✎",
                "§c+2.5% ❤/s",
                "§4+5 ♨",
                "§a+5 ☄",
                "§c+25 ❁",
            ),
            2,
            setOf(0xBC2525, 0x6CC7EB, 0x3B0E37),
        ) {
            private val rings = arrayOf(
                Ring(30, 150 * PI / 180.0, 2.0 + Math.random(), 1.2, Color(59, 14, 55).rgb),
                Ring(25, 90 * PI / 180.0, 4.0 + Math.random(), 1.0, Color(124, 26, 46).rgb),
                Ring(20, 10 * PI / 180.0, 3.0 + Math.random(), 0.8, Color(188, 37, 37).rgb),
            )

            override fun addParts(
                t: Int,
                x: Double,
                y: Double,
                z: Double,
                actualY: Double,
                spawn: (id: Int, x: Double, y: Double, z: Double, color: Int, ttl: Int) -> Unit
            ) {
                spawnRings(t, x, y, z, actualY, spawn, rings)
            }
        },
        PLASMAFLUX(
            "PLASMAFLUX_POWER_ORB", null,
            20 * 60, 20,
            listOf(
                "§b+125% ✎",
                "§c+3% ❤/s",
                "§4+7.5 ♨",
                "§a+7.5 ☄",
                "§c+35 ❁",
            ),
            0,
            setOf(0xBC2525, 0x6A3294, 0x3B0E37, 0x0D0606),
        ) {
            private val rings = arrayOf(
                Ring(30, 150 * PI / 180.0, 2.0 + Math.random(), 1.2, Color(13, 6, 6).rgb),
                Ring(25, 90 * PI / 180.0, 4.0 + Math.random(), 1.0, Color(59, 14, 55).rgb),
                Ring(20, 60 * PI / 180.0, 3.0 + Math.random(), 0.8, Color(106, 50, 148).rgb),
                Ring(15, 10 * PI / 180.0, 6.0 + Math.random(), 0.6, Color(209, 145, 252).rgb),
            )

            override fun addParts(
                t: Int,
                x: Double,
                y: Double,
                z: Double,
                actualY: Double,
                spawn: (id: Int, x: Double, y: Double, z: Double, color: Int, ttl: Int) -> Unit
            ) {
                spawnRings(t, x, y, z, actualY, spawn, rings)
            }
        },
        WARNING_FLARE(
            null, UUID.fromString("20878304-7e4f-3dd1-b1f9-72d50bbb9fce"),
            20 * 60 * 3, 40,
            listOf(
                "§4+10 ♨",
                "§f+10 ❂",
            ),
            5,
        ) {
            override fun addParts(
                t: Int,
                x: Double,
                y: Double,
                z: Double,
                actualY: Double,
                spawn: (id: Int, x: Double, y: Double, z: Double, color: Int, ttl: Int) -> Unit
            ) {
                val q = 5.0 / 2.0
                val k = q / (q + 2.0)
                val R = 2.0
                val N = 3
                val t = t * 0.342 + PI
                for (i in 0 until N) {
                    val a = i * 30.0 / N + t
                    spawn(
                        i,
                        R * (k * cos(a) * cos(k * a) + sin(a) * sin(k * a)) + x,
                        R * sqrt(1.0 - k * k) * cos(k * a) + y,
                        R * (k * sin(a) * cos(k * a) - cos(a) * sin(k * a)) + z,
                        0x33CC28,
                        20,
                    )
                }
            }
        },
        ALERT_FLARE(
            null, UUID.fromString("ee51537a-c348-3492-be77-d835d8d98cdd"),
            20 * 60 * 3, 40,
            listOf(
                "§b+50% ✎",
                "§4+20 ♨",
                "§f+20 ❂",
                "§c+10 ⫽",
            ),
            3,
        ) {
            override fun addParts(
                t: Int,
                x: Double,
                y: Double,
                z: Double,
                actualY: Double,
                spawn: (id: Int, x: Double, y: Double, z: Double, color: Int, ttl: Int) -> Unit
            ) {
                val k = 3.0 / 5.0
                val R = 2.0
                val N = 5
                val t = t * 0.2
                for (i in 0 until N) {
                    val a = i * 30.0 / k + t
                    spawn(
                        i,
                        R * cos(k * a) * cos(a) + x,
                        R * sin(k * a) + y,
                        R * cos(k * a) * sin(a) + z,
                        0x007FFF,
                        20,
                    )
                }
            }
        },
        SOS_FLARE(
            null, UUID.fromString("680f7ffe-6925-396e-9d6c-7b3fe6c57e11"),
            20 * 60 * 3, 40,
            listOf(
                "§b+125% ✎",
                "§4+30 ♨",
                "§f+25 ❂",
                "§c+10 ⫽",
                "§e+5 ⚔",
            ),
            1,
        ) {
            override fun addParts(
                t: Int,
                x: Double,
                y: Double,
                z: Double,
                actualY: Double,
                spawn: (id: Int, x: Double, y: Double, z: Double, color: Int, ttl: Int) -> Unit
            ) {
                val a = 2.0
                val b = 2.0
                val p = 2.0
                val q = 3.0
                val u = 60 * PI / 180.0
                val v = 20 * PI / 180.0
                val n = 4
                val t = t * 0.19 + PI
                for (i in 0 until 8) {
                    val o = 2.0 * PI * i / n
                    spawn(
                        i,
                        a * sin(p * t + u + o) + x,
                        b * sin(t) + y,
                        a * sin(q * t + v + o) + z,
                        0x9919CC,
                        20,
                    )
                }
            }
        },
        UMBERELLA(
            "UMBERELLA", null,
            20 * 60 * 5, 30,
            listOf("§6+5 ♔"),
            7,
            setOf(0x6CC7EB),
        ) {
            override fun addParts(
                t: Int,
                x: Double,
                y: Double,
                z: Double,
                actualY: Double,
                spawn: (id: Int, x: Double, y: Double, z: Double, color: Int, ttl: Int) -> Unit
            ) {
                val a = 70 * PI / 180.0
                val k = 8.0 / 3.0
                val R = 1.0
                val N = 3
                val t = t * 0.1
                for (i in 0 until 3) {
                    val o = 2.0 * PI * i / N + t
                    spawn(
                        i,
                        R * (cos(a) * cos(o) * cos(k * o) - sin(o) * sin(k * o)) + x,
                        R * sin(a) * cos(k * o) + actualY,
                        R * (cos(a) * sin(o) * cos(k * o) + cos(o) * sin(k * o)) + z,
                        0xBA5916,
                        20,
                    )
                }
            }
        };

        val isFlare = sbId == null
        val rangeSq = range * range

        abstract fun addParts(
            t: Int,
            x: Double, y: Double, z: Double,
            actualY: Double,
            spawn: (id: Int, x: Double, y: Double, z: Double, color: Int, ttl: Int) -> Unit
        )

        companion object {
            data class Ring(val count: Int, val offset: Double, val period: Double, val radius: Double, val color: Int)

            fun spawnRings(
                t: Int,
                x: Double, y: Double, z: Double,
                actualY: Double,
                spawn: (id: Int, x: Double, y: Double, z: Double, color: Int, ttl: Int) -> Unit,
                rings: Array<Ring>,
            ) {
                val t = t / 50.0
                var id = 0
                rings.forEach { (n, a, k, r, c) ->
                    val nx = cos(a) * cos(t) * cos(k * t) - sin(t) * sin(k * t)
                    val ny = sin(a) * cos(k * t)
                    val nz = cos(a) * sin(t) * cos(k * t) + cos(t) * sin(k * t)
                    var ux = 0.0
                    var uy = 1.0
                    var uz = if (nz == 0.0) 1.0 else -(nx * ux + ny * uy) / nz
                    val l = sqrt(ux * ux + uy * uy + uz * uz)
                    ux /= l
                    uy /= l
                    uz /= l
                    val vx = ny * uz - nz * uy
                    val vy = nz * ux - nx * uz
                    val vz = nx * uy - ny * ux
                    for (i in 0 until n) {
                        val a = 2.0 * PI * i / n
                        spawn(
                            id++,
                            r * (ux * cos(a) + vx * sin(a)) + x,
                            r * (uy * cos(a) + vy * sin(a)) + actualY,
                            r * (uz * cos(a) + vz * sin(a)) + z,
                            c,
                            5,
                        )
                    }
                }
            }

            private val sbIdMap = entries.filter { it.sbId != null }.associateByTo(LinkedHashMap()) { it.sbId!! }
            private val texIdMap = entries.filter { it.texId != null }.associateByTo(LinkedHashMap()) { it.texId!! }

            fun fromSbId(sbId: String) = sbIdMap[sbId]
            fun fromTexId(texId: UUID) = texIdMap[texId]

            val DUST_COLORS = entries.flatMapTo(LinkedHashSet()) { it.dustColors }
        }
    }

    data class DeployableInstance(
        val type: Deployable,
        var ent: ArmorStand?,
        var ttl: Int,
        val stats: List<String>,
        var y: Double,
    ) : Comparable<DeployableInstance> {
        val spawner = ParticleSpawner()

        fun spawnParticles() {
            val e = ent ?: return
            type.addParts(
                type.duration - ttl,
                e.x, y + 1.7, e.z,
                e.y + 1.8,
                spawner::spawn
            )
        }

        override fun compareTo(other: DeployableInstance): Int {
            return comparator.compare(this, other)
        }

        companion object {
            val comparator = Comparator.comparingInt<DeployableInstance> { it.type.prio }.thenBy { -it.ttl }
        }
    }

    class ParticleSpawner {
        private var old = mutableListOf<Position?>()
        private var swap = mutableListOf<Position?>()

        fun swapPositions() {
            old = swap
            swap = mutableListOf()
        }

        fun spawn(
            id: Int,
            x: Double, y: Double, z: Double,
            color: Int, ttl: Int
        ) {
            var dx = 0.0
            var dy = 0.0
            var dz = 0.0
            var px = x
            var py = y
            var pz = z
            while (swap.size <= id) swap.add(null)
            old.getOrNull(id)?.let {
                dx = px - it.x
                dy = py - it.y
                dz = pz - it.z
                px = it.x
                py = it.y
                pz = it.z
            }
            swap[id] = Position(x, y, z)

            val part = minecraft.particleEngine.createParticle(
                DustParticleOptions(color, 1f),
                px, py, pz,
                dx, dy, dz,
            ) ?: return
            part as ParticleAccessor
            part.lifetime = ttl
            part.friction = 1f
            part.speedUpWhenYMotionIsBlocked = false
            part.hasPhysics = false
        }

        data class Position(val x: Double, val y: Double, val z: Double)
    }
}