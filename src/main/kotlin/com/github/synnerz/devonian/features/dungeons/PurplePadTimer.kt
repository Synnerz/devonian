package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.devonian.api.events.ChatEvent
import com.github.synnerz.devonian.api.events.RenderOverlayEvent
import com.github.synnerz.devonian.api.events.ServerTickEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.hud.texthud.TextHudFeature

object PurplePadTimer : TextHudFeature(
    "purplePadTimer",
    "",
    Categories.DUNGEONS,
    "catacombs",
    subcategory = "HUD",
) {
    private val stormRegex = "^\\[BOSS] Storm: (ENERGY HEED MY CALL|THUNDER LET ME BE YOUR CATALYST!)$".toRegex()
    private var startedAt = 0

    override fun initialize() {
        on<ChatEvent> { event ->
            if (event.matches(stormRegex) == null) return@on
            startedAt = 96
        }

        on<ServerTickEvent> {
            if (startedAt == 0) return@on
            startedAt--.coerceAtLeast(0)
        }

        on<RenderOverlayEvent> {
            if (startedAt <= 0) return@on
            val seconds = "%.2fs".format(startedAt * 0.05)

            setLine("&d$seconds")
            draw(it.ctx)
        }
    }

    override fun getEditText(): List<String> = listOf("&d4.8s")
}