package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.devonian.api.Scheduler
import com.github.synnerz.devonian.api.WorldUtils
import com.github.synnerz.devonian.api.dungeon.Dungeons
import com.github.synnerz.devonian.api.dungeon.FloorType
import com.github.synnerz.devonian.api.dungeon.Stages
import com.github.synnerz.devonian.api.events.*
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.utils.BasicState
import com.github.synnerz.devonian.utils.render.Render3DImmediate
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.Blocks

object TerracottaTimer : Feature(
    "terracottaTimer",
    "Shows a timer for whenever the terracotta should spawn",
    Categories.DUNGEONS,
    "catacombs",
    subcategory = "QOL"
) {
    override fun createRequirements(): List<BasicState<Boolean>?> {
        return super.createRequirements() + listOf(Stages.F6.isActiveState)
    }

    private val terraEndRegex = "^\\[BOSS] Sadan: ENOUGH!$".toRegex()
    private val terracottasPos = mutableListOf<TerraBlock>()
    private var done = false

    data class TerraBlock(val pos: BlockPos, val ticks: Int)

    override fun initialize() {
        on<ChatEvent> { event ->
            event.matches(terraEndRegex) ?: return@on
            terracottasPos.clear()
            done = true
        }

        on<BlockUpdateEvent> { event ->
            if (done) return@on
            if (event.blockState.block != Blocks.POTTED_POPPY) return@on
            val pos = event.blockPos
            val block = WorldUtils.getBlockState(pos.x, pos.y, pos.z) ?: return@on
            if (!block.isAir) return@on

            Scheduler.scheduleTask {
                terracottasPos.add(TerraBlock(
                    pos,
                    EventBus.serverTicks() + if (Dungeons.floor == FloorType.F6) 300 else 240)
                )
            }
        }

        on<RenderWorldEvent> { event ->
            if (terracottasPos.isEmpty() || done) return@on

            terracottasPos.removeIf {
                val pos = it.pos
                val time = it.ticks - EventBus.serverTicks()
                val format = colorForNumber(time, if (Dungeons.floor == FloorType.F6) 300 else 240)
                val seconds = "${format}%.2f".format(time * 0.05)

                Render3DImmediate.renderString(
                    seconds,
                    pos.x + 0.5,
                    pos.y + 1.0,
                    pos.z + 0.5,
                    phase = true
                )
                time < 0
            }
        }
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        terracottasPos.clear()
        done = false
    }

    private fun colorForNumber(num: Int, max: Int) = when {
        num >= max * 0.75 -> "§c"
        num >= max * 0.50 -> "§e"
        else -> "§a"
    }
}