package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.devonian.api.Scheduler
import com.github.synnerz.devonian.api.dungeon.Stages
import com.github.synnerz.devonian.api.events.BlockUpdateEvent
import com.github.synnerz.devonian.api.events.ClientThreadServerTickEvent
import com.github.synnerz.devonian.api.events.MultiBlockUpdateEvent
import com.github.synnerz.devonian.api.events.RenderOverlayEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.hud.texthud.TextHudFeature
import com.github.synnerz.devonian.utils.BasicState
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState

object SpiritKillCounter : TextHudFeature(
    "spiritKillCounter",
    "Displays all the current spirit kills",
    Categories.DUNGEONS,
    "catacombs",
    subcategory = "HUD",
    searchTags = setOf("f4", "m4"),
) {
    override fun createRequirements(): List<BasicState<Boolean>?> {
        return super.createRequirements() + listOf(Stages.F4.isActiveState)
    }

    private var kills = 0
        set(value) {
            field = value.coerceIn(0, 164)
        }

    override fun initialize() {
        on<BlockUpdateEvent> { event -> onBlockUpdate(event.blockPos, event.blockState) }
        on<MultiBlockUpdateEvent> { event -> event.forEach(::onBlockUpdate) }

        on<ClientThreadServerTickEvent> {
            val amount = (kills / 5.4).toInt()
            setLine("&dkills&f: ${colorForNumber(amount, 30)}$amount&f/&a30")
        }

        on<RenderOverlayEvent> {
            draw(it.ctx)
        }
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        kills = 0
    }

    fun colorForNumber(num: Int, max: Int) = when {
        num >= max * 0.75 -> "§a"
        num >= max * 0.50 -> "§e"
        num >= max * 0.25 -> "§c"
        else -> "§4"
    }

    fun onBlockUpdate(blockPos: BlockPos, blockState: BlockState) {
        if (blockPos.y != 77) return
        if (blockState.block != Blocks.SEA_LANTERN) return
        if (blockPos.x == 7 && blockPos.z == 34) {
            kills = 164
            Scheduler.scheduleServerTask(20) { kills = 0 }
            return
        }

        kills++
    }

    override fun getEditText(): List<String> = listOf("&dkills&f: &42&f/&a30")
}