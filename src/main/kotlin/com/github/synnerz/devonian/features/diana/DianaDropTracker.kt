package com.github.synnerz.devonian.features.diana

import com.github.synnerz.devonian.Devonian
import com.github.synnerz.devonian.api.events.ChatEvent
import com.github.synnerz.devonian.api.events.RenderOverlayEvent
import com.github.synnerz.devonian.commands.DevonianCommand
import com.github.synnerz.devonian.hud.texthud.TextHudFeature
import com.github.synnerz.devonian.utils.PersistentJsonClass
import java.io.File

object DianaDropTracker : TextHudFeature(
    "dianaDropTracker",
    "Tracks the drops you got during the diana event",
    "Diana",
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

    private val loader = object : PersistentJsonClass<DianaDropData>(statsFile, DianaDropData::class.java) {
        override fun onLoadDefault() {
            data = DianaDropData()
        }
    }

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
        DevonianCommand.command.subcommand("rsdianadroptracker") { _, args ->
            val data = loader.data ?: return@subcommand 1
            data.hiltOfRevelations = 0
            data.mythosFragment = 0
            data.dwarfTurtleShelmet = 0
            data.daedalusStick = 0
            data.griffinFeather = 0
            data.chimeraBook = 0
            data.cretanUrn = 0
            data.washedUpSouvenir = 0
            data.antiqueRemedies = 0
            data.brainFood = 0
            data.shimmeringWool = 0
            data.fatefulStinger = 0
            data.crownOfGreed = 0
            data.minosRelic = 0
            data.crochetTigerPlushie = 0
            data.braidedGriffinFeather = 0
            1
        }
    }

    override fun initialize() {
        on<ChatEvent> { event ->
            val coinsMatch = event.matches(coinsDropCriteria)
            if (coinsMatch != null) {
                val coins = coinsMatch[0].replace(",", "").toInt()

                loader.data?.coins += coins
                return@on
            }

            when (event.matches(rareDropCriteria)?.firstOrNull() ?: return@on) {
                "Hilt of Revelations" -> loader.data?.hiltOfRevelations++
                "Mythos Fragment" -> loader.data?.mythosFragment++
                "Dwarf Turtle Shelmet" -> loader.data?.dwarfTurtleShelmet++
                "Daedalus Stick" -> loader.data?.daedalusStick++
                "Griffin Feather" -> loader.data?.griffinFeather++
                "Enchanted Book (Chimera 1)" -> loader.data?.chimeraBook++
                "Cretan Urn" -> loader.data?.cretanUrn++
                "Washed-up Souvenir" -> loader.data?.washedUpSouvenir++
                "Antique Remedies" -> loader.data?.antiqueRemedies++
                "Brain Food" -> loader.data?.brainFood++
                "Shimmering Wool" -> loader.data?.shimmeringWool++
                "Fateful Stinger" -> loader.data?.fatefulStinger++
                "Crown of Greed" -> loader.data?.crownOfGreed++
                "Minos Relic" -> loader.data?.minosRelic++
                "Crochet Tiger Plushie" -> loader.data?.crochetTigerPlushie++
                "Braided Griffin Feather" -> loader.data?.braidedGriffinFeather++
            }
        }

        on<RenderOverlayEvent> { event ->
            setLines(
                listOf(
                    "&9Griffin Feather&f: &b${loader.data?.griffinFeather ?: 0}",
                    "&9Hilt Of Revelations&f: &b${loader.data?.hiltOfRevelations ?: 0}",
                    "&9Mythos Fragment&f: &b${loader.data?.mythosFragment ?: 0}",
                    "&9Dwarf Turtle Shelmet&f: &b${loader.data?.dwarfTurtleShelmet ?: 0}",
                    "&5Cretan Urn&f: &b${loader.data?.cretanUrn ?: 0}",
                    "&5Antique Remedies&f: &b${loader.data?.antiqueRemedies ?: 0}",
                    "&5Crochet Tiger Plushie&f: &b${loader.data?.crochetTigerPlushie ?: 0}",
                    "&5Braided Griffin Feather&f: &b${loader.data?.braidedGriffinFeather ?: 0}",
                    "&5Brain Food&f: &b${loader.data?.brainFood ?: 0}",
                    "&5Minos Relic&f: &b${loader.data?.minosRelic ?: 0}",
                    "&6Washed-up Souvenir&f: &b${loader.data?.washedUpSouvenir ?: 0}",
                    "&6Daedalus Stick&f: &b${loader.data?.daedalusStick ?: 0}",
                    "&6Crown of Greed&f: &b${loader.data?.crownOfGreed ?: 0}",
                    "&6Fateful Stinger&f: &b${loader.data?.fatefulStinger ?: 0}",
                    "&6Chimera&f: &b${loader.data?.chimeraBook ?: 0}",
                    "&6Shimmering Wool&f: &b${loader.data?.shimmeringWool ?: 0}",
                    "&6Coins&f: &b${loader.data?.coins ?: 0}",
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