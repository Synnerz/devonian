package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.devonian.api.dungeon.Stages
import com.github.synnerz.devonian.api.events.ChatEvent
import com.github.synnerz.devonian.api.events.ClientThreadServerTickEvent
import com.github.synnerz.devonian.api.events.RenderOverlayEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.hud.texthud.TextHudFeature
import com.github.synnerz.devonian.utils.BasicState
import com.github.synnerz.devonian.utils.StringUtils

object GoldorFrenzyTimer : TextHudFeature(
    "goldorFrenzyTimer",
    "timer until goldor damage tick",
    Categories.DUNGEONS,
    "catacombs",
    subcategory = "F7",
) {
    override fun createRequirements(): List<BasicState<Boolean>?> {
        return super.createRequirements() + listOf(Stages.F7.isActiveState, Stages.Terminals.hasFinishedState.map(
            Boolean::not))
    }

    private var preGoldor = false
    private var inGoldor = false
    private var until = 0
    private var preGoldorTicks = 0

    override fun initialize() {
        on<ChatEvent> { event ->
            when (event.message) {
                "[BOSS] Goldor: Who dares trespass into my domain?" -> {
                    inGoldor = true
                    preGoldor = false
                    until = 60
                    preGoldorTicks = 0
                }
                "[BOSS] Storm: I should have known that I stood no chance." -> {
                    preGoldor = true
                    preGoldorTicks = 100
                }

                "The Core entrance is opening!" -> inGoldor = false
            }
        }

        on<ClientThreadServerTickEvent> {
            if (preGoldor) {
                val time = preGoldorTicks * 0.05
                setLine(
                    "%s%.2f".format(
                        StringUtils.colorForNumber(preGoldorTicks, 100),
                        time
                    )
                )
                if (time < 0) {
                    preGoldor = false
                    preGoldorTicks = 0
                }
                return@on
            }
            if (!inGoldor) return@on
            until = if (until > 1) until - 1 else 60
            setLine(
                "%s%.2f".format(
                    StringUtils.colorForNumber(until, 60),
                    until * 0.05
                )
            )
        }

        on<RenderOverlayEvent> { event ->
            if (preGoldor) return@on draw(event.ctx)
            if (!inGoldor) return@on
            draw(event.ctx)
        }
    }

    override fun getEditText(): List<String> = listOf("${StringUtils.colorForNumber(1.95, 3.0)}1.95")

    override fun onWorldChange(event: WorldChangeEvent) {
        inGoldor = false
        preGoldor = false
        until = 0
        preGoldorTicks = 0
    }
}