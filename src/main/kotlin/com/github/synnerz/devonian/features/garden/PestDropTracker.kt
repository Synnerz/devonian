package com.github.synnerz.devonian.features.garden

import com.github.synnerz.devonian.api.ChatUtils
import com.github.synnerz.devonian.api.events.ClientThreadServerTickEvent
import com.github.synnerz.devonian.api.events.RenderOverlayEvent
import com.github.synnerz.devonian.api.garden.GardenEvents
import com.github.synnerz.devonian.commands.DevonianCommand
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.hud.texthud.TextHudFeature
import com.github.synnerz.devonian.utils.PersistentJsonClass

object PestDropTracker : TextHudFeature(
    "pestDropTracker",
    "Tracks the pests drops you've gotten and displays them as §7x§a§l1§a§l0§r §rfor enchanted and §7x§9§l1§9§l0§r §rfor compacted form (/dv rspestdroptracker to reset)",
    Categories.GARDEN,
    "garden"
) {
    private val loader = object : PersistentJsonClass<PestDropsData>("devonian/pestdropdata.json", PestDropsData::class.java) {
        override fun onLoadDefault() {
            data = PestDropsData()
        }
    }

    data class PestDropsData(
        var melonSlice: Int = 0,
        var cocoaBeans: Int = 0,
        var netherWart: Int = 0,
        var carrot: Int = 0,
        var potato: Int = 0,
        var cactusGreen: Int = 0,
        var sugar: Int = 0,
        var wheat: Int = 0,
        var pumpkin: Int = 0,
        var redMushroom: Int = 0,
        var brownMushroom: Int = 0,
        var sunflower: Int = 0,
        var moonflower: Int = 0,
        var wildRose: Int = 0,
        var compactMelon: Int = 0,
        var compactRedMushroom: Int = 0,
        var compactBrownMushroom: Int = 0,
        var compactPupmkin: Int = 0,
        var compactCookie: Int = 0,
        var compactSugarCane: Int = 0,
        var compactCactus: Int = 0,
        var compactPotato: Int = 0,
        var compactWheat: Int = 0,
        var compactCarrot: Int = 0,
        var compactNetherWart: Int = 0,
        var compactedSunflower: Int = 0,
        var compactedMoonflower: Int = 0,
        var compactedWildRose: Int = 0,
    )

    override fun initialize() {
        loader.load()
        DevonianCommand.command.subcommand("rspestdroptracker") { _, args ->
            loader.data?.apply {
                melonSlice = 0
                cocoaBeans = 0
                netherWart = 0
                carrot = 0
                potato = 0
                cactusGreen = 0
                sugar = 0
                wheat = 0
                pumpkin = 0
                redMushroom = 0
                brownMushroom = 0
                sunflower = 0
                moonflower = 0
                wildRose = 0
                compactMelon = 0
                compactRedMushroom = 0
                compactBrownMushroom = 0
                compactPupmkin = 0
                compactCookie = 0
                compactSugarCane = 0
                compactCactus = 0
                compactPotato = 0
                compactWheat = 0
                compactCarrot = 0
                compactNetherWart = 0
                compactedSunflower = 0
                compactedMoonflower = 0
                compactedWildRose = 0
                ChatUtils.sendMessage("&aSuccessfully reset &bPestDropTracker&a data", true)
            }
            1
        }

        on<GardenEvents.PestDrop> { event ->
            loader.data?.apply {
                if (event.isRare) {
                    when (event.name.trim()) {
                        "Compacted Wild Rose" -> compactedWildRose += event.amount
                        "Enchanted Brown Mushroom Block" -> compactBrownMushroom += event.amount
                        "Enchanted Red Mushroom Block" -> compactRedMushroom += event.amount
                        "Enchanted Cookie" -> compactCookie += event.amount
                        "Enchanted Melon" -> compactMelon += event.amount
                        "Enchanted Baked Potato" -> compactPotato += event.amount
                        // these below are untested
                        "Enchanted Golden Carrot" -> compactCarrot += event.amount
                        "Enchanted Polished Pumpkin" -> compactPupmkin += event.amount
                        "Enchanted Sugar Cane" -> compactSugarCane += event.amount
                        "Enchanted Cactus" -> compactCactus += event.amount
                        "Enchanted Hay Bale" -> compactWheat += event.amount
                        "Compacted Moonflower" -> compactedMoonflower += event.amount
                        "Compacted Sunflower" -> compactedSunflower += event.amount
                        "Mutant Nether Wart" -> compactNetherWart += event.amount
                    }
                    return@apply
                }

                when (event.name.trim()) {
                    "Enchanted Nether Wart" -> netherWart += event.amount
                    "Enchanted Carrot" -> carrot += event.amount
                    "Enchanted Potato" -> potato += event.amount
                    "Enchanted Wheat" -> wheat += event.amount
                    "Enchanted Cactus Green" -> cactusGreen += event.amount
                    "Enchanted Sugar" -> sugar += event.amount
                    "Enchanted Cocoa Bean" -> cocoaBeans += event.amount
                    "Enchanted Pumpkin" -> pumpkin += event.amount
                    "Enchanted Red Mushroom" -> redMushroom += event.amount
                    "Enchanted Brown Mushroom" -> brownMushroom += event.amount
                    "Enchanted Melon Slice" -> melonSlice += event.amount
                    "Enchanted Sunflower" -> sunflower += event.amount
                    "Enchanted Moonflower" -> moonflower += event.amount
                    "Enchanted Wild Rose" -> wildRose += event.amount
                }
            }
        }

        on<ClientThreadServerTickEvent> {
            // TODO: impl total profit section
            loader.data?.apply {
                setLines(listOf(
                    "&aNether Wart&f: &7x&a&l${netherWart} &7x&9&l${compactNetherWart}",
                    "&aCarrot&f: &7x&a&l${carrot} &7x&9&l${compactCarrot}",
                    "&aPotato&f: &7x&a&l${potato} &7x&9&l${compactPotato}",
                    "&aWheat&f: &7x&a&l${wheat} &7x&9&l${compactWheat}",
                    "&aCactus Green&f: &7x&a&l${cactusGreen} &7x&9&l${compactCactus}",
                    "&aSugar&f: &7x&a&l${sugar} &7x&9&l${compactSugarCane}",
                    "&aCocoa Bean&f: &7x&a&l${cocoaBeans} &7x&9&l${compactCookie}",
                    "&aPumpkin&f: &7x&a&l${pumpkin} &7x&9&l${compactPupmkin}",
                    "&aRed Mushroom&f: &7x&a&l${redMushroom} &7x&9&l${compactRedMushroom}",
                    "&aBrown Mushroom&f: &7x&a&l${brownMushroom} &7x&9&l${compactBrownMushroom}",
                    "&aMelon Slice&f: &7x&a&l${melonSlice} &7x&9&l${compactMelon}",
                    "&aSunflower&f: &7x&a&l${sunflower} &7x&9&l${compactedSunflower}",
                    "&aMoonflower&f: &7x&a&l${moonflower} &7x&9&l${compactedMoonflower}",
                    "&aWild Rose&f: &7x&a&l${wildRose} &7x&9&l${compactedWildRose}",
                ))
            }
        }

        on<RenderOverlayEvent> {
            draw(it.ctx)
        }
    }

    override fun getEditText(): List<String> = listOf(
        "&aNether Wart&f: &7x&a&l50 &7x&9&l10",
        "&aCarrot&f: &7x&a&l50 &7x&9&l10",
        "&aPotato&f: &7x&a&l50 &7x&9&l10",
        "&aWheat&f: &7x&a&l50 &7x&9&l10",
        "&aCactus Green&f: &7x&a&l50 &7x&9&l10",
        "&aSugar&f: &7x&a&l50 &7x&9&l10",
        "&aCocoa Bean&f: &7x&a&l50 &7x&9&l10",
        "&aPumpkin&f: &7x&a&l50 &7x&9&l10",
        "&aRed Mushroom&f: &7x&a&l50 &7x&9&l10",
        "&aBrown Mushroom&f: &7x&a&l50 &7x&9&l10",
        "&aMelon Slice&f: &7x&a&l50 &7x&9&l10",
        "&aSunflower&f: &7x&a&l50 &7x&9&l10",
        "&aMoonflower&f: &7x&a&l50 &7x&9&l10",
        "&aWild Rose&f: &7x&a&l50 &7x&9&l10",
    )
}