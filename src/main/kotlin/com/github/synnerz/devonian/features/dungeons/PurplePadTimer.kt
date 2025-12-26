package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.devonian.api.dungeon.Stages
import com.github.synnerz.devonian.api.events.ChatEvent
import com.github.synnerz.devonian.api.events.RenderOverlayEvent
import com.github.synnerz.devonian.api.events.ServerTickEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.hud.texthud.TextHudFeature
import com.github.synnerz.devonian.utils.BasicState

object PurplePadTimer : TextHudFeature(
    "purplePadTimer",
    "",
    Categories.DUNGEONS,
    "catacombs",
    subcategory = "HUD",
) {
    override fun createRequirements(): List<BasicState<Boolean>?> {
        return super.createRequirements() + listOf(Stages.Storm.isActiveState)
    }

    private val stormRegex = "^\\[BOSS] Storm: (ENERGY HEED MY CALL!|THUNDER LET ME BE YOUR CATALYST!)$".toRegex()
    private var triggered = false
    private var startedAt = 0

    override fun initialize() {
        on<ChatEvent> { event ->
            if (triggered || event.matches(stormRegex) == null) return@on
            startedAt = 96
            triggered = true
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

    override fun onWorldChange(event: WorldChangeEvent) {
        triggered = false
    }

    override fun getEditText(): List<String> = listOf("&d4.8s")
}