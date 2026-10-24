package com.github.synnerz.devonian.features.dungeons.solvers

import com.github.synnerz.devonian.Devonian
import com.github.synnerz.devonian.api.Scheduler
import com.github.synnerz.devonian.api.WorldUtils
import com.github.synnerz.devonian.api.dungeon.DungeonEvent
import com.github.synnerz.devonian.api.dungeon.DungeonRoom
import com.github.synnerz.devonian.api.dungeon.DungeonStages
import com.github.synnerz.devonian.api.events.PacketReceivedEvent
import com.github.synnerz.devonian.api.events.RenderWorldEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.utils.BasicState
import com.github.synnerz.devonian.utils.render.Render3DImmediate
import net.minecraft.core.BlockPos
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import java.awt.Color
import java.util.*
import java.util.concurrent.Future
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

object IceFillSolver : Feature(
    "iceFillSolver",
    "",
    Categories.DUNGEONS,
    "catacombs",
    subcategory = "Solvers",
    searchTags = setOf("puzzle"),
) {
    override fun createRequirements(): List<BasicState<Boolean>?> {
        return super.createRequirements() + listOf(DungeonStages.Clear.isActiveState)
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
        "allows the solution to start anywhere",
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
                    Scheduler.scheduleAfterPacket {
                        onBlock(packet.pos, packet.blockState)?.let {
                            if (!it.first) it.second.reset(iceRoom!!)
                            it.second.solve(iceRoom!!, !SETTING_ALLOW_WALL_START.get(), it.first)
                        }
                    }
                }

                is ClientboundSectionBlocksUpdatePacket -> {
                    Scheduler.scheduleAfterPacket {
                        val update = linkedMapOf<IcePlatform, Boolean>()
                        packet.runUpdates { pos, state ->
                            onBlock(pos, state)?.let {
                                update.merge(it.second, it.first, Boolean::and)
                            }
                        }
                        update.forEach {
                            if (!it.value) it.key.reset(iceRoom!!)
                            it.key.solve(iceRoom!!, !SETTING_ALLOW_WALL_START.get(), it.value)
                        }
                    }
                }
            }
        }

        on<RenderWorldEvent> {
            if (!inIce) return@on

            platforms.forEach {
                val sol = it.solution ?: return@forEach
                if (sol.isEmpty()) return@forEach

                val start = sol.first
                val end = sol.last

                Render3DImmediate.renderWireframeBox(
                    start.x.toDouble(),
                    start.y.toDouble(),
                    start.z.toDouble(),
                    1.0, 1.0,
                    SETTING_BOX1_COLOR.getColor(),
                    lineWidth = SETTING_LINE_WIDTH.get(),
                )
                if (start !== end) Render3DImmediate.renderWireframeBox(
                    end.x.toDouble(),
                    end.y.toDouble(),
                    end.z.toDouble(),
                    1.0, 1.0,
                    SETTING_BOX2_COLOR.getColor(),
                    lineWidth = SETTING_LINE_WIDTH.get(),
                )
            }

            val color = SETTING_LINE_COLOR.getColor()
            Render3DImmediate.renderLineStrip(color.alpha == 255) {
                platforms.forEach {
                    val sol = it.solution ?: return@forEach

                    sol.forEach { pos ->
                        submit(
                            pos.x + 0.5,
                            pos.y + 1.1,
                            pos.z + 0.5,
                            color,
                            SETTING_LINE_WIDTH.get(),
                        )
                    }
                    endBatch()
                }
            }

            /*
            Render3DImmediate.renderLines(true) {
                platforms.forEach {
                    val sol = it.solution ?: return@forEach

                    var prevC: IcePlatform.Direction? = null
                    var prev: SolutionCoord? = null
                    sol.forEach { pos ->
                        val l = prev
                        prev = pos

                        if (pos.c == prevC) return@forEach
                        prevC = pos.c

                        if (l == null) return@forEach

                        submit(
                            l.x + 0.5,
                            l.y + 1.11,
                            l.z + 0.5,
                            l.x + pos.c.dx * 0.5 + 0.5,
                            l.y + 1.11,
                            l.z + pos.c.dz * 0.5 + 0.5,
                            Color.RED,
                            Color.RED,
                            SETTING_LINE_WIDTH.get(),
                        )
                    }
                }
            }
            */
        }
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        inIce = false
        iceRoom = null
    }
}

