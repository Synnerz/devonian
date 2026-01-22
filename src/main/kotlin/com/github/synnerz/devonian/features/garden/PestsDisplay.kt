package com.github.synnerz.devonian.features.garden

import com.github.synnerz.devonian.api.events.RenderOverlayEvent
import com.github.synnerz.devonian.api.events.TabUpdateEvent
import com.github.synnerz.devonian.api.events.TickEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.hud.texthud.TextHudFeature

object PestsDisplay : TextHudFeature(
    "pestsDisplay",
    "Displays all your Garden's current Pests stats.",
    Categories.GARDEN,
    "garden",
) {
    private val pestsAliveRegex = "^ Alive: ([\\d,.]+)$".toRegex()
    private val infestedPlotsRegex = "^ Plots: ([\\d, ]+)$".toRegex()
    private val bonusFortuneRegex = "^ Bonus: ([\\w+☘]+) ?([\\dms ]+)?$".toRegex()
    private val currentSprayRegex = "^ Spray: (\\w+) ?\\(?([\\d,.ms ]+)?\\)?$".toRegex()
    private val currentCooldownRegex = "^ Cooldown: ([\\dms ]+|READY|MAX PESTS)$".toRegex()
    private var pestsAlive = "0"
    private var infestedPlots = "None"
    private var bonusFortune = "INACTIVE"
    private var currentSpray = "None"
    private var cooldown = "0s"

    override fun initialize() {
        on<TabUpdateEvent> { event ->
            val pestsAliveMatch = event.matches(pestsAliveRegex)
            if (pestsAliveMatch != null) {
                pestsAlive = pestsAliveMatch[0]
                val amount = pestsAlive.toIntOrNull() ?: return@on
                if (amount == 0) infestedPlots = "None"
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

            event.matches(currentCooldownRegex)?.let {
                val cd = it[0]
                val format = if (cd == "READY") "&a" else if (cd == "MAX PESTS") "&c" else "&e"
                cooldown = "$format$cd"
                return@on
            }

            val currentSprayMatch = event.matches(currentSprayRegex) ?: return@on

            currentSpray = currentSprayMatch[0]
        }

        on<TickEvent> {
            setLines(
                listOf(
                    "&4&lPests Display",
                    "&cAlive&f: &c&l$pestsAlive",
                    "&cInfested Plots&f: &c&l$infestedPlots",
                    "&eSpray&f: &b$currentSpray",
                    "&aBonus&f: &6$bonusFortune",
                    "&fCooldown&f: &e$cooldown"
                )
            )
        }

        on<RenderOverlayEvent> { event ->
            draw(event.ctx)
        }
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        pestsAlive = "0"
        infestedPlots = "None"
        bonusFortune = "INACTIVE"
        currentSpray = "None"
        cooldown = "0s"
    }

    override fun getEditText(): List<String> = listOf(
        "&4&lPests Display",
        "&cAlive&f: &c&l0",
        "&cInfested Plots&f: &c&lNone",
        "&eSpray&f: &bNone",
        "&aBonus&f: &6INACTIVE",
        "&fCooldown&f: &e0s"
    )
}