package com.github.synnerz.devonian.features.garden

import com.github.synnerz.devonian.api.events.RenderOverlayEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.api.events.garden.GardenEvents
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.hud.texthud.TextHudFeature
import com.github.synnerz.devonian.utils.StringUtils

object VisitorProfitDisplay : TextHudFeature(
    "visitorProfitDisplay",
    "Displays how much profit a visitor will give if accepted",
    Categories.GARDEN,
    "garden"
) {
    private val SETTING_DISPLAY_XP = addSwitch(
        "displayXP",
        true,
        "Displays the Garden and Farming XP that you get from the visitor",
        "Display XP",
    )

    override fun initialize() {
        on<GardenEvents.VisitorItems> { event ->
            val data = event.data
            val profit = data.profit()
            val format = if (profit > 0) "&a" else if (profit < 0) "&c" else "&e"

            setLines(buildList {
                add(data.name.format)
                if (SETTING_DISPLAY_XP.get()) {
                    add("&3FarmingXP&f: &a${StringUtils.shortenNumber(data.farmingXP)}")
                    add("&2GardenXP&f: &a${data.gardenXP}")
                }
                add("&cCopper&f: &a${StringUtils.addCommasTruncate(data.copper)} &7(${StringUtils.shortenNumber(data.copperPrice())})")
                add("&cRequires&f:")
                data.requiredCrops.forEach {
                    val ( crop, amount ) = it
                    add(" ${crop.format} &ax${StringUtils.addCommasTruncate(amount)} &7(${StringUtils.shortenNumber(it.price())})")
                }
                add("&bRare Items&f:")
                data.rareItems.forEach { add(" ${it.lore} &7(${StringUtils.shortenNumber(it.price())})") }
                add("&eProfit&f: $format${StringUtils.addCommasTruncate(profit)} &7(${StringUtils.shortenNumber(profit)})")
            })
        }

        on<GardenEvents.VisitorClose> {
            clearLines()
        }

        on<RenderOverlayEvent> {
            draw(it.ctx)
        }
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        clearLines()
    }

    override fun getEditText(): List<String> = buildList {
        add("&cSpaceman")
        if (SETTING_DISPLAY_XP.get()) {
            add("&3FarmingXP&f: &a100k")
            add("&2GardenXP&f: &a75")
        }
        add("&cCopper&f: &a100 &7(145,400)")
        add("&cRequires&f:")
        add(" &9Enchanted Melon")
        add("&bRare Items&f:")
        add(" &cSpace Helmet &7(550,000,000)")
        add("&eProfit&f: &c-450,000,000 &7(-450M)")
    }
}