data class Coord(val x: Int, val y: Int, val z: Int)
data class SolutionCoord(val x: Int, val y: Int, val z: Int, val c: IcePlatform.Direction)

class IcePlatform(
    val end: Coord,
    val start: Coord,
    val corner1: Coord,
    val corner2: Coord,
) {
    private val scanSize = corner2.x - corner1.x + 1
    private val scanCount = corner2.z - corner1.z + 1 + 1
    private val gridSize = scanSize * scanCount

    private val leftColMask = run {
        var m = -1L
        for (i in 0 until 64 step scanSize) {
            m = m and (1L shl i).inv()
        }
        return@run m
    }
    private val rightColMask = run {
        var m = -1L
        for (i in scanSize - 1 until 64 step scanSize) {
            m = m and (1L shl i).inv()
        }
        return@run m
    }

    private val endId = idAt(end.x, end.z)
    private val blocks = arrayOfNulls<CompWorld>(gridSize)
    private var mutableBlocks = blocks.copyOf()
    private var solutionFuture: Future<*>? = null
    var solution: Deque<SolutionCoord>? = null

    init {
        if (gridSize > 63) throw IllegalArgumentException("long bitset :(")
    }

    private fun idAt(cx: Int, cz: Int) =
        (cz - corner1.z) * scanSize +
        cx - corner1.x

    private fun parityOf(x: Int, z: Int) = (x + z) and 1

    private fun contains(x: Int, z: Int): Boolean {
        return x == end.x && z == end.z ||
        (x in corner1.x .. corner2.x && z in corner1.z .. corner2.z)
    }

    private fun isWall(x: Int, z: Int): Boolean {
        if (contains(x, z)) return blocks[idAt(x, z)] == null
        if (contains(x + 1, z)) return true
        if (contains(x - 1, z)) return true
        if (contains(x, z + 1)) return x != start.x
        if (contains(x, z - 1)) return true
        return false
    }

    fun rescan(room: DungeonRoom, endOnStart: Boolean) {
        blocks.fill(null)
        val hasBlock = BooleanArray(blocks.size)

        blocks[endId] = CompWorld(
            endId,
            1L shl endId,
            end,
            room.fromComp(end.x, end.z)!!,
            parityOf(end.x, end.z),
            IntArray(5) {
                Direction.entries[it].let {
                    if (it == Direction.NegZ) idAt(end.x + it.dx, end.z + it.dz)
                    else -1
                }
            },
            BooleanArray(5),
        )
        hasBlock[endId] = true

        val y = end.y
        for (cx in corner1.x .. corner2.x) {
            for (cz in corner1.z .. corner2.z) {
                val (x, z) = room.fromComp(cx, cz)!!
                val bs = WorldUtils.getBlockState(x, y + 1, z)!!
                if (bs.isAir) hasBlock[idAt(cx, cz)] = true
            }
        }
        for (cx in corner1.x .. corner2.x) {
            for (cz in corner1.z .. corner2.z) {
                val (x, z) = room.fromComp(cx, cz)!!
                val id = idAt(cx, cz)
                if (hasBlock[id]) blocks[id] = CompWorld(
                    id,
                    1L shl id,
                    Coord(cx, y, cz),
                    Pair(x, z),
                    parityOf(cx, cz),
                    IntArray(5) {
                        if (it == 4) return@IntArray -1
                        val dir = Direction.entries[it]
                        val x = cx + dir.dx
                        val z = cz + dir.dz
                        val id = idAt(x, z)
                        if (contains(x, z) && hasBlock[id]) id
                        else -1
                    },
                    BooleanArray(5) {
                        if (it == 4) return@BooleanArray true
                        Direction.entries[it].let {
                            isWall(cx + it.dx, cz + it.dz)
                        }
                    },
                )
            }
        }

        blocks.forEach { block ->
            if (block == null) return@forEach
            for (i in 0 until 4) {
                block.walls[i] = Direction.entries[i].let {
                    isWall(block.comp.x + it.dx, block.comp.z + it.dz)
                }
            }
            block.walls[4] = true
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
        val thisBlocks = mutableBlocks.copyOf()

        var total = 0
        var odd = 0
        thisBlocks.forEach {
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

        val src = Devonian.minecraft.player?.let {
            if (!checkPlayer) return@let null
            // ether sets height to +.05
            if (it.y - (end.y + 1) !in 0.0 .. 0.1) return@let null
            val wx = floor(it.x).toInt()
            val wz = floor(it.z).toInt()
            val (x, z) = room.fromPos(wx, wz)!!
            if (!contains(x, z)) return@let null
            val id = idAt(x, z)
            val b = blocks[id] ?: return@let null
            if (thisBlocks[id] == null) {
                total++
                odd += b.parity
                even += b.parity xor 1
                if (abs(odd - even) >= 2) return
                thisBlocks[id] = b
            }
            b
        } ?: listOf(
            if (endOnStart) thisBlocks[idAt(start.x, start.z)] else null,
        ).firstOrNull {
            if (it == null) return@firstOrNull false
            if (even == odd) return@firstOrNull true
            return@firstOrNull (odd > even) == (it.parity == 1)
        }

        solutionFuture?.cancel(true)
        solutionFuture = Scheduler.schedulePool.submit {
            val best = if (src == null) thisBlocks.fold(null) { acc, block ->
                if (Thread.interrupted()) return@submit
                if (block == null) return@fold acc
                if (even != odd && (odd < even) == (block.parity == 1)) return@fold acc
                val sol = findPath(thisBlocks, total, block) ?: return@fold acc
                if (acc == null || sol.cost.last() < acc.cost.last()) sol
                else acc
            }
            else findPath(thisBlocks, total, src)

            if (best == null) return@submit
            if (Thread.interrupted()) return@submit

            solution = best.path.mapTo(LinkedList()) {
                val (x, z) = blocks[it.id]!!.world
                SolutionCoord(x, end.y, z, Direction.entries[it.oc])
            }
        }
    }

    // bitboard flood
    private fun isConnected(unvisited: Long): Boolean {
        var flood = unvisited.takeLowestOneBit()
        var prev: Long
        do {
            prev = flood
            flood = flood or
                ((flood shl 1) and leftColMask) or
                ((flood shr 1) and rightColMask) or
                (flood shl scanSize) or
                (flood shr scanSize)
            flood = flood and unvisited
        } while (flood != prev)
        return flood == unvisited
    }

    private fun degreeOf(mask: Long, unvisited: Long): Int {
        return (
            unvisited and
            (
                ((mask shl 1) and leftColMask) or
                ((mask shr 1) and rightColMask) or
                (mask shl scanSize) or
                (mask shr scanSize)
            )
        ).countOneBits()
    }

    private fun findPath(thisBlocks: Array<CompWorld?>, len: Int, src: CompWorld): Solution? {
        val unvisited = run {
            var m = 0L
            thisBlocks.forEachIndexed { i, v ->
                if (v != null) m = m or (1L shl i)
            }
            return@run m
        }

        val fastSol = findPathFast(thisBlocks, len, src) ?: return null
        val minCost = fastSol.cost
        var best: Solution? = null

        val visitedHist = LongArray(len)
        val route = arrayOfNulls<State>(len)
        val queue = ArrayDeque<State>()

        queue.add(State(src.id, 4, 4, if (src.comp == start) State.BEGIN_ON_START_INCENTIVE else 0))

        var ops = 0
        // val startTime = System.currentTimeMillis()
        while (queue.isNotEmpty()) {
            if (++ops == 100_000) break

            val node = queue.removeLast()

            var pathLen = node.length
            if (node.totalCost - node.heur(len) >= minCost[pathLen]) continue
            val curr = thisBlocks[node.id] ?: continue

            minCost[pathLen] = min(minCost[pathLen], node.totalCost)
            route[pathLen] = node
            val visited = visitedHist[pathLen] or curr.mask

            if (++pathLen == len) {
                if (node.totalCost == minCost[pathLen - 1]) {
                    best = Solution(minCost, route.copyOf(pathLen).requireNoNulls())
                }
                continue
            }

            visitedHist[pathLen] = visited
            val wall = curr.walls[node.od]

            Direction.nextD[node.oc].forEach { d ->
                val id = curr.neighbors[d]
                if (id < 0) return@forEach

                val mask = 1L shl id
                if (visited and mask != 0L) return@forEach

                val v = unvisited and (visited or mask).inv()
                if (!isConnected(v)) return@forEach

                Direction.nextC[node.oc].forEach { c ->
                    val cost = node.cost(d, c, wall)

                    queue.add(
                        State(
                            id,
                            if (d != node.od && d == c && node.od != 4) 4 else d,
                            c,
                            node.totalCost + cost,
                            pathLen,
                            node,
                        )
                    )
                }

                if (node.od == 4) return@forEach
                if (d == node.od) return@forEach
                val cost = node.cost(d, d, wall)

                queue.add(
                    State(
                        id,
                        4,
                        d,
                        node.totalCost + cost,
                        pathLen,
                        node,
                    )
                )
            }
        }
        // ChatUtils.sendMessage("calced solution in ${System.currentTimeMillis() - startTime}ms and ${StringUtils.addCommas(ops)} ops with ${minCost.last()}")

        return best ?: fastSol
    }

    private fun findPathFast(thisBlocks: Array<CompWorld?>, len: Int, src: CompWorld): Solution? {
        val unvisited = run {
            var m = 0L
            thisBlocks.forEachIndexed { i, v ->
                if (v != null) m = m or (1L shl i)
            }
            return@run m
        }

        val minCost = IntArray(len) { Int.MAX_VALUE }

        val visitedHist = LongArray(len)
        val route = arrayOfNulls<State>(len)
        val queue = ArrayDeque<State>()

        queue.add(State(src.id, 4, 4, if (src.comp == start) State.BEGIN_ON_START_INCENTIVE else 0))

        var ops = 0
        // val startTime = System.currentTimeMillis()
        while (queue.isNotEmpty()) {
            if (++ops == 10_000) break

            val node = queue.removeLast()

            var pathLen = node.length

            val curr = thisBlocks[node.id] ?: continue

            minCost[pathLen] = min(minCost[pathLen], node.totalCost)
            route[pathLen] = node
            val visited = visitedHist[pathLen] or curr.mask

            if (++pathLen == len) {
                // ChatUtils.sendMessage("calced fast solution in ${System.currentTimeMillis() - startTime}ms and ${StringUtils.addCommas(ops)} ops with ${node.totalCost}")
                return Solution(minCost, route.requireNoNulls())
            }

            visitedHist[pathLen] = visited
            val wall = curr.walls[node.od]

            Direction.nextD[node.od].forEach { d ->
                val id = curr.neighbors[d]
                if (id < 0) return@forEach

                val mask = 1L shl id
                if (visited and mask != 0L) return@forEach

                val v = unvisited and (visited or mask).inv()
                if (!isConnected(v)) return@forEach

                val c = if (pathLen == 1) d else node.oc
                val cost = node.cost(d, c, wall)

                queue.add(
                    State(
                        id,
                        if (d != node.od && d == c && node.od != 4) 4 else d,
                        c,
                        node.totalCost + cost,
                        pathLen,
                        node,
                    )
                )
            }
        }

        return null
    }

    data class State(
        val id: Int,
        val od: Int,
        val oc: Int,
        val totalCost: Int,
        val length: Int,
        val moveCost: Int,
        val cameraCost: Int,
    ) {
        constructor(
            id: Int,
            od: Int,
            oc: Int,
            totalCost: Int = 0,
            length: Int = 0,
            prev: State? = null,
        ) : this(
            id,
            od, oc,
            totalCost, length,
            prev?.nextMoveCost(od) ?: 0,
            prev?.nextCameraCost(od, oc) ?: 0,
        )

        fun cost(d: Int, c: Int, wall: Boolean): Int {
            return moveCost +
                (
                    if (od == 4) 0
                    else if (d == od) 0
                    else if (d == c) CAMERA_TP_COST
                    else TURN_COST + (if (wall) 0 else NOT_WALL_TURN_PENALTY)
                ) +
                (if (c == oc) 0 else cameraCost) +
                (
                    if (d == c xor 2) BACKWARDS_MOVEMENT_PENALTY
                    else if (d != c) SIDE_MOVEMENT_PENALTY
                    else 0
                )
        }

        fun heur(totalLen: Int): Int {
            return 10
        }

        fun nextMoveCost(d: Int) =
            if (d == od) max(0, moveCost - MOVEMENT_COST_REDUCTION)
            else if (d != 4) min(INITIAL_MOVEMENT_COST, moveCost + TURN_MOVEMENT_PENALTY)
            else INITIAL_MOVEMENT_COST

        fun nextCameraCost(d: Int, c: Int) =
            if (d == 4) INITIAL_CHANGE_CAMERA_COST
            else if (c == oc) max(0, cameraCost - CHANGE_CAMERA_COST_REDUCTION)
            else INITIAL_CHANGE_CAMERA_COST

        companion object {
            const val BEGIN_ON_START_INCENTIVE = -20
            const val INITIAL_MOVEMENT_COST = 4
            const val MOVEMENT_COST_REDUCTION = 1
            const val TURN_MOVEMENT_PENALTY = 1
            const val BACKWARDS_MOVEMENT_PENALTY = 5
            const val SIDE_MOVEMENT_PENALTY = 1
            const val TURN_COST = 5
            const val CAMERA_TP_COST = 3
            const val INITIAL_CHANGE_CAMERA_COST = 15
            const val CHANGE_CAMERA_COST_REDUCTION = 3
            const val NOT_WALL_TURN_PENALTY = 100
        }
    }

    enum class Direction(val dx: Int, val dz: Int) {
        PosX(1, 0),
        PosZ(0, 1),
        NegX(-1, 0),
        NegZ(0, -1),
        Empty(0, 0);

        operator fun component1() = dx
        operator fun component2() = dz

        companion object {
            @JvmField
            val nextD = arrayOf(
                intArrayOf(0, 2, 1, 3),
                intArrayOf(1, 3, 0, 2),
                intArrayOf(2, 0, 3, 1),
                intArrayOf(3, 1, 2, 0),
                intArrayOf(0, 1, 2, 3),
            )
            @JvmField
            val nextC = arrayOf(
                intArrayOf(0),
                intArrayOf(1),
                intArrayOf(2),
                intArrayOf(3),
                intArrayOf(0, 1, 2, 3),
            )
        }
    }

    class CompWorld(
        val id: Int, val mask: Long,
        val comp: Coord,
        val world: Pair<Int, Int>,
        val parity: Int,
        val neighbors: IntArray,
        val walls: BooleanArray,
    )

    class Solution(val cost: IntArray, val path: Array<State>)
}