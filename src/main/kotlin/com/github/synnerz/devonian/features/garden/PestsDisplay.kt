package com.github.synnerz.devonian.features.garden

import com.github.synnerz.devonian.events.RenderOverlayEvent
import com.github.synnerz.devonian.events.TabUpdateEvent
import com.github.synnerz.devonian.events.WorldChangeEvent
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.hud.HudManager
import com.github.synnerz.devonian.utils.render.Render2D

object PestsDisplay : Feature("pestsDisplay", "garden") {
    private val pestsAliveRegex = "^ Alive: ([\\d,.]+)$".toRegex()
    private val infestedPlotsRegex = "^ Plots: ([\\d, ]+)$".toRegex()
    private val bonusFortuneRegex = "^ Bonus: ([\\w+☘]+) ?([\\dms ]+)?$".toRegex()
    private val currentSprayRegex = "^ Spray: (\\w+) ?\\(?([\\d,.ms ]+)?\\)?$".toRegex()
    private val hud = HudManager.createHud("PestsDisplay", "&4&lPests Display\n&cAlive&f: &c&l0\n&cInfested Plots&f: &c&lNone\n&eSpray&f: &bNone\n&aBonus&f: &6INACTIVE")
    private var pestsAlive = "0"
    private var infestedPlots = "None"
    private var bonusFortune = "INACTIVE"
    private var currentSpray = "None"

    override fun initialize() {
        on<TabUpdateEvent> { event ->
            val pestsAliveMatch = event.matches(pestsAliveRegex)
            if (pestsAliveMatch != null) {
                pestsAlive = pestsAliveMatch[0]
                return@on
            }

            val infestedPlotMatch = event.matches(infestedPlotsRegex)
            if (infestedPlotMatch != null) {
                infestedPlots = infestedPlotMatch[0]
                return@on
            }

            val bonusFortuneMatch = event.matches(bonusFortuneRegex)
            if (bonusFortuneMatch != null) {
                bonusFortune = bonusFortuneMatch[0]
                return@on
            }

            val currentSprayMatch = event.matches(currentSprayRegex) ?: return@on

            currentSpray = currentSprayMatch[0]
        }

        on<WorldChangeEvent> {
            pestsAlive = "0"
            infestedPlots = "None"
            bonusFortune = "INACTIVE"
            currentSpray = "None"
        }

        on<RenderOverlayEvent> { event ->
            Render2D.drawStringNW(
                event.ctx,
                "&4&lPests Display\n&cAlive&f: &c&l$pestsAlive\n&cInfested Plots&f: &c&l$infestedPlots\n&eSpray&f: &b$currentSpray\n&aBonus&f: &6$bonusFortune",
                hud.x, hud.y, hud.scale
            )
        }
    }
}