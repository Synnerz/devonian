package com.github.synnerz.devonian.features.kuudra

import com.github.synnerz.devonian.api.events.ClientThreadServerTickEvent
import com.github.synnerz.devonian.api.events.RenderOverlayEvent
import com.github.synnerz.devonian.api.events.ServerTickEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.api.events.kuudra.KuudraEvents
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.hud.texthud.TextHudFeature
import com.github.synnerz.devonian.utils.StringUtils

object AnkyTickTimer : TextHudFeature(
    "ankyTickTimer",
    "Displays a tick timer of when the \"stats\" update",
    Categories.KUUDRA,
    "kuudra",
) {
    private var started = false
    private var tickTimer = 7

    override fun initialize() {
        on<KuudraEvents.FuelPhaseLeave> {
            started = true
        }

        on<ServerTickEvent> {
            if (!started) return@on
            tickTimer = if (tickTimer <= 0) 20 else tickTimer - 1
        }

        on<ClientThreadServerTickEvent> {
            if (!started) return@on
            setLine("${StringUtils.colorForNumber(tickTimer, 20)}${tickTimer}t")
        }

        on<RenderOverlayEvent> {
            if (!started) return@on
            draw(it.ctx)
        }
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        tickTimer = 7
        started = false
    }

    override fun getEditText(): List<String> = listOf("&220t")
}