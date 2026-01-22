package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.devonian.api.dungeon.Dungeons
import com.github.synnerz.devonian.api.events.ChatEvent
import com.github.synnerz.devonian.api.events.ClientThreadServerTickEvent
import com.github.synnerz.devonian.api.events.RenderOverlayEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.hud.texthud.TextHudFeature
import com.github.synnerz.devonian.utils.BasicState

object LividInvulnerable : TextHudFeature(
    "lividInvulnerable",
    "Displays a timer for livid's invulnerability.",
    Categories.DUNGEONS,
    "catacombs",
    subcategory = "HUD",
    searchTags = setOf("f5", "m5"),
) {
    override fun createRequirements(): List<BasicState<Boolean>?> {
        return super.createRequirements() + listOf(Dungeons.floorState.map { it.floorNum == 5 })
    }

    private val lividRegex = "^\\[BOSS] Livid: Welcome, you've arrived right on time. I am Livid, the Master of Shadows\\.$".toRegex()
    private var startedAt = 0

    override fun initialize() {
        on<ChatEvent> { event ->
            if (event.matches(lividRegex) == null) return@on
            startedAt = 390
        }

        on<ClientThreadServerTickEvent> {
            if (startedAt <= 0) return@on
            startedAt--.coerceAtLeast(0)

            val seconds = "%.2fs".format(startedAt * 0.05)
            setLine("&e$seconds")
        }

        on<RenderOverlayEvent> {
            if (startedAt <= 0) return@on
            draw(it.ctx)
        }
    }

    override fun getEditText(): List<String> = listOf("&e19.5s")
}