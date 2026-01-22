package com.github.synnerz.devonian.features.dungeons.solvers

import com.github.synnerz.devonian.Devonian
import com.github.synnerz.devonian.api.Ping
import com.github.synnerz.devonian.api.Scheduler
import com.github.synnerz.devonian.api.dungeon.ComponentPosition
import com.github.synnerz.devonian.api.dungeon.DungeonScanner
import com.github.synnerz.devonian.api.dungeon.Stages
import com.github.synnerz.devonian.api.dungeon.WorldPosition
import com.github.synnerz.devonian.api.dungeon.mapEnums.RoomTypes
import com.github.synnerz.devonian.api.events.*
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.mixin.accessor.ClientboundMoveEntityPacketAccessor
import com.github.synnerz.devonian.utils.BasicState
import com.github.synnerz.devonian.utils.math.MathUtils
import com.github.synnerz.devonian.utils.render.Render3DImmediate
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.item.Items
import java.awt.Color
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.roundToInt

object CampHelper : Feature(
    "campHelper",
    "Predicts where blood mobs will spawn.",
    Categories.DUNGEONS,
    "catacombs",
    subcategory = "Solvers",
    searchTags = setOf("blood"),
) {
    override fun createRequirements(): List<BasicState<Boolean>?> {
        return super.createRequirements() + listOf(Stages.WatcherClear.isActiveState)
    }

    private val SETTING_SHOW_TIMER = addSwitch(
        "showTimer",
        true,
        "renders timer under each box",
        "Camp Show Timer",
    )
    private val SETTING_USE_PING = addSwitch(
        "usePing",
        true,
        "attempts to correct timer for ping",
        "Camp Adjust For Ping",
    )
    private val SETTING_WIRE_COLOR = addColorPicker(
        "wireColor",
        Color(0, 255, 0).rgb,
        "",
        "Camp Wire Color",
    )
    private val SETTING_FILL_COLOR = addColorPicker(
        "fillColor",
        Color(0, 255, 255).rgb,
        "",
        "Camp Fill Color",
    )
    private val SETTING_LINE_WIDTH = addSlider(
        "lineWidth",
        3.0,
        0.0, 10.0,
        "",
        "Camp Line Width",
    )

    private var bloodComp: ComponentPosition? = null
    private val bloodStands = ConcurrentHashMap<Int, UndeadGuesser>()

    private fun addStand(id: Int) {
        bloodStands.putIfAbsent(id, UndeadGuesser(id))
    }

    override fun initialize() {
        on<TickEvent> {
            if (bloodComp != null) {
                val w = minecraft.level ?: return@on
                bloodStands.forEach { (id, v) ->
                    if (v.ent?.isAlive != false) return@forEach
                    v.ent = w.getEntity(id)
                }
                return@on
            }
            bloodComp = DungeonScanner.rooms.find { it?.type == RoomTypes.BLOOD }?.comps?.getOrNull(0)?.toComponent()
            if (bloodComp != null) {
                val w = minecraft.level ?: return@on
                w.entitiesForRendering().forEach {
                    if (it !is ArmorStand) return@forEach

                    val pos = WorldPosition(it.x.toInt(), it.z.toInt()).toComponent().toRoom()
                    if (pos != bloodComp) return@forEach

                    val head = it.getItemBySlot(EquipmentSlot.HEAD) ?: return@forEach
                    if (head.isEmpty) return@forEach
                    if (head.item != Items.PLAYER_HEAD) return@forEach

                    addStand(it.id)
                }
            }
        }

        on<EntityEquipmentEvent> { event ->
            if (bloodComp == null) return@on
            if (event.type != EntityType.ARMOR_STAND) return@on

            if (event.slots.size != 1) return@on
            val entry = event.slots.getOrNull(0) ?: return@on
            if (entry.first != EquipmentSlot.HEAD) return@on

            if (entry.second?.item != Items.PLAYER_HEAD) return@on

            val pos = WorldPosition(event.spawnPos.x.toInt(), event.spawnPos.z.toInt()).toComponent().toRoom()
            if (pos == bloodComp) addStand(event.entityId)
        }

        on<PacketReceivedEvent> { event ->
            val packet = event.packet as? ClientboundMoveEntityPacket ?: return@on
            val id = (event.packet as? ClientboundMoveEntityPacketAccessor)?.entityId ?: return@on

            bloodStands[id]?.update(
                packet.xa,
                packet.ya,
                packet.za,
                if (Stages.FirstWatcherSpawn.hasFinished()) 40 else 80,
            )
        }

        on<RenderWorldEvent> {
            val t = System.currentTimeMillis()
            val st = EventBus.serverTicks()
            bloodStands.forEach { (_, v) ->
                if (!v.shouldShow()) return@forEach

                val f = if (v.guessTimeOld == 0L) 1.0 else (t - v.guessTime).toDouble() / (v.guessTime - v.guessTimeOld)
                val ttl = v.ttl(st)

                if (ttl <= -3) return@forEach

                val x = MathUtils.lerp(f, v.guessXOld, v.guessX)
                val y = MathUtils.lerp(f, v.guessYOld, v.guessY) + 1.0
                val z = MathUtils.lerp(f, v.guessZOld, v.guessZ)

                val m = 1.0 - (max(ttl, 0) - (if (SETTING_USE_PING.get()) Ping.getMedianPing() / 50.0 else 0.0)) / v.maxTTL
                val w = 1.0
                val h = 2.0

                Render3DImmediate.renderWireframeBox(
                    x, y, z,
                    w, h,
                    SETTING_WIRE_COLOR.getColor(),
                    lineWidth = SETTING_LINE_WIDTH.get(),
                    centered = true,
                )
                Render3DImmediate.renderFilledBox(
                    x,
                    y + (h - h * m) * 0.5,
                    z,
                    w * m, h * m,
                    SETTING_FILL_COLOR.getColor(),
                    centered = true,
                )

                if (SETTING_SHOW_TIMER.get()) Render3DImmediate.renderString(
                    "%.2f".format(ttl * 0.05),
                    x, y - 1.0, z,
                )
            }
        }
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        bloodComp = null
        bloodStands.clear()
    }
}

