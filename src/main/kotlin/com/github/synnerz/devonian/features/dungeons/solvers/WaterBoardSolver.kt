package com.github.synnerz.devonian.features.dungeons.solvers

import com.github.synnerz.barrl.Context
import com.github.synnerz.devonian.api.ChatUtils
import com.github.synnerz.devonian.api.WorldUtils
import com.github.synnerz.devonian.api.dungeon.DungeonEvent
import com.github.synnerz.devonian.api.dungeon.DungeonScanner
import com.github.synnerz.devonian.api.events.*
import com.github.synnerz.devonian.features.Feature
import com.google.gson.Gson
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.Vec3
import java.awt.Color

object WaterBoardSolver : Feature(
    "waterBoardSolver",
    "Highlights the most \"efficient\" levers to flick at the specified time to get a one flow solution in water board puzzle",
    "Dungeons",
    "catacombs"
) {
    // TODO: optimize these since they are not the most optimal currently.
    @Suppress("unchecked_cast")
    private val solutionsData = Gson().fromJson(
        this::class.java.getResourceAsStream("/assets/devonian/dungeons/WaterBoardSolutions.json")
            ?.bufferedReader()
            .use { it?.readText() },
        Map::class.java
    ) as Map<String, Map<String, Map<String, List<Double>>>>

    // y is either 77 or 78
    private val TOP_LEFT_BLOCK = 16 to 26
    private val TOP_RIGHT_BLOCk = 14 to 26
    private val SEA_LANTERN_MIDDLE = 15 to 27 // 77y
    private val PURPLE_WOOL = 15 to 19 // 57y
    private val leverPos = mapOf(
        "minecraft:quartz_block" to (20 to 20),
        "minecraft:gold_block" to (20 to 15),
        "minecraft:coal_block" to (20 to 10),
        "minecraft:diamond_block" to (10 to 20),
        "minecraft:emerald_block" to (10 to 15),
        "minecraft:hardened_clay" to (10 to 10),
        "minecraft:water" to (15 to 5)
    )
    private val woolOrder = listOf(
        Blocks.PURPLE_WOOL, // 10
        Blocks.ORANGE_WOOL, // 1
        Blocks.BLUE_WOOL, // 11
        Blocks.LIME_WOOL, // 5
        Blocks.RED_WOOL // 14
    )
    private val FIRST_COLOR = Color(0, 255, 0, 255)
    private val SECOND_COLOR = Color(255, 165, 0, 255)
    var inWaterBoard = false
    var variant: Int? = null
    var subvariant: String? = null
    var currentSolution: Map<String, MutableList<Double>>? = null
    var openedWaterAt = -1

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

        on<ServerTickEvent> {
            if (!inWaterBoard || variant == null || subvariant?.length == 3) return@on

            val room = DungeonScanner.currentRoom ?: return@on
            subvariant = ""

            for (idx in woolOrder.indices) {
                val woolType = woolOrder[idx] ?: continue
                val roomPos = room.fromComp(PURPLE_WOOL.first, PURPLE_WOOL.second - idx) ?: continue
                WorldUtils.fromBlockTypeOrNull(roomPos.first, 57, roomPos.second, woolType) ?: continue

                subvariant += "$idx"
            }

            if (subvariant!!.length == 3) {
                currentSolution = buildMap {
                    val sol = solutionsData["$variant"]?.get(subvariant)
                    for (solution in sol!!)
                        put(solution.key, solution.value.toMutableList())
                }
                println("Devonian\$WaterBoard[variant=\"$variant\", subvariant=\"$subvariant\"]")
            }
            else
                subvariant = null
        }

        on<DungeonEvent.RoomLeave> {
            if (!inWaterBoard) return@on
            inWaterBoard = false
            variant = null
            subvariant = null
            currentSolution = null
        }

        on<PacketSentEvent> { event ->
            val packet = event.packet
            if (packet !is ServerboundUseItemOnPacket) return@on
            if (!inWaterBoard) return@on
            val result = packet.hitResult
            val pos = result.blockPos
            val x = pos.x
            val y = pos.y
            val z = pos.z

            WorldUtils.fromBlockTypeOrNull(x, y, z, Blocks.CHEST)?.let {
                if (openedWaterAt == -1 || currentSolution == null) return@on
                val room = DungeonScanner.currentRoom ?: return@on
                val compPos = room.fromPos(x, z) ?: return@on
                if (compPos.first != 15 || y != 56 || compPos.second != 22) return@on
                if (currentSolution!!.values.any { it.isNotEmpty() }) return@on

                val time = (EventBus.serverTicks() - openedWaterAt) * 0.05
                val seconds = "%.2fs".format(time)
                ChatUtils.sendMessage("&bWater Board took&f: &6$seconds", true)
                openedWaterAt = -1

                return@on
            }

            WorldUtils.fromBlockTypeOrNull(x, y, z, Blocks.LEVER) ?: return@on

            val room = DungeonScanner.currentRoom ?: return@on
            val compPos = room.fromPos(x, z) ?: return@on

            for (entry in leverPos) {
                val k = entry.key
                val v = entry.value
                if (v != compPos) continue

                if (k == "minecraft:water" && openedWaterAt == -1)
                    openedWaterAt = EventBus.serverTicks()

                if (currentSolution == null) continue

                val solutionEntry = currentSolution?.get(k) ?: continue
                if (solutionEntry.isEmpty()) continue
                val time = solutionEntry.first()
                val remaining =
                    if (openedWaterAt == -1) time
                    else time - ((EventBus.serverTicks() - openedWaterAt) * 0.05)

                if (time <= 0) {
                    solutionEntry.removeFirst()
                    continue
                }

                if (remaining >= 1) continue

                solutionEntry.removeFirst()
            }
        }

        on<RenderWorldEvent> {
            if (!inWaterBoard || currentSolution == null || currentSolution!!.isEmpty()) return@on

            val room = DungeonScanner.currentRoom ?: return@on
            val levers = mutableListOf<Pair<Int, Int>>()

            for (entry in currentSolution!!.entries) {
                val name = entry.key
                val arr = entry.value
                val y = if (name == "minecraft:water") 60.0 else 61.0
                val compPos = leverPos[name] ?: return@on
                val roomPos = room.fromComp(compPos.first, compPos.second) ?: return@on

                for (idx in arr.indices) {
                    val time = arr[idx]
                    val _y = y + (idx * 1)

                    Context.Immediate?.renderBox(
                        roomPos.first.toDouble(), _y, roomPos.second.toDouble(),
                        if (idx == 0) FIRST_COLOR else SECOND_COLOR,
                        true
                    )

                    if (openedWaterAt == -1) {
                        val title = if (time <= 0.0) "§aClick Now!" else "§e${time}s"

                        Context.Immediate?.renderString(
                            title,
                            roomPos.first + 0.5, _y + 0.5, roomPos.second + 0.5,
                            increase = true,
                            phase = true
                        )
                        continue
                    }

                    val remaining = time - ((EventBus.serverTicks() - openedWaterAt) * 0.05)
                    val title = if (remaining <= 0.0) "§aClick Now!" else "§e${"%.2fs".format(remaining)}"
                    if (levers.size < 2 && remaining <= 4.0 && !levers.contains(roomPos))
                        levers.add(roomPos)

                    Context.Immediate?.renderString(
                        title,
                        roomPos.first + 0.5, _y + 0.5, roomPos.second + 0.5,
                        increase = true,
                        phase = true
                    )
                }
            }

            // Render a line to the closest next lever
            val lever1 = levers.getOrNull(0) ?: return@on
            val lever2 = levers.getOrNull(1) ?: return@on

            BlazeSolver.renderLine(
                Vec3(lever1.first + 0.5, 60.0, lever1.second + 0.5),
                Vec3(lever2.first.toDouble(), 60.0, lever2.second.toDouble()),
                FIRST_COLOR,
                SECOND_COLOR
            )
        }
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        inWaterBoard = false
        variant = null
        subvariant = null
        currentSolution = null
        openedWaterAt = -1
    }
}