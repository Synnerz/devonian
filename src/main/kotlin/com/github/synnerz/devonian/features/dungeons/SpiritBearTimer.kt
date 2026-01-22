package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.devonian.api.dungeon.Stages
import com.github.synnerz.devonian.api.events.*
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.hud.texthud.TextHudFeature
import com.github.synnerz.devonian.utils.BasicState
import net.minecraft.world.level.block.Blocks

object SpiritBearTimer : TextHudFeature(
    "spiritBearTimer",
    "Displays a timer for whenever the Spirit Bear will spawn.",
    Categories.DUNGEONS,
    "catacombs",
    subcategory = "HUD",
    searchTags = setOf("f4", "m4"),
) {
    override fun createRequirements(): List<BasicState<Boolean>?> {
        return super.createRequirements() + listOf(Stages.F4.isActiveState)
    }

    private var startedAt = 0

    override fun initialize() {
        on<MultiBlockUpdateEvent> { event ->

            event.forEach { blockPos, blockState ->
                if (blockPos.x != 7 || blockPos.y != 77 || blockPos.z != 34) return@forEach
                if (blockState.block != Blocks.SEA_LANTERN) return@forEach
                startedAt = EventBus.serverTicks() + 68
            }
        }

        on<ClientThreadServerTickEvent> {
            if (startedAt == 0) return@on

            val time = (startedAt - EventBus.serverTicks()) * 0.05
            val format = "&d%.2fs".format(time)

            setLine(format)

            if (time <= 0) startedAt = 0
        }

        on<RenderOverlayEvent> { event ->
            if (startedAt == 0) return@on

            draw(event.ctx)
        }
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        startedAt = 0
    }

    override fun getEditText(): List<String> = listOf("&d3.4s")
}