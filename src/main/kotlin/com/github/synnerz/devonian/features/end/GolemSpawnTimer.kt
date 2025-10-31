package com.github.synnerz.devonian.features.end

import com.github.synnerz.devonian.api.events.ChatEvent
import com.github.synnerz.devonian.api.events.RenderOverlayEvent
import com.github.synnerz.devonian.api.events.ServerTickEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.features.WorldFeature
import com.github.synnerz.devonian.hud.HudManager
import com.github.synnerz.devonian.utils.Render2D

object GolemSpawnTimer : WorldFeature("golemSpawnTimer", "the end") {
    private val hud = HudManager.createHud("GolemSpawnTimer", "&bGolem In&f: &a20s")
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

        on<RenderOverlayEvent> {
            if (remainingTime == 0) return@on
            val time = ((remainingTime - serverTicks) * 0.05).toInt()

            if (time <= 0) {
                remainingTime = 0
                serverTicks = 0
                return@on
            }

            Render2D.drawString(
                it.ctx,
                "&bGolem In&f: &a${time}s",
                hud.x, hud.y, hud.scale
            )
        }
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        serverTicks = 0
        remainingTime = 0
    }
}