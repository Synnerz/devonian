package com.github.synnerz.devonian.features.end

import com.github.synnerz.devonian.api.events.*
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.hud.texthud.TextHudFeature

object GolemSpawnTimer : TextHudFeature(
    "golemSpawnTimer",
    "Displays a timer for 20 seconds whenever the Golem has hit stage 5.",
    Categories.END,
    "the end",
) {
    private val golemSpawnRegex = "^The ground begins to shake as an End Stone Protector rises from below!$".toRegex()
    var remainingTime = 0

    override fun initialize() {
        on<ChatEvent> { event ->
            event.matches(golemSpawnRegex) ?: return@on

            remainingTime = EventBus.serverTicks() + 400
        }

        on<ClientThreadServerTickEvent> {
            if (remainingTime == 0) return@on
            val time = ((remainingTime - EventBus.serverTicks()) * 0.05).toInt()

            setLine("&bGolem In&f: &a${time}s")

            if (time <= 0) remainingTime = 0
        }

        on<RenderOverlayEvent> { event ->
            if (remainingTime == 0) return@on

            draw(event.ctx)
        }
    }

    override fun getEditText(): List<String> = listOf("&bGolem In&f: &a20s")

    override fun onWorldChange(event: WorldChangeEvent) {
        remainingTime = 0
    }
}