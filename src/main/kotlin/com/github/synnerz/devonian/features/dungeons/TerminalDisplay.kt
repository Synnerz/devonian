package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.devonian.api.dungeon.Stages
import com.github.synnerz.devonian.api.dungeon.TerminalSection
import com.github.synnerz.devonian.api.events.RenderOverlayEvent
import com.github.synnerz.devonian.api.events.TickEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.hud.texthud.TextHudFeature
import com.github.synnerz.devonian.utils.BasicState

object TerminalDisplay : TextHudFeature(
    "terminalDisplay",
    "displays current terminals status (# done, gate, etc)",
    Categories.DUNGEONS,
    "catacombs",
    subcategory = "F7",
) {
    override fun createRequirements(): List<BasicState<Boolean>?> {
        return super.createRequirements() + listOf(Stages.Terminals.isActiveState)
    }

    private val SETTING_SIMPLE = addSwitch(
        "simple",
        true,
        "",
        "Simple Mode",
    )

    private fun colorFor(n: Int, max: Int): String {
        if (n == max) return "&a"
        if (n == 0) return "&c"
        return "&e"
    }

    override fun initialize() {
        on<TickEvent> {
            val cur = Stages.Terminals.activeChild as TerminalSection
            if (SETTING_SIMPLE.get()) {
                val color = if (cur.gateDestroyed) "&a" else "&c"
                setLine("${color}${cur.termsDone + cur.leversDone + (if (cur.deviceDone) 1 else 0)}/${cur.terms + 3}")
            } else {
                setLines(
                    listOf(
                        "${colorFor(cur.termsDone, cur.terms)}Terms: ${cur.termsDone}/${cur.terms}",
                        "${colorFor(cur.leversDone, 2)}Levers: ${cur.leversDone}/2",
                        if (cur.deviceDone) "&aDevice: &l✔" else "&cDevice: &l✘",
                        if (cur.gateDestroyed) "&aGate: &l✔" else "&cGate: &l✘",
                    )
                )
            }
        }

        on<RenderOverlayEvent> { event ->
            draw(event.ctx)
        }
    }

    override fun getEditText(): List<String> {
        if (SETTING_SIMPLE.get()) return listOf("&c2/7")
        return listOf(
            "&eTerms: 3/4",
            "&aLevers: 2/2",
            "&aDevice: &l✔",
            "&cGate: &l✘",
        )
    }
}