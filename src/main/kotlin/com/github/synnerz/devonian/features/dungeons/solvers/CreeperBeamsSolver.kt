package com.github.synnerz.devonian.features.dungeons.solvers

import com.github.synnerz.devonian.api.ChatUtils
import com.github.synnerz.devonian.api.WorldUtils
import com.github.synnerz.devonian.api.dungeon.DungeonEvent
import com.github.synnerz.devonian.api.dungeon.Stages
import com.github.synnerz.devonian.api.events.*
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.utils.BasicState
import com.github.synnerz.devonian.utils.render.Render3DImmediate
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.Blocks
import java.awt.Color
import kotlin.math.sqrt

object CreeperBeamsSolver : Feature(
    "creeperBeamsSolver",
    "Highlights the correct blocks to hit whenever doing the creeper beams puzzle.",
    Categories.DUNGEONS,
    "catacombs",
    subcategory = "Solvers",
    searchTags = setOf("puzzle"),
) {
    override fun createRequirements(): List<BasicState<Boolean>?> {
        return super.createRequirements() + listOf(Stages.Clear.isActiveState)
    }

    private val solutions = listOf(
        BeamsSolutionData(15, 74, 15, 15, 84, 13),
        BeamsSolutionData(15, 78, 3, 15, 76, 27),
        BeamsSolutionData(5, 76, 24, 24, 77, 7),
        BeamsSolutionData(2, 75, 16, 27, 78, 14),
        BeamsSolutionData(4, 72, 8, 25, 79, 21),
        BeamsSolutionData(4, 75, 9, 25, 76, 23),
        BeamsSolutionData(22, 80, 22, 4, 72, 8),
        BeamsSolutionData(3, 76, 18, 26, 78, 12),
        BeamsSolutionData(9, 81, 20, 26, 70, 7),
        BeamsSolutionData(18, 81, 21, 9, 69, 3),
        BeamsSolutionData(18, 82, 8, 10, 69, 27),
        BeamsSolutionData(25, 76, 23, 6, 74, 5),
        BeamsSolutionData(6, 74, 5, 25, 76, 23),
        BeamsSolutionData(26, 70, 7, 9, 81, 20)
    )
    private val colorChoicesOutline = listOf(
        Color.CYAN,
        Color.GREEN,
        Color.RED,
        Color.ORANGE
    )
    private val colorChoicesFilled = listOf(
        Color(0, 255, 255, 80),
        Color(0, 255, 0, 80),
        Color(255, 0, 0, 80),
        Color(255, 165, 0, 80)
    )
    private val solutionList = mutableListOf<BeamsSolutionData>()
    var blockPair: Pair<Int, Int>? = null
    var inRoom = false
    var enteredAt = -1

    data class BeamsSolutionData(
        val x1: Int,
        val y1: Int,
        val z1: Int,
        val x2: Int,
        val y2: Int,
        val z2: Int,
        var blacklisted: Boolean = false
    ) {
        fun containsOneOf(x: Int, y: Int, z: Int): Boolean
            = (x1 == x && y1 == y && z1 == z) || (x2 == x && y2 == y && z2 == z)
    }

    override fun initialize() {
        on<DungeonEvent.RoomEnter> {
            val room = it.room
            if (room.name != "Creeper Beams") return@on
            inRoom = true
            val roomComp = room.fromComp(15, 15) ?: return@on
            blockPair = roomComp
            val blockState = WorldUtils.getBlockState(roomComp.first, 74, roomComp.second) ?: return@on
            if (blockState.block != Blocks.SEA_LANTERN) return@on

            val currentSol = mutableListOf<BeamsSolutionData>()

            for (solution in solutions) {
                val ( x1, y1, z1, x2, y2, z2 ) = solution
                val comp1 = room.fromComp(x1, z1) ?: continue
                val comp2 = room.fromComp(x2, z2) ?: continue
                val block1 = WorldUtils.getBlockState(comp1.first, y1, comp1.second)?.block ?: continue
                val block2 = WorldUtils.getBlockState(comp2.first, y2, comp2.second)?.block ?: continue
                if (block1 != Blocks.SEA_LANTERN || block2 != Blocks.SEA_LANTERN) continue

                currentSol.add(
                    BeamsSolutionData(
                        comp1.first,
                        y1,
                        comp1.second,
                        comp2.first,
                        y2,
                        comp2.second
                    )
                )

                // In case there is already a solution with one of the current solution's points
                // we just continue (skip it) because we don't want to have two lines heading
                // towards the same block
//                if (solutionList.any { data ->
//                    data.containsOneOf(comp1.first, y1, comp1.second) ||
//                    data.containsOneOf(comp2.first, y2, comp2.second)
//                }) continue
//
//                solutionList.add(
//                    BeamsSolutionData(
//                        comp1.first,
//                        y1,
//                        comp1.second,
//                        comp2.first,
//                        y2,
//                        comp2.second
//                    )
//                )
            }

            currentSol.sortedBy {
                val dx0 = it.x1 - roomComp.first
                val dy0 = it.y1 - 74
                val dz0 = it.z1 - roomComp.second
                val dx1 = it.x2 - roomComp.first
                val dy1 = it.y2 - 74
                val dz1 = it.z2 - roomComp.second

                sqrt((dx0 * dx0 + dy0 * dy0 + dz0 * dz0).toDouble()) +
                sqrt((dx1 * dx1 + dy1 * dy1 + dz1 * dz1).toDouble())
            }.forEach {
                if (solutionList.any { data ->
                    data.containsOneOf(it.x1, it.y1, it.z1) ||
                    data.containsOneOf(it.x2, it.y2, it.z2)
                }) return@forEach

                solutionList.add(it)
            }
        }

        on<DungeonEvent.RoomLeave> {
            if (!inRoom) return@on
            inRoom = false
            blockPair = null
            enteredAt = -1
            solutionList.clear()
        }

        on<RenderWorldEvent> {
            if (solutionList.isEmpty()) return@on
            for (idx in solutionList.indices) {
                if (idx >= 4) break
                val solution = solutionList[idx]
                if (solution.blacklisted) continue

                Render3DImmediate.renderWireframeBox(
                    solution.x1.toDouble(), solution.y1.toDouble(), solution.z1.toDouble(),
                    1.0, 1.0,
                    colorChoicesOutline[idx],
                    phase = true,
                )
                Render3DImmediate.renderFilledBox(
                    solution.x1.toDouble(), solution.y1.toDouble(), solution.z1.toDouble(),
                    1.0, 1.0,
                    colorChoicesFilled[idx]
                )

                Render3DImmediate.renderWireframeBox(
                    solution.x2.toDouble(), solution.y2.toDouble(), solution.z2.toDouble(),
                    1.0, 1.0,
                    colorChoicesOutline[idx],
                    phase = true,
                )
                Render3DImmediate.renderFilledBox(
                    solution.x2.toDouble(), solution.y2.toDouble(), solution.z2.toDouble(),
                    1.0, 1.0,
                    colorChoicesFilled[idx]
                )
            }

            Render3DImmediate.renderLines(true) {
                for (idx in solutionList.indices) {
                    if (idx >= 4) break
                    val solution = solutionList[idx]
                    if (solution.blacklisted) continue

                    val color = colorChoicesOutline[idx]
                    submit(
                        solution.x1 + 0.5, solution.y1 + 0.5, solution.z1 + 0.5,
                        solution.x2 + 0.5, solution.y2 + 0.5, solution.z2 + 0.5,
                        color
                    )
                }
            }
        }

        on<MultiBlockUpdateEvent> {
            if (!inRoom || solutionList.isEmpty()) return@on

            it.forEach { blockPos, blockState ->
                if (blockState.block != Blocks.PRISMARINE) return@forEach
                onBlockUpdate(blockPos)
            }
        }

        on<BlockUpdateEvent> {
            if (!inRoom || solutionList.isEmpty()) return@on

            if (it.blockState.block != Blocks.PRISMARINE) return@on
            onBlockUpdate(it.blockPos)
        }
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        inRoom = false
        blockPair = null
        enteredAt = -1
        solutionList.clear()
    }

    private fun onBlockUpdate(blockPos: BlockPos) {
        for (data in solutionList) {
            if (!data.containsOneOf(blockPos.x, blockPos.y, blockPos.z) || data.blacklisted) continue
            data.blacklisted = true
            if (enteredAt == -1) enteredAt = EventBus.serverTicks()
        }

        if (enteredAt == -1) return

        var blacklisting = 0
        for (idx in 0..3) {
            val data = solutionList.getOrNull(idx) ?: continue
            if (!data.blacklisted) continue
            blacklisting++
        }
        if (blacklisting != 4 || !PuzzleTimers.isEnabled()) return

        val time = (EventBus.serverTicks() - enteredAt) * 0.05
        val seconds = "%.2fs".format(time)
        ChatUtils.sendMessage("&bCreeper Beams took&f: &6$seconds", true)
        enteredAt = -1
    }
}