class UndeadGuesser(val id: Int) {
    var maxTTL = -1

    var ent: Entity? = null
    val knownT = mutableListOf<Double>()
    val knownX = mutableListOf<Double>()
    val knownY = mutableListOf<Double>()
    val knownZ = mutableListOf<Double>()

    var dead = false

    var guessX = 0.0
    var guessY = 0.0
    var guessZ = 0.0
    var guessXOld = 0.0
    var guessYOld = 0.0
    var guessZOld = 0.0
    var guessTime = 0L
    var guessTimeOld = 0L

    var startTick = -1
    var calcStart = false

    var hasSpawn = false

    fun shouldShow() = hasSpawn && (ent?.isAlive == true) && startTick != -1 && !dead && guessTime > 0L

    fun ttl(st: Int) = if (startTick == -1 || maxTTL == -1) 0 else startTick + maxTTL - st

    fun update(dxRaw: Short, dyRaw: Short, dzRaw: Short, maxTtl: Int) {
        if (dead) return
        val tick = EventBus.serverTicks()

        val dx = dxRaw / 4096.0
        val dy = dyRaw / 4096.0
        val dz = dzRaw / 4096.0

        Scheduler.scheduleBeforePacket {
            if (ent == null) ent = Devonian.minecraft.level?.getEntity(id) ?: return@scheduleBeforePacket

            val pos = ent!!.positionCodec.base
            val x = pos.x
            val y = pos.y
            val z = pos.z

            if (!hasSpawn) {
                hasSpawn = true

                if (
                    fract(x) != 0.5 ||
                    fract(z) != 0.5 ||
                    !spawnY.contains(y)
                ) calcStart = true
            }

            if (dxRaw == 0.s && dyRaw == 0.s && dzRaw == 0.s) return@scheduleBeforePacket

            if (maxTTL == -1) maxTTL = maxTtl

            if (calcStart) {
                calcStart = false
                for (sy in spawnY) {
                    val t = (y - sy) / dy
                    if (t < 0.0) continue
                    if (!eq(fract(x - t * dx), 0.5)) continue
                    if (!eq(fract(z - t * dz), 0.5)) continue
                    startTick = tick - (t * 3).roundToInt()
                    break
                }
                if (startTick == -1) {
                    dead = true
                    return@scheduleBeforePacket
                }
            }

            if (startTick == -1) startTick = tick - 3

            val t = ttl(tick)
            if (t < -3) {
                dead = true
                return@scheduleBeforePacket
            }
            knownT.add(tick.toDouble())
            knownX.add(x + dx)
            knownY.add(y + dy)
            knownZ.add(z + dz)

            if (knownT.size < 2) return@scheduleBeforePacket

            // val fit = MathUtils.fitLine3D(knownX, knownY, knownZ)
            val linX = MathUtils.linReg(knownT, knownX) ?: return@scheduleBeforePacket
            val linY = MathUtils.linReg(knownT, knownY) ?: return@scheduleBeforePacket
            val linZ = MathUtils.linReg(knownT, knownZ) ?: return@scheduleBeforePacket

            val x0 = linX.b * t + x + dx
            val y0 = linY.b * t + y + dy
            val z0 = linZ.b * t + z + dz
            // val x1 = fit[0][0]
            // val y1 = fit[1][0]
            // val z1 = fit[2][0]
            // val vx = fit[0][1]
            // val vy = fit[1][1]
            // val vz = fit[2][1]

            // val u = -((x1 - x0) * vx + (y1 - y0) * vy + (z1 - z0) * vz) / (vx * vx + vy * vy + vz * vz)

            // val gx = x1 + vx * u
            // val gy = y1 + vy * u
            // val gz = z1 + vz * u

            guessXOld = guessX
            guessYOld = guessY
            guessZOld = guessZ
            guessTimeOld = guessTime
            guessX = x0
            guessY = y0
            guessZ = z0
            guessTime = System.currentTimeMillis()
        }
    }

    companion object {
        private val spawnY = listOf(71.75, 75.75, 79.75)

        private fun eq(a: Double, b: Double) = abs(a - b) < 1e-6
        private fun fract(v: Double) = v - floor(v)
    }
}

private val Int.s
    get() = this.toShort()