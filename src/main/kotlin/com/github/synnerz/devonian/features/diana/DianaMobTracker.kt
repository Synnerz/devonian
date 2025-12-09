package com.github.synnerz.devonian.features.diana

import com.github.synnerz.devonian.Devonian
import com.github.synnerz.devonian.api.events.ChatEvent
import com.github.synnerz.devonian.api.events.RenderOverlayEvent
import com.github.synnerz.devonian.commands.DevonianCommand
import com.github.synnerz.devonian.hud.texthud.TextHudFeature
import com.github.synnerz.devonian.utils.PersistentJsonClass
import java.io.File

object DianaMobTracker : TextHudFeature(
    "dianaMobTracker",
    "Tracks the mobs you killed during the diana event",
    "Diana",
    "hub"
) {
    private val mobCriteria = "^(?:Woah|Yikes|Oi|Danger|Good Grief|Uh oh|Oh)! You dug out a? ?([\\w ]+)!$".toRegex()
    private val mobNames = listOf(
        "Minos Hunter",
        "Gaia Construct",
        "Stranded Nymph",
        "Siamese Lynxes",
        "Cretan Bull",
        "Harpy",
        "Minotaur",
        "Minos Champion",
        "Minos Inquisitor",
        "Sphinx",
        "King Minos"
    )
    private val statsFile = File(
        Devonian.minecraft.gameDirectory,
        "config"
    )
        .resolve("devonian")
        .resolve("dianamobtracker.json")

    private val loader = object : PersistentJsonClass<DianaMobData>(statsFile, DianaMobData::class.java) {
        override fun onLoadDefault() {
            data = DianaMobData()
        }
    }

    data class DianaMobData(
        var minosHunter: Int = 0,
        var gaiaConstruct: Int = 0,
        var strandedNymph: Int = 0,
        var lynxes: Int = 0,
        var cretanBull: Int = 0,
        var harpy: Int = 0,
        var minotaur: Int = 0,
        var champion: Int = 0,
        var inquisitor: Int = 0,
        var sphinx: Int = 0,
        var kingMinos: Int = 0
    )

    init {
        DevonianCommand.command.subcommand("rsdianamobtracker") { _, args ->
            val data = loader.data ?: return@subcommand 1
            data.minosHunter = 0
            data.gaiaConstruct = 0
            data.strandedNymph = 0
            data.lynxes = 0
            data.cretanBull = 0
            data.harpy = 0
            data.minotaur = 0
            data.champion = 0
            data.inquisitor = 0
            data.sphinx = 0
            data.kingMinos = 0
            1
        }
    }

    override fun initialize() {
        on<ChatEvent> { event ->
            val match = event.matches(mobCriteria) ?: return@on
            val dropType = match[0]
            if (!mobNames.contains(dropType)) return@on

            when (dropType) {
                "Minos Hunter" -> loader.data?.minosHunter++
                "Gaia Construct" -> loader.data?.gaiaConstruct++
                "Stranded Nymph" -> loader.data?.strandedNymph++
                "Siamese Lynxes" -> loader.data?.lynxes++
                "Cretan Bull" -> loader.data?.cretanBull++
                "Harpy" -> loader.data?.harpy++
                "Minotaur" -> loader.data?.minotaur++
                "Minos Champion" -> loader.data?.champion++
                "Minos Inquisitor" -> loader.data?.inquisitor++
                "Sphinx" -> loader.data?.sphinx++
                "King Minos" -> loader.data?.kingMinos++
            }
        }

        on<RenderOverlayEvent> { event ->
            setLines(
                listOf(
                    "&aMinos Hunters&f: &b${loader.data?.minosHunter ?: 0}",
                    "&aGaia Construct&f: &b${loader.data?.gaiaConstruct ?: 0}",
                    "&aStranded Nymph&f: &b${loader.data?.strandedNymph ?: 0}",
                    "&aSiamese Lynxes&f: &b${loader.data?.lynxes ?: 0}",
                    "&aCretan Bull&f: &b${loader.data?.cretanBull ?: 0}",
                    "&aHarpy&f: &b${loader.data?.harpy ?: 0}",
                    "&6Minotaur&f: &b${loader.data?.minotaur ?: 0}",
                    "&5Minos Champion&f: &b${loader.data?.champion ?: 0}",
                    "&6Minos Inquisitor&f: &b${loader.data?.inquisitor ?: 0}",
                    "&6Sphinx&f: &b${loader.data?.sphinx ?: 0}",
                    "&6King Minos&f: &b${loader.data?.kingMinos ?: 0}",
                )
            )
            draw(event.ctx)
        }
    }

    override fun getEditText(): List<String> = listOf(
        "&aMinos Hunters&f: &b0",
        "&aGaia Construct&f: &b0",
        "&aStranded Nymph&f: &b0",
        "&aSiamese Lynxes&f: &b0",
        "&aCretan Bull&f: &b0",
        "&aHarpy&f: &b0",
        "&6Minotaur&f: &b0",
        "&5Minos Champion&f: &b0",
        "&6Minos Inquisitor&f: &b0",
        "&6Sphinx&f: &b0",
        "&6King Minos&f: &b0",
    )
}