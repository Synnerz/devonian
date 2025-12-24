package com.github.synnerz.devonian.features.dungeons.solvers

import com.github.synnerz.barrl.Context
import com.github.synnerz.barrl.utils.RendererLayers
import com.github.synnerz.devonian.Devonian
import com.github.synnerz.devonian.api.Scheduler
import com.github.synnerz.devonian.api.WorldUtils
import com.github.synnerz.devonian.api.dungeon.DungeonEvent
import com.github.synnerz.devonian.api.dungeon.DungeonRoom
import com.github.synnerz.devonian.api.dungeon.Stages
import com.github.synnerz.devonian.api.events.PacketReceivedEvent
import com.github.synnerz.devonian.api.events.RenderWorldEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.utils.BasicState
import net.minecraft.core.BlockPos
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import org.joml.Vector3f
import java.awt.Color
import java.util.*
import kotlin.math.abs
import kotlin.math.floor

object IceFillSolver : Feature(
    "iceFillSolver",
    "",
    Categories.DUNGEONS,
    "catacombs",
    subcategory = "Solvers",
) {
    override fun createRequirements(): List<BasicState<Boolean>?> {
        return super.createRequirements() + listOf(Stages.Clear.isActiveState)
    }

    private val SETTING_LINE_COLOR = addColorPicker(
        "lineColor",
        Color.GREEN.rgb,
        "",
        "Line Color",
    )
    private val SETTING_BOX1_COLOR = addColorPicker(
        "startColor",
        Color.BLUE.rgb,
        "",
        "Start Color",
    )
    private val SETTING_BOX2_COLOR = addColorPicker(
        "endColor",
        Color.RED.rgb,
        "",
        "End Color",
    )
    private val SETTING_LINE_WIDTH = addSlider(
        "lineWidth",
        3.0,
        1.0, 10.0,
        "",
        "Line Width",
    )
    private val SETTING_ALLOW_WALL_START = addSwitch(
        "fastSolution",
        false,
        "allows the solution to start along the wall",
        "Fast Solution",
    )

    private val platforms = listOf(
        IcePlatform(
            Coord(15, 69, 10),
            Coord(15, 69, 7),
            Coord(14, 69, 7),
            Coord(16, 69, 9),
        ),
        IcePlatform(
            Coord(15, 70, 17),
            Coord(15, 70, 12),
            Coord(13, 70, 12),
            Coord(17, 70, 16),
        ),
        IcePlatform(
            Coord(15, 71, 26),
            Coord(15, 71, 19),
            Coord(12, 71, 19),
            Coord(18, 71, 25),
        ),
    )

    private var inIce = false
    private var iceRoom: DungeonRoom? = null

    private fun onBlock(pos: BlockPos, state: BlockState): Pair<Boolean, IcePlatform>? {
        platforms.forEach {
            if (!it.contains(iceRoom!!, pos.x, pos.y, pos.z)) return@forEach
            if (state.block == Blocks.PACKED_ICE) {
                if (it.removeBlock(iceRoom!!, pos.x, pos.z)) return Pair(true, it)
            } else if (state.isAir) {
                it.reset(iceRoom!!)
                return Pair(false, it)
            }
        }
        return null
    }

    override fun initialize() {
        on<DungeonEvent.RoomEnter> { event ->
            val room = event.room
            if (room.name != "Ice Fill") return@on
            iceRoom = room
            if (!room.hasRotation()) return@on

            inIce = true
            platforms.forEach { it.rescan(room, !SETTING_ALLOW_WALL_START.get()) }
        }

        on<DungeonEvent.RoomLeave> {
            inIce = false
        }

        on<PacketReceivedEvent> { event ->
            if (!inIce) return@on
            when (val packet = event.packet) {
                is ClientboundBlockUpdatePacket -> {
                    Scheduler.scheduleTask {
                        onBlock(packet.pos, packet.blockState)?.let {
                            it.second.solve(iceRoom!!, !SETTING_ALLOW_WALL_START.get(), it.first)
                        }
                    }
                }

                is ClientboundSectionBlocksUpdatePacket -> {
                    Scheduler.scheduleTask {
                        val update = linkedMapOf<IcePlatform, Boolean>()
                        packet.runUpdates { pos, state ->
                            onBlock(pos, state)?.let {
                                update.merge(it.second, it.first, Boolean::and)
                            }
                        }
                        update.forEach {
                            it.key.solve(iceRoom!!, !SETTING_ALLOW_WALL_START.get(), it.value)
                        }
                    }
                }
            }
        }

        on<RenderWorldEvent> { event ->
            if (!inIce) return@on

            platforms.forEach {
                val sol = it.solution ?: return@forEach
                if (sol.isEmpty()) return@forEach

                val start = sol.first
                val end = sol.last

                Context.Immediate?.renderBox(
                    start.x.toDouble(),
                    start.y.toDouble(),
                    start.z.toDouble(),
                    SETTING_BOX1_COLOR.getColor(),
                    lineWidth = SETTING_LINE_WIDTH.get(),
                )
                if (start !== end) Context.Immediate?.renderBox(
                    end.x.toDouble(),
                    end.y.toDouble(),
                    end.z.toDouble(),
                    SETTING_BOX2_COLOR.getColor(),
                    lineWidth = SETTING_LINE_WIDTH.get(),
                )
            }

            val consumer = minecraft.renderBuffers().bufferSource().getBuffer(
                RendererLayers.lines(
                    SETTING_LINE_WIDTH.get(),
                    true,
                    SETTING_LINE_COLOR.getColor().alpha == 255
                )
            )
            val camPos = event.ctx.worldState().cameraRenderState.pos ?: return@on
            event.ctx.matrices().pushPose()
            event.ctx.matrices().translate(camPos.reverse())
            val mat = event.ctx.matrices().last()

            platforms.forEach {
                val sol = it.solution ?: return@forEach

                var p: Coord? = null
                sol.forEach { curr ->
                    val prev = p
                    p = curr
                    if (prev == null) return@forEach

                    val x1 = prev.x + 0.5f
                    val y1 = prev.y + 1.1f
                    val z1 = prev.z + 0.5f
                    val x2 = curr.x + 0.5f
                    val y2 = curr.y + 1.1f
                    val z2 = curr.z + 0.5f

                    val normalized = Vector3f(x2 - x1, y2 - y1, z2 - z1).normalize()

                    consumer
                        .addVertex(mat, x1, y1, z1)
                        .setColor(SETTING_LINE_COLOR.get())
                        .setNormal(mat, normalized)

                    consumer
                        .addVertex(mat, x2, y2, z2)
                        .setColor(SETTING_LINE_COLOR.get())
                        .setNormal(mat, normalized)
                }
            }

            event.ctx.matrices().popPose()
        }
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        inIce = false
        iceRoom = null
    }
}

