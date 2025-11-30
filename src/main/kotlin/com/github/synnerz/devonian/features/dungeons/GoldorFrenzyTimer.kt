package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.devonian.api.events.ChatEvent
import com.github.synnerz.devonian.api.events.RenderOverlayEvent
import com.github.synnerz.devonian.api.events.ServerTickEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.hud.texthud.TextHudFeature
import com.github.synnerz.devonian.utils.StringUtils

object GoldorFrenzyTimer : TextHudFeature(
    "goldorFrenzyTimer",
    "timer until goldor damage tick",
    "Dungeons",
    "catacombs"
) {
    private var inGoldor = false
    private var until = 0

    override fun initialize() {
        on<ChatEvent> { event ->
            when (event.message) {
                "[BOSS] Goldor: Who dares trespass into my domain?" -> {
                    inGoldor = true
                    until = 60
                }

                "The Core entrance is opening!" -> inGoldor = false
            }
        }

        on<ServerTickEvent> {
            until = if (until > 1) until - 1 else 60
        }

        on<RenderOverlayEvent> { event ->
            if (!inGoldor) return@on
            setLine(
                "%s%.2f".format(
                    StringUtils.colorForNumber(until.toDouble(), 60.0),
                    until * 0.05
                )
            )
            draw(event.ctx)
        }
    }

    override fun getEditText(): List<String> = listOf("${StringUtils.colorForNumber(1.95, 3.0)}1.95")

    override fun onWorldChange(event: WorldChangeEvent) {
        inGoldor = false
        until = 0
    }
}