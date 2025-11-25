package com.github.synnerz.devonian.features.dungeons.solvers

import com.github.synnerz.barrl.Context
import com.github.synnerz.devonian.api.WorldUtils
import com.github.synnerz.devonian.api.dungeon.DungeonEvent
import com.github.synnerz.devonian.api.events.RenderWorldEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.features.Feature
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.Vec3
import java.awt.Color

object CreeperBeamsSolver : Feature(
    "creeperBeamsSolver",
    "Highlights the correct blocks to hit whenever doing the creeper beams puzzle",
    "Dungeons",
    "catacombs"
) {
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

    data class BeamsSolutionData(
        val x1: Int,
        val y1: Int,
        val z1: Int,
        val x2: Int,
        val y2: Int,
        val z2: Int
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

            for (solution in solutions) {
                val ( x1, y1, z1, x2, y2, z2 ) = solution
                val comp1 = room.fromComp(x1, z1) ?: continue
                val comp2 = room.fromComp(x2, z2) ?: continue
                val block1 = WorldUtils.getBlockState(comp1.first, y1, comp1.second)?.block ?: continue
                val block2 = WorldUtils.getBlockState(comp2.first, y2, comp2.second)?.block ?: continue
                if (block1 != Blocks.SEA_LANTERN || block2 != Blocks.SEA_LANTERN) continue

                // In case there is already a solution with one of the current solution's points
                // we just continue (skip it) because we don't want to have two lines heading
                // towards the same block
                if (solutionList.any { data ->
                    data.containsOneOf(comp1.first, y1, comp1.second) ||
                    data.containsOneOf(comp2.first, y2, comp2.second)
                }) continue

                solutionList.add(
                    BeamsSolutionData(
                        comp1.first,
                        y1,
                        comp1.second,
                        comp2.first,
                        y2,
                        comp2.second
                    )
                )
            }
        }

        on<DungeonEvent.RoomLeave> {
            if (!inRoom) return@on
            inRoom = false
            blockPair = null
            solutionList.clear()
        }

        on<RenderWorldEvent> {
            if (solutionList.isEmpty()) return@on
            for (idx in solutionList.indices) {
                if (idx >= 4) break
                val solution = solutionList[idx]

                Context.Immediate?.renderFilledBox(
                    solution.x1.toDouble(), solution.y1.toDouble(), solution.z1.toDouble(),
                    colorChoicesFilled[idx]
                )
                Context.Immediate?.renderBox(
                    solution.x1.toDouble(), solution.y1.toDouble(), solution.z1.toDouble(),
                    colorChoicesOutline[idx],
                    true
                )

                Context.Immediate?.renderFilledBox(
                    solution.x2.toDouble(), solution.y2.toDouble(), solution.z2.toDouble(),
                    colorChoicesFilled[idx]
                )
                Context.Immediate?.renderBox(
                    solution.x2.toDouble(), solution.y2.toDouble(), solution.z2.toDouble(),
                    colorChoicesOutline[idx],
                    true
                )

                // TODO: make the renderLine from BlazeSolver into barrl or something
                BlazeSolver.renderLine(
                    Vec3(solution.x1 + 0.5, solution.y1 - 1.0, solution.z1 + 0.5),
                    Vec3(solution.x2.toDouble(), solution.y2 - 1.0, solution.z2.toDouble()),
                    colorChoicesOutline[idx]
                )
            }
        }
        // TODO: listen to packet block change and remove each solution
        //  it is _not_ required for dungeon update but later on (since it's simple)
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        inRoom = false
        blockPair = null
        solutionList.clear()
    }
}