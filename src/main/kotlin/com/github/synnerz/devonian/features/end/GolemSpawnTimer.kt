package com.github.synnerz.devonian.features.end

import com.github.synnerz.devonian.api.events.ChatEvent
import com.github.synnerz.devonian.api.events.RenderOverlayEvent
import com.github.synnerz.devonian.api.events.ServerTickEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.hud.texthud.TextHudFeature

object GolemSpawnTimer : TextHudFeature(
    "golemSpawnTimer",
    "Displays a timer whenever the Golem has hit stage 5 of 20 seconds (according to wiki)",
    "end",
    "the end"
) {
    private val golemSpawnRegex = "^The ground begins to shake as an End Stone Protector rises from below!$".toRegex()
    var serverTicks = 0
    var remainingTime = 0

    override fun initialize() {
        on<ServerTickEvent> {
            serverTicks++
        }

        on<ChatEvent> { event ->
            event.matches(golemSpawnRegex) ?: return@on

            remainingTime = serverTicks + 400
        }

        on<RenderOverlayEvent> { event ->
            if (remainingTime == 0) return@on
            val time = ((remainingTime - serverTicks) * 0.05).toInt()

            if (time <= 0) {
                remainingTime = 0
                serverTicks = 0
                return@on
            }

            setLine("&bGolem In&f: &a${time}s")
            draw(event.ctx)
        }
    }

    override fun getEditText(): List<String> = listOf("&bGolem In&f: &a20s")

    override fun onWorldChange(event: WorldChangeEvent) {
        serverTicks = 0
        remainingTime = 0
    }
}