package com.github.synnerz.devonian.features.diana

import com.github.synnerz.devonian.Devonian
import com.github.synnerz.devonian.api.events.ChatEvent
import com.github.synnerz.devonian.api.events.RenderOverlayEvent
import com.github.synnerz.devonian.commands.DevonianCommand
import com.github.synnerz.devonian.hud.texthud.TextHudFeature
import com.github.synnerz.devonian.config.JsonUtils
import java.io.File
import java.io.FileWriter

object DianaDropTracker : TextHudFeature(
    "dianaDropTracker",
    "Tracks the drops you got during the diana event",
    "diana",
    "hub"
) {
    private val rareDropCriteria =
        "^RARE DROP! (?:You dug out a )?([-() \\w]+)(?: \\(\\+\\d+ . Magic Find\\))?!?$".toRegex()
    private val coinsDropCriteria = "^Wow! You dug out ([\\d,.]+) coins!$".toRegex()
    private val statsFile = File(
        Devonian.minecraft.gameDirectory,
        "config"
    )
        .resolve("devonian")
        .resolve("dianadroptracker.json")
    private var currentStats =
        if (statsFile.exists() && statsFile.readText().isNotEmpty())
            JsonUtils.gson.fromJson(statsFile.readText(), DianaDropData::class.java)
        else DianaDropData()

    data class DianaDropData(
        var hiltOfRevelations: Int = 0,
        var mythosFragment: Int = 0,
        var dwarfTurtleShelmet: Int = 0,
        var daedalusStick: Int = 0,
        var griffinFeather: Int = 0,
        var chimeraBook: Int = 0,
        var cretanUrn: Int = 0,
        var washedUpSouvenir: Int = 0,
        var antiqueRemedies: Int = 0,
        var brainFood: Int = 0,
        var shimmeringWool: Int = 0,
        var fatefulStinger: Int = 0,
        var crownOfGreed: Int = 0,
        var minosRelic: Int = 0,
        var crochetTigerPlushie: Int = 0,
        var braidedGriffinFeather: Int = 0,
        var coins: Int = 0
    )

    init {
        if (!statsFile.exists()) {
            statsFile.parentFile.mkdirs()
            statsFile.createNewFile()
        }

        JsonUtils.preSave {
            FileWriter(statsFile).use { JsonUtils.gson.toJson(currentStats, it) }
        }

        DevonianCommand.command.subcommand("rsdianadroptracker") { _, args ->
            currentStats.hiltOfRevelations = 0
            currentStats.mythosFragment = 0
            currentStats.dwarfTurtleShelmet = 0
            currentStats.daedalusStick = 0
            currentStats.griffinFeather = 0
            currentStats.chimeraBook = 0
            currentStats.cretanUrn = 0
            currentStats.washedUpSouvenir = 0
            currentStats.antiqueRemedies = 0
            currentStats.brainFood = 0
            currentStats.shimmeringWool = 0
            currentStats.fatefulStinger = 0
            currentStats.crownOfGreed = 0
            currentStats.minosRelic = 0
            currentStats.crochetTigerPlushie = 0
            currentStats.braidedGriffinFeather = 0
            1
        }
    }

    override fun initialize() {
        on<ChatEvent> { event ->
            val coinsMatch = event.matches(coinsDropCriteria)
            if (coinsMatch != null) {
                val coins = coinsMatch[0].replace(",", "").toInt()

                currentStats.coins += coins
                return@on
            }

            when (event.matches(rareDropCriteria)?.firstOrNull() ?: return@on) {
                "Hilt of Revelations" -> currentStats.hiltOfRevelations++
                "Mythos Fragment" -> currentStats.mythosFragment++
                "Dwarf Turtle Shelmet" -> currentStats.dwarfTurtleShelmet++
                "Daedalus Stick" -> currentStats.daedalusStick++
                "Griffin Feather" -> currentStats.griffinFeather++
                "Enchanted Book (Chimera 1)" -> currentStats.chimeraBook++
                "Cretan Urn" -> currentStats.cretanUrn++
                "Washed-up Souvenir" -> currentStats.washedUpSouvenir++
                "Antique Remedies" -> currentStats.antiqueRemedies++
                "Brain Food" -> currentStats.brainFood++
                "Shimmering Wool" -> currentStats.shimmeringWool++
                "Fateful Stinger" -> currentStats.fatefulStinger++
                "Crown of Greed" -> currentStats.crownOfGreed++
                "Minos Relic" -> currentStats.minosRelic++
                "Crochet Tiger Plushie" -> currentStats.crochetTigerPlushie++
                "Braided Griffin Feather" -> currentStats.braidedGriffinFeather++
            }
        }

        on<RenderOverlayEvent> { event ->
            setLines(
                listOf(
                    "&9Griffin Feather&f: &b${currentStats.griffinFeather}",
                    "&9Hilt Of Revelations&f: &b${currentStats.hiltOfRevelations}",
                    "&9Mythos Fragment&f: &b${currentStats.mythosFragment}",
                    "&9Dwarf Turtle Shelmet&f: &b${currentStats.dwarfTurtleShelmet}",
                    "&5Cretan Urn&f: &b${currentStats.cretanUrn}",
                    "&5Antique Remedies&f: &b${currentStats.antiqueRemedies}",
                    "&5Crochet Tiger Plushie&f: &b${currentStats.crochetTigerPlushie}",
                    "&5Braided Griffin Feather&f: &b${currentStats.braidedGriffinFeather}",
                    "&5Brain Food&f: &b${currentStats.brainFood}",
                    "&5Minos Relic&f: &b${currentStats.minosRelic}",
                    "&6Washed-up Souvenir&f: &b${currentStats.washedUpSouvenir}",
                    "&6Daedalus Stick&f: &b${currentStats.daedalusStick}",
                    "&6Crown of Greed&f: &b${currentStats.crownOfGreed}",
                    "&6Fateful Stinger&f: &b${currentStats.fatefulStinger}",
                    "&6Chimera&f: &b${currentStats.chimeraBook}",
                    "&6Shimmering Wool&f: &b${currentStats.shimmeringWool}",
                    "&6Coins&f: &b${currentStats.coins}",
                )
            )
            draw(event.ctx)
        }
    }

    override fun getEditText(): List<String> = listOf(
        "&9Griffin Feather&f: &b0",
        "&9Hilt Of Revelations&f: &b0",
        "&9Mythos Fragment&f: &b0",
        "&9Dwarf Turtle Shelmet&f: &b0",
        "&5Cretan Urn&f: &b0",
        "&5Antique Remedies&f: &b0",
        "&5Crochet Tiger Plushie&f: &b0",
        "&5Braided Griffin Feather&f: &b0",
        "&5Brain Food&f: &b0",
        "&5Minos Relic&f: &b0",
        "&6Washed-up Souvenir&f: &b0",
        "&6Daedalus Stick&f: &b0",
        "&6Crown of Greed&f: &b0",
        "&6Fateful Stinger&f: &b0",
        "&6Chimera&f: &b0",
        "&6Shimmering Wool&f: &b0",
        "&6Coins&f: &b0",
    )
}