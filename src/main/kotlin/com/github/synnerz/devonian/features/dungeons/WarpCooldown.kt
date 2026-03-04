package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.devonian.api.events.ChatEvent
import com.github.synnerz.devonian.api.events.ClientThreadServerTickEvent
import com.github.synnerz.devonian.api.events.RenderOverlayEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.hud.texthud.TextHudFeature

object WarpCooldown : TextHudFeature(
    "warpCooldown",
    "Displays the cooldown to start a new dungeon run",
    Categories.DUNGEONS,
    subcategory = "HUD"
) {
    private val floorStartRegex = "^-+\\n(?:\\[[^ *]+] )?\\w{1,16} entered (?:MM )?The Catacombs, Floor [VI]+!\\n-+$".toRegex()
    private var started = -1L

    override fun initialize() {
        on<ChatEvent> { event ->
            event.matches(floorStartRegex) ?: return@on
            started = System.currentTimeMillis() + 30_000
        }

        on<ClientThreadServerTickEvent> {
            if (started == -1L) return@on
            val ms = started - System.currentTimeMillis()
            val seconds = ms / 1000.0

            setLine("${colorForNumber(ms, 30_000)}${"%.2fs".format(seconds)}")

            if (ms < 0) started = -1L
        }

        on<RenderOverlayEvent> {
            if (started == -1L) return@on
            draw(it.ctx)
        }
    }

    override fun getEditText(): List<String> = listOf("&c30.00s")

    fun colorForNumber(num: Long, max: Long) = when {
        num >= max * 0.75 -> "§4"
        num >= max * 0.50 -> "§c"
        num >= max * 0.25 -> "§e"
        else -> "§a"
    }
}