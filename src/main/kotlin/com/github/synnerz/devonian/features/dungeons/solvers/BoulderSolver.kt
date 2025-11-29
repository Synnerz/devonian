package com.github.synnerz.devonian.features.dungeons.solvers

import com.github.synnerz.barrl.Context
import com.github.synnerz.devonian.api.WorldUtils
import com.github.synnerz.devonian.api.dungeon.DungeonEvent
import com.github.synnerz.devonian.api.dungeon.DungeonRoom
import com.github.synnerz.devonian.api.events.BlockPlaceEvent
import com.github.synnerz.devonian.api.events.RenderWorldEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.features.Feature
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.HitResult
import java.awt.Color
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.abs

object BoulderSolver : Feature(
    "boulderSolver",
    "Highlights the blocks you should click to successfully solve the Boulder puzzle",
    "Dungeons",
    "catacombs"
) {
    private val OUTLINE_COLOR = Color(0, 255, 255, 255)
    private val FILLED_COLOR = Color(0, 255, 255, 80)
    private val solutions = mapOf(
        "100101001000000010101001111101010101010101" to listOf(
            21 to 11,
            22 to 21
        ),
        "010000010111101001010011100000101110000111" to listOf(
            13 to 12
        ),
        "000000011111101001010011100000101110000110" to listOf(
            13 to 12
        ),
        "100000111101111011101110001110111010000000" to listOf(
            21 to 14,
            15 to 17,
            15 to 20,
            13 to 21
        ),
        "110001110111011010001100111111100011000001" to listOf(
            15 to 14,
            19 to 21
        ),
        "100100101000100010100010101000101000100010" to listOf(
            22 to 21
        ),
        "000000010101110101010011010000010100000000" to listOf(
            22 to 18
        ),
        "000000001111100100010010001011111110000000" to listOf(
            24 to 11,
            24 to 14,
            24 to 17,
            24 to 20,
            22 to 21
        )
    )
    var inBoulder = false
    var currentSolution: CopyOnWriteArrayList<Pair<Int, Int>>? = null

    override fun initialize() {
        on<DungeonEvent.RoomEnter> {
            val room = it.room
            if (room.name != "Boulder") return@on

            inBoulder = true

            val grid = getGridLayout(room)

            solutions[grid]
                ?.map { pair -> room.fromComp(pair.first, pair.second)!! }
                ?.let { currentSolution = CopyOnWriteArrayList(it) }
        }

        on<DungeonEvent.RoomLeave> {
            if (!inBoulder) return@on
            inBoulder = false
            currentSolution = null
        }

        on<RenderWorldEvent> {
            if (currentSolution == null) return@on
            currentSolution?.forEach {
                Context.Immediate?.renderBox(
                    it.first.toDouble(), 65.0, it.second.toDouble(),
                    color = OUTLINE_COLOR,
                    phase = true
                )
                Context.Immediate?.renderFilledBox(
                    it.first.toDouble(), 65.0, it.second.toDouble(),
                    color = FILLED_COLOR
                )
            }
        }

        on<BlockPlaceEvent> { event ->
            if (!inBoulder || currentSolution == null) return@on
            val hitResult = event.blockHitResult
            if (hitResult.type == HitResult.Type.MISS) return@on

            val pos = hitResult.blockPos
            val x = pos.x
            val y = pos.y
            val z = pos.z

            WorldUtils.fromBlockTypeOrNull(x, y, z, Blocks.STONE_BUTTON)
                ?: WorldUtils.fromBlockTypeOrNull(x, y, z, Blocks.OAK_WALL_SIGN)
                ?: return@on

            for (data in currentSolution!!) {
                val dist = abs(x - data.first) + abs(z - data.second)
                if (dist != 1) continue
                currentSolution!!.remove(data)
            }
        }
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        currentSolution = null
        inBoulder = false
    }

    private fun getGridLayout(room: DungeonRoom): String {
        // Far left boulder
        val ( x0, y0, z0 ) = listOf(24, 65, 24)

        return buildString {
            // The width of each boulder is 3 blocks
            for (z in 0 until 16 step 3) {
                for (x in 0 until 19 step 3) {
                    val pos = room.fromComp(x0 - x, z0 - z) ?: continue
                    val block = WorldUtils.getBlockState(pos.first, y0, pos.second)
                    if (block == null || block.isAir) append("0")
                    else append("1")
                }
            }
        }
    }
}