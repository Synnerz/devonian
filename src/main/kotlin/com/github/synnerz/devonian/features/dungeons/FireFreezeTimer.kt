package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.devonian.api.events.ChatEvent
import com.github.synnerz.devonian.api.events.EventBus
import com.github.synnerz.devonian.api.events.RenderOverlayEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.hud.texthud.TextHudFeature

object FireFreezeTimer : TextHudFeature(
    "fireFreezeTimer",
    "",
    Categories.DUNGEONS,
    "catacombs",
    subcategory = "HUD"
) {
    private val professorRegex = "^\\[BOSS] The Professor: Oh\\? You found my Guardians' one weakness\\?$".toRegex()
    private var startedAt = -1

    override fun initialize() {
        on<ChatEvent> { event ->
            if (event.matches(professorRegex) == null) return@on
            startedAt = EventBus.serverTicks() + 110
        }

        on<RenderOverlayEvent> {
            if (startedAt == -1) return@on
            val time = (startedAt - EventBus.serverTicks()) * 0.05
            val seconds = "%.2fs".format(time)

            setLine("&bFF&f: &a$seconds")
            draw(it.ctx)

            if (time <= 0) startedAt = -1
        }
    }

    override fun getEditText(): List<String> = listOf("&bFF&f: &a5.00s")

    override fun onWorldChange(event: WorldChangeEvent) {
        startedAt = -1
    }
}