package com.github.synnerz.devonian.features.dungeons.solvers

import com.github.synnerz.barrl.Context
import com.github.synnerz.devonian.api.Scheduler
import com.github.synnerz.devonian.api.WorldUtils
import com.github.synnerz.devonian.api.dungeon.Dungeons
import com.github.synnerz.devonian.api.dungeon.Stages
import com.github.synnerz.devonian.api.events.*
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.utils.BasicState
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.Blocks
import java.awt.Color
import java.util.concurrent.CopyOnWriteArraySet
import kotlin.math.abs

object SharpShooterSolver : Feature(
    "sharpShooterSolver",
    "Highlights the block you've already hit in section 4 goldor phase f7 (i4 helper)",
    Categories.DUNGEONS,
    "catacombs",
    subcategory = "Solvers"
) {
    override fun createRequirements(): List<BasicState<Boolean>?> {
        return super.createRequirements() + listOf(Stages.Terminals.isActiveState)
    }

    private val deviceCompletedRegex = "^(\\w{1,16}) completed a device! \\(\\d/7\\)$".toRegex()
    private val emeraldPositions = listOf(
        SolverPosition(68, 130, 50),
        SolverPosition(66, 130, 50),
        SolverPosition(64, 130, 50),
        SolverPosition(68, 128, 50),
        SolverPosition(66, 128, 50),
        SolverPosition(64, 128, 50),
        SolverPosition(68, 126, 50),
        SolverPosition(66, 126, 50),
        SolverPosition(64, 126, 50),
    )
    private val basePosition = SolverPosition(63, 127, 35)
    private val whitelist = CopyOnWriteArraySet<SolverPosition>()

    private data class SolverPosition(val x: Int, val y: Int, val z: Int, var hit: Boolean = false)

    override fun initialize() {
        on<ChatEvent> { event ->
            event.matches(deviceCompletedRegex)?.let {
                val playerName = minecraft.player?.name?.string ?: return@on
                if (it[0] != playerName) return@on
                if (whitelist.size >= 8) whitelist.clear()
                return@on
            }
        }

        on<BlockUpdateEvent> { event ->
            if (!Dungeons.inBoss.value || Dungeons.floor.floorNum != 7) return@on

            val bp = event.blockPos
            if (event.blockState.block != Blocks.EMERALD_BLOCK) {
                if (event.blockState.block == Blocks.BLUE_TERRACOTTA)
                    onBlueTerracotta(bp)
                return@on
            }
            Scheduler.scheduleTask { onEmeraldBlock(bp) }
        }

        on<MultiBlockUpdateEvent> { event ->
            if (!Dungeons.inBoss.value || Dungeons.floor.floorNum != 7) return@on

            event.forEach { blockPos, blockState ->
                if (blockState.block != Blocks.EMERALD_BLOCK) {
                    if (blockState.block == Blocks.BLUE_TERRACOTTA)
                        onBlueTerracotta(blockPos)
                    return@forEach
                }
                Scheduler.scheduleTask { onEmeraldBlock(blockPos) }
            }
        }

        on<RenderWorldEvent> { event ->
            if (!Dungeons.inBoss.value || Dungeons.floor.floorNum != 7) return@on
            if (whitelist.isEmpty()) return@on

            whitelist.forEach {
                Context.Immediate?.renderFilledBox(
                    it.x.toDouble(), it.y.toDouble(), it.z.toDouble(),
                    if (it.hit) Color(255, 0, 0, 80) else Color(0, 255, 0, 80),
                    false
                )
            }

            if (whitelist.size >= 9) whitelist.clear()
        }
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        whitelist.clear()
    }

    private fun onEmeraldBlock(bp: BlockPos) {
        val blockAt = WorldUtils.getBlockState(bp.x, bp.y, bp.z)
        var ox = 0
        if (blockAt?.block != Blocks.EMERALD_BLOCK) {
            val blockAt2 = WorldUtils.getBlockState(bp.x + 1, bp.y, bp.z)
            ox = if (blockAt2?.block == Blocks.EMERALD_BLOCK) 1
                else -1
        }
        emeraldPositions.find { it.x == bp.x + ox && it.y == bp.y && it.z == bp.z } ?: return

        val pos = SolverPosition(bp.x + ox, bp.y, bp.z)
        if (whitelist.contains(pos)) return

        val player = minecraft.player ?: return
        val x1 = player.x.toInt()
        val y1 = player.y.toInt()
        val z1 = player.z.toInt()
        val dist = abs(basePosition.x - x1) + abs(basePosition.y - y1) + abs(basePosition.z - z1)
        if (dist > 2) return

        whitelist.add(pos)
    }

    private fun onBlueTerracotta(blockPos: BlockPos) {
        val cachedData = whitelist.find { it.x == blockPos.x && it.y == blockPos.y && it.z == blockPos.z } ?: return
        cachedData.hit = true
    }
}