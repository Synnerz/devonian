package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.devonian.api.dungeon.Dungeons
import com.github.synnerz.devonian.api.dungeon.FloorType
import com.github.synnerz.devonian.api.events.ChatEvent
import com.github.synnerz.devonian.api.events.EventBus
import com.github.synnerz.devonian.api.events.RenderOverlayEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.hud.texthud.TextHudFeature

object RelicTimer : TextHudFeature(
    "relicTimer",
    "Displays a timer that tells you when the relics are going to spawn in M7",
    Categories.DUNGEONS,
    "catacombs",
    subcategory = "HUD"
) {
    private const val SPAWN_TICKS = 43
    private val necronRegex = "^\\[BOSS] Necron: All this, for nothing\\.\\.\\.$".toRegex()
    private var startedAt = -1

    override fun initialize() {
        on<ChatEvent> { event ->
            if (event.matches(necronRegex) == null) return@on

            startedAt = EventBus.serverTicks() + SPAWN_TICKS
        }

        on<RenderOverlayEvent> {
            if (!Dungeons.inBoss.value || Dungeons.floor != FloorType.M7 || startedAt == -1) return@on

            val elapsedTime = (startedAt - EventBus.serverTicks()) * 0.05
            val time = "%.2fs".format(elapsedTime)

            setLine("&a${time}")
            draw(it.ctx)

            if (elapsedTime <= 0)
                startedAt = -1
        }
    }

    override fun getEditText(): List<String> = listOf("&a2.15s")

    override fun onWorldChange(event: WorldChangeEvent) {
        startedAt = -1
    }
}