data class Coord(val x: Int, val y: Int, val z: Int)

class IcePlatform(
    val end: Coord,
    val start: Coord,
    val corner1: Coord,
    val corner2: Coord,
) {
    private val maxId = (corner2.x - corner1.x + 1) * (corner2.z - corner1.z + 1)
    private val blocks = arrayOfNulls<CompWorld>(maxId + 1)
    private var mutableBlocks = blocks.copyOf()
    var solution: Deque<Coord>? = null

    init {
        if (maxId > 63) throw IllegalArgumentException("long bitset :(")
    }

    private fun idAt(x: Int, z: Int) =
        if (z == end.z) maxId
        else (x - corner1.x) + (corner2.x - corner1.x + 1) * (z - corner1.z)

    private fun parityOf(x: Int, z: Int) = (x + z) and 1

    private val directions = listOf(
        Pair(-1, 0),
        Pair(+1, 0),
        Pair(0, -1),
        Pair(0, +1),
    )

    fun rescan(room: DungeonRoom, endOnStart: Boolean) {
        blocks.fill(null)
        blocks[maxId] = CompWorld(
            end,
            room.fromComp(end.x, end.z)!!,
            parityOf(end.x, end.z),
            arrayOf(idAt(end.x, end.z - 1)),
        )

        val y = end.y
        for (cx in corner1.x .. corner2.x) {
            for (cz in corner1.z .. corner2.z) {
                val (x, z) = room.fromComp(cx, cz)!!
                val bs = WorldUtils.getBlockState(x, y + 1, z)!!
                if (bs.isAir) blocks[idAt(cx, cz)] = CompWorld(
                    Coord(cx, y, cz),
                    Pair(x, z),
                    parityOf(cx, cz),
                    directions.filter {
                        contains(cx + it.first, cz + it.second)
                    }.map {
                        idAt(cx + it.first, cz + it.second)
                    }.toTypedArray()
                )
            }
        }

        reset(room)
        solve(room, endOnStart, false)
    }

    fun reset(room: DungeonRoom) {
        mutableBlocks = blocks.copyOf()

        for (cx in corner1.x .. corner2.x) {
            for (cz in corner1.z .. corner2.z) {
                val (x, z) = room.fromComp(cx, cz) ?: return
                val bs = WorldUtils.getBlockState(x, end.y, z)!!
                if (bs.block == Blocks.PACKED_ICE) mutableBlocks[idAt(cx, cz)] = null
            }
        }

        solution = null
    }

    private fun contains(x: Int, z: Int): Boolean {
        return x == end.x && z == end.z ||
        (x in corner1.x .. corner2.x && z in corner1.z .. corner2.z)
    }

    fun contains(room: DungeonRoom, x: Int, y: Int, z: Int): Boolean {
        val pos = room.fromPos(x, z) ?: return false
        return end.y == y && contains(pos.first, pos.second)
    }

    fun removeBlock(room: DungeonRoom, wx: Int, wz: Int): Boolean {
        val (x, z) = room.fromPos(wx, wz) ?: return false
        val id = idAt(x, z)
        mutableBlocks[id] = null

        val sol = solution ?: return true

        val head = sol.peekFirst()
        if (head != null && head.x == wx && head.z == wz) {
            sol.removeFirst()
            return false
        }

        val tail = sol.peekLast()
        if (tail != null && tail.x == wx && tail.z == wz) {
            sol.removeLast()
            return false
        }

        solution = null
        return true
    }

    fun solve(room: DungeonRoom, endOnStart: Boolean, checkPlayer: Boolean) {
        var total = 0
        var odd = 0
        mutableBlocks.forEach {
            if (it == null) return@forEach
            total++
            odd += it.parity
        }
        var even = total - odd
        if (total == 0) {
            solution = LinkedList()
            return
        }
        if (abs(odd - even) >= 2) return

        val first = Devonian.minecraft.player?.let {
            if (!checkPlayer) return@let null
            // ether sets height to +.05
            if (it.y - (end.y + 1) !in 0.0 .. 0.1) return@let null
            val wx = floor(it.x).toInt()
            val wz = floor(it.z).toInt()
            val (x, z) = room.fromPos(wx, wz)!!
            if (!contains(x, z)) return@let null
            val id = idAt(x, z)
            val b = blocks[id] ?: return@let null
            if (mutableBlocks[id] == null) {
                total++
                odd += b.parity
                even += b.parity xor 1
                if (abs(odd - even) >= 2) return
            }
            b
        } ?: listOf(
            if (endOnStart) mutableBlocks[idAt(start.x, start.z)] else null,
            mutableBlocks[maxId],
            if (endOnStart) null else mutableBlocks[idAt(start.x, start.z)],
            *mutableBlocks,
        ).first {
            if (it == null) return@first false
            if (even == odd) return@first true
            return@first (odd > even) == (it.parity == 1)
        } ?: return

        var minCost = 5318008
        var best: LinkedList<Coord>? = null

        val visitedHist = LongArray(maxId + 1)
        val route = arrayOfNulls<Destination>(total)
        val queue = ArrayDeque<Destination>()

        queue.add(Destination(first, -1, 0, 0, 1L shl idAt(first.comp.x, first.comp.z)))

        while (queue.isNotEmpty()) {
            val d = queue.removeLast()

            var pathLen = d.size
            if (d.cost + total - pathLen > minCost) continue

            route[pathLen++] = d
            val visited = visitedHist[d.size] or d.mask
            visitedHist[pathLen] = visited

            if (pathLen == total) {
                if (d.cost < minCost) {
                    minCost = d.cost
                    best = route.mapTo(LinkedList()) {
                        Coord(it!!.data.world.first, it.data.comp.y, it.data.world.second)
                    }
                }
                continue
            }

            d.data.neighbors.forEach {
                val data = mutableBlocks[it] ?: return@forEach

                val mask = 1L shl it
                if (visited and mask != 0L) return@forEach

                val dx = data.comp.x - d.data.comp.x
                val dz = data.comp.z - d.data.comp.z
                val dir = dx + dz * 100

                queue.add(
                    Destination(
                        data,
                        dir,
                        pathLen,
                        d.cost + (if (dir == d.dir) 0 else 1),
                        mask,
                    )
                )
            }
        }

        solution = best
    }

    class Destination(val data: CompWorld, val dir: Int, val size: Int, val cost: Int, val mask: Long)

    class CompWorld(val comp: Coord, val world: Pair<Int, Int>, val parity: Int, val neighbors: Array<Int>)
}