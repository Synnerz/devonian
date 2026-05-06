package com.github.synnerz.devonian.features.dungeons.solvers

import com.github.synnerz.devonian.api.ChatUtils
import com.github.synnerz.devonian.api.WorldUtils
import com.github.synnerz.devonian.api.dungeon.DungeonEvent
import com.github.synnerz.devonian.api.dungeon.DungeonScanner
import com.github.synnerz.devonian.api.dungeon.Stages
import com.github.synnerz.devonian.api.events.*
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.utils.BasicState
import com.github.synnerz.devonian.utils.render.Render3DImmediate
import com.google.gson.Gson
import net.minecraft.world.level.block.Blocks
import java.awt.Color
import java.util.*
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

object WaterBoardSolver : Feature(
    "waterBoardSolver",
    "Highlights the most \"efficient\" levers to flick at the specified time to get a one flow solution in water board puzzle.",
    Categories.DUNGEONS,
    "catacombs",
    subcategory = "Solvers",
    searchTags = setOf("wb", "puzzle"),
) {
    override fun createRequirements(): List<BasicState<Boolean>?> {
        return super.createRequirements() + listOf(Stages.Clear.isActiveState)
    }

    private val SETTING_SOLUTION_MODE = addSelection(
        "solutionMode",
        0,
        listOf("Desco1", "Efficient"),
        "Choose the waterboard solutions mode.",
        "WaterBoardSolver Mode"
    )

    @Suppress("unchecked_cast")
    private val solutionsData = (Gson().fromJson(
        this::class.java.getResourceAsStream("/assets/devonian/dungeons/WaterBoardSolutions.json")
            ?.bufferedReader()
            .use { it?.readText() },
        Map::class.java
    ) as Map<String, Map<String, Map<String, List<Double>>>>)
        .mapValues { (_, subvariants) ->
            subvariants.mapValues { (_, levers) ->
                levers.entries.associateTo(EnumMap(Lever::class.java)) { (lever, times) ->
                     Lever.from(lever) to times.map { (it * 20).roundToInt() }
                }
            }
        }

    // Thanks to FlameOfWar for sending me these which were made by Moody and PandaguinDK all credits to them
    @Suppress("unchecked_cast")
    private val efficientSolutionsData = (Gson().fromJson(
        this::class.java.getResourceAsStream("/assets/devonian/dungeons/EfficientWaterboardSolutions.json")
            ?.bufferedReader()
            .use { it?.readText() },
        Map::class.java
    ) as Map<String, Map<String, Map<String, List<Double>>>>)
        .mapValues { (_, subvariants) ->
            subvariants.mapValues { (_, levers) ->
                levers.entries.associateTo(EnumMap(Lever::class.java)) { (lever, times) ->
                    Lever.from(lever) to times.map { (it * 20).roundToInt() }
                }
            }
        }

    // y is either 77 or 78
    private val TOP_LEFT_BLOCK = 16 to 26
    private val TOP_RIGHT_BLOCk = 14 to 26
    private val SEA_LANTERN_MIDDLE = 15 to 27 // 77y
    private val PURPLE_WOOL = 15 to 19 // 57y
    private val woolOrder = listOf(
        Blocks.WOOL.purple, // 10
        Blocks.WOOL.orange, // 1
        Blocks.WOOL.blue, // 11
        Blocks.WOOL.lime, // 5
        Blocks.WOOL.red // 14
    )
    private val FIRST_COLOR = Color(0, 255, 0, 255)
    private val SECOND_COLOR = Color(255, 165, 0, 255)
    var inWaterBoard = false
    var variant: Int? = null
    var subvariant: String? = null
    var solution: MutableList<SolutionEntry>? = null
    var openedWaterAt = -1
    private val solutionSort = Comparator.comparingInt<Pair<Lever, Int>> { it.second }.thenBy { it.first.ordinal }

    data class SolutionEntry(
        val x: Int, val y: Int, val z: Int,
        val time: Int, val lever: Lever,
        val x1: Double, val z1: Double,
        val wx: Double, val wz: Double,
        val y1: Double, val h: Double,
    )
    enum class Lever(
        val type: String,
        val x: Int, val y: Int, val z: Int,
        val x1: Double, val z1: Double,
        val x2: Double, val z2: Double,
        val y1: Double, val h: Double,
    ) {
        Quartz("quartz_block", 20, 61, 20, 20.625, 20.3125, 21.0, 20.6875, 61.25, 0.5),
        Gold("gold_block", 20, 61, 15, 20.625, 15.3125, 21.0, 15.6875, 61.25, 0.5),
        Coal("coal_block", 20, 61, 10, 20.625, 10.3125, 21.0, 10.6875, 61.25, 0.5),
        Diamond("diamond_block", 10, 61, 20, 10.0, 20.3125, 10.375, 20.6875, 61.25, 0.5),
        Emerald("emerald_block", 10, 61, 15, 10.0, 15.3125, 10.375, 15.6875, 61.25, 0.5),
        Terracotta("hardened_clay", 10, 61, 10, 10.0, 10.3125, 10.375, 10.6875, 61.25, 0.5),
        Water("water", 15, 60, 5, 15.25, 5.3125, 15.75, 5.6875, 60.0, 0.375);

        companion object {
            fun from(type: String) = entries.find { it.type == type }!!
        }
    }

    override fun initialize() {
        on<DungeonEvent.RoomEnter> {
            val room = it.room
            if (room.name != "Water Board") return@on
            inWaterBoard = true

            // Finding out what Y level the puzzle is currently in
            var currentY = 77
            val lantern = room.fromComp(SEA_LANTERN_MIDDLE.first, SEA_LANTERN_MIDDLE.second) ?: return@on
            val lanternState = WorldUtils.fromBlockTypeOrNull(lantern.first, currentY, lantern.second, Blocks.SEA_LANTERN)
            if (lanternState == null) currentY = 78

            val topLeft = room.fromComp(TOP_LEFT_BLOCK.first, TOP_LEFT_BLOCK.second) ?: return@on
            val topRight = room.fromComp(TOP_RIGHT_BLOCk.first, TOP_RIGHT_BLOCk.second) ?: return@on

            var leftBlockState = WorldUtils.getBlockState(topLeft.first, currentY, topLeft.second) ?: return@on
            var rightBlockState = WorldUtils.getBlockState(topRight.first, currentY, topRight.second) ?: return@on

            if (leftBlockState.isAir || leftBlockState.block == Blocks.STONE) {
                val newPos = room.fromComp(TOP_LEFT_BLOCK.first, TOP_LEFT_BLOCK.second + 1) ?: return@on
                leftBlockState = WorldUtils.getBlockState(newPos.first, currentY, newPos.second) ?: return@on
            }
            if (rightBlockState.isAir || rightBlockState.block == Blocks.STONE) {
                val newPos = room.fromComp(TOP_RIGHT_BLOCk.first, TOP_RIGHT_BLOCk.second + 1) ?: return@on
                rightBlockState = WorldUtils.getBlockState(newPos.first, currentY, newPos.second) ?: return@on
            }

            val left = leftBlockState.block
            val right = rightBlockState.block

            variant = when {
                left == Blocks.GOLD_BLOCK && right == Blocks.TERRACOTTA -> 0
                left == Blocks.EMERALD_BLOCK && right == Blocks.QUARTZ_BLOCK -> 1
                left == Blocks.QUARTZ_BLOCK && right == Blocks.DIAMOND_BLOCK -> 2
                left == Blocks.GOLD_BLOCK && right == Blocks.QUARTZ_BLOCK -> 3
                else -> null
            } ?: return@on
        }

        on<ClientThreadServerTickEvent> {
            if (!inWaterBoard || variant == null || subvariant?.length == 3) return@on

            val room = DungeonScanner.currentRoom ?: return@on
            if (!room.hasRotation()) return@on
            subvariant = ""

            for (idx in woolOrder.indices) {
                val woolType = woolOrder[idx] ?: continue
                val roomPos = room.fromComp(PURPLE_WOOL.first, PURPLE_WOOL.second - idx) ?: continue
                WorldUtils.fromBlockTypeOrNull(roomPos.first, 57, roomPos.second, woolType) ?: continue

                subvariant += "$idx"
            }

            if (subvariant!!.length == 3) {
                val solutionMap = if (SETTING_SOLUTION_MODE.get() == 0) solutionsData else efficientSolutionsData
                val sol = solutionMap["$variant"]?.get(subvariant)
                if (sol == null) ChatUtils.sendMessage("&4Unknown water board variant: $variant/$subvariant")
                else {
                    val arr = sol.flatMapTo(mutableListOf()) { (lever, times) -> times.map { lever to it } }
                    arr.sortWith(solutionSort)
                    solution = arr.mapTo(mutableListOf()) {
                        val pos = room.fromComp(it.first.x, it.first.z) ?: return@on
                        val p1 = room.fromComp(it.first.x1, it.first.z1) ?: return@on
                        val p2 = room.fromComp(it.first.x2, it.first.z2) ?: return@on
                        val x1 = min(p1.first, p2.first)
                        val z1 = min(p1.second, p2.second)
                        val x2 = max(p1.first, p2.first)
                        val z2 = max(p1.second, p2.second)
                        SolutionEntry(
                            pos.first, it.first.y, pos.second,
                            it.second, it.first,
                            x1, z1,
                            x2 - x1, z2 - z1,
                            it.first.y1, it.first.h,
                        )
                    }
                }
                println("Devonian\$WaterBoard[variant=\"$variant\", subvariant=\"$subvariant\"]")
            } else subvariant = null
        }

        on<DungeonEvent.RoomLeave> {
            if (!inWaterBoard) return@on
            inWaterBoard = false
            variant = null
            subvariant = null
            solution = null
        }

        on<UseItemOnEvent> { event ->
            if (!inWaterBoard) return@on
            val result = event.blockHitResult
            val pos = result.blockPos
            val x = pos.x
            val y = pos.y
            val z = pos.z

            val room = DungeonScanner.currentRoom ?: return@on
            val compPos = room.fromPos(x, z) ?: return@on

            val state = WorldUtils.getBlockState(x, y, z) ?: return@on
            if (state.block == Blocks.CHEST) {
                if (openedWaterAt == -1) return@on
                if (compPos.first != 15 || y != 56 || compPos.second != 22) return@on

                val sol = solution ?: return@on
                if (sol.isNotEmpty() || !PuzzleTimers.isEnabled()) return@on

                val time = (EventBus.serverTicks() - openedWaterAt) * 0.05
                val seconds = "%.2fs".format(time)
                ChatUtils.sendMessage("&bWater Board took&f: &6$seconds", true)
                openedWaterAt = -1

                return@on
            }

            if (state.block != Blocks.LEVER) return@on

            if (
                openedWaterAt == -1 &&
                compPos.first == Lever.Water.x &&
                compPos.second == Lever.Water.z &&
                y == Lever.Water.y
            ) openedWaterAt = EventBus.serverTicks()

            val sol = solution ?: return@on

            val idx = sol.indexOfFirst { it.x == x && it.y == y && it.z == z }
            if (idx < 0) return@on

            val time = sol[idx].time
            val remaining =
                if (openedWaterAt == -1) time
                else time - (EventBus.serverTicks() - openedWaterAt)

            if (time <= 0 || remaining < 20) sol.removeAt(idx)
        }

        on<RenderWorldEvent> {
            if (!inWaterBoard) return@on

            val sol = solution ?: return@on
            val levers = EnumMap<Lever, Int>(Lever::class.java)

            var lastX = 0.0
            var lastY = 0.0
            var lastZ = 0.0
            sol.forEachIndexed { i, entry ->
                val yo = levers.merge(entry.lever, 1, Int::plus)!! - 1

                Render3DImmediate.renderWireframeBox(
                    entry.x1, entry.y1 + yo, entry.z1,
                    entry.wx, entry.h,
                    if (i == 0) FIRST_COLOR else SECOND_COLOR,
                    wz = entry.wz,
                    phase = false,
                    lineWidth = 2.0,
                )

                val remaining =
                    if (openedWaterAt == -1) entry.time
                    else entry.time - (EventBus.serverTicks() - openedWaterAt)
                val title = if (remaining <= 0) "§aClick Now!" else "§e${"%.2fs".format(remaining * 0.05)}"

                val x = entry.x + 0.5
                val y = entry.y + 0.5 + yo
                val z = entry.z + 0.5

                Render3DImmediate.renderString(title, x, y, z)

                if (i in 1 .. 2) Render3DImmediate.renderLine(
                    lastX, lastY, lastZ,
                    x, y, z,
                    if (i == 1) FIRST_COLOR else SECOND_COLOR,
                )

                lastX = x
                lastY = y
                lastZ = z
            }
        }
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        inWaterBoard = false
        variant = null
        subvariant = null
        solution = null
        openedWaterAt = -1
    }
}