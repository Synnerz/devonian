package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.devonian.api.events.ChatEvent
import com.github.synnerz.devonian.api.events.RenderOverlayEvent
import com.github.synnerz.devonian.api.events.ServerTickEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.hud.texthud.TextHudFeature

object LividInvulnerable : TextHudFeature(
    "lividInvulnerable",
    "Displays a timer when livid's invl phase is over",
    Categories.DUNGEONS,
    "catacombs",
    subcategory = "HUD"
) {
    private val lividRegex = "^\\[BOSS] Livid: Welcome, you've arrived right on time. I am Livid, the Master of Shadows\\.$".toRegex()
    private var startedAt = 0

    override fun initialize() {
        on<ChatEvent> { event ->
            if (event.matches(lividRegex) == null) return@on
            startedAt = 395
        }

        on<ServerTickEvent> {
            if (startedAt == 0) return@on
            startedAt--.coerceAtLeast(0)
        }

        on<RenderOverlayEvent> {
            if (startedAt <= 0) return@on
            val seconds = "%.2fs".format(startedAt * 0.05)

            setLine("&e$seconds")
            draw(it.ctx)
        }
    }

    override fun getEditText(): List<String> = listOf("&e19.5s")
}