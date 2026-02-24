package com.github.synnerz.devonian.features.dungeons.m7

import com.github.synnerz.devonian.api.dungeon.Dungeons
import com.github.synnerz.devonian.api.dungeon.FloorType
import com.github.synnerz.devonian.api.dungeon.Stages
import com.github.synnerz.devonian.api.events.*
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.hud.texthud.TextHudFeature
import com.github.synnerz.devonian.utils.BasicState

object RelicTimer : TextHudFeature(
    "relicTimer",
    "Displays a timer that tells you when the relics are going to spawn in M7.",
    Categories.M7,
    "catacombs",
    subcategory = "HUD",
) {
    override fun createRequirements(): List<BasicState<Boolean>?> {
        return super.createRequirements() + listOf(Stages.F7.isActiveState, Dungeons.floorState.map { it == FloorType.M7 })
    }

    private const val SPAWN_TICKS = 45
    private val necronRegex = "^\\[BOSS] Necron: All this, for nothing\\.\\.\\.$".toRegex()
    private var startedAt = -1

    override fun initialize() {
        on<ChatEvent> { event ->
            if (event.matches(necronRegex) == null) return@on

            startedAt = EventBus.serverTicks() + SPAWN_TICKS
        }

        on<ClientThreadServerTickEvent> {
            if (startedAt == -1) return@on

            val elapsedTime = (startedAt - EventBus.serverTicks()) * 0.05
            val time = "%.2fs".format(elapsedTime)

            setLine("&a${time}")

            if (elapsedTime <= 0)
                startedAt = -1
        }

        on<RenderOverlayEvent> { event ->
            if (startedAt == -1) return@on

            draw(event.ctx)
        }
    }

    override fun getEditText(): List<String> = listOf("&a2.15s")

    override fun onWorldChange(event: WorldChangeEvent) {
        startedAt = -1
    }
}