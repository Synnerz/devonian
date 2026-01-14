package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.devonian.api.dungeon.Stages
import com.github.synnerz.devonian.api.events.ChatEvent
import com.github.synnerz.devonian.api.events.ClientThreadServerTickEvent
import com.github.synnerz.devonian.api.events.EventBus
import com.github.synnerz.devonian.api.events.RenderOverlayEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.hud.texthud.TextHudFeature
import com.github.synnerz.devonian.utils.BasicState

object FireFreezeTimer : TextHudFeature(
    "fireFreezeTimer",
    "",
    Categories.DUNGEONS,
    "catacombs",
    subcategory = "HUD"
) {
    override fun createRequirements(): List<BasicState<Boolean>?> {
        return super.createRequirements() + listOf(Stages.F3.isActiveState)
    }

    private var startedAt = -1

    override fun initialize() {
        on<ChatEvent> { event ->
            if (event.message != "[BOSS] The Professor: Oh? You found my Guardians' one weakness?") return@on
            startedAt = EventBus.serverTicks() + 110
        }

        on<ClientThreadServerTickEvent> {
            if (startedAt == -1) return@on
            val time = (startedAt - EventBus.serverTicks()) * 0.05
            val seconds = "%.2fs".format(time)

            setLine("&bFF&f: &a$seconds")

            if (time <= 0) startedAt = -1
        }

        on<RenderOverlayEvent> {
            if (startedAt == -1) return@on

            draw(it.ctx)
        }
    }

    override fun getEditText(): List<String> = listOf("&bFF&f: &a5.00s")

    override fun onWorldChange(event: WorldChangeEvent) {
        startedAt = -1
    }
}