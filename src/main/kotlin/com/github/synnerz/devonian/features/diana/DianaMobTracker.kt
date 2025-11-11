package com.github.synnerz.devonian.features.diana

import com.github.synnerz.devonian.api.events.ChatEvent
import com.github.synnerz.devonian.api.events.RenderOverlayEvent
import com.github.synnerz.devonian.commands.DevonianCommand
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.hud.HudManager
import com.github.synnerz.devonian.utils.JsonUtils
import com.github.synnerz.devonian.utils.Render2D
import net.minecraft.client.MinecraftClient
import java.io.File
import java.io.FileWriter

object DianaMobTracker : Feature("dianaMobTracker", "hub") {
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
    private val hud = HudManager.createHud("DianaMobTracker", "&aMinos Hunters&f: &b0" +
            "\n&aGaia Construct&f: &b0" +
            "\n&aStranded Nymph&f: &b0" +
            "\n&aSiamese Lynxes&f: &b0" +
            "\n&aCretan Bull&f: &b0" +
            "\n&aHarpy&f: &b0" +
            "\n&6Minotaur&f: &b0" +
            "\n&5Minos Champion&f: &b0" +
            "\n&6Minos Inquisitor&f: &b0" +
            "\n&6Sphinx&f: &b0" +
            "\n&6King Minos&f: &b0")
    private val statsFile = File(
        MinecraftClient.getInstance().runDirectory,
        "config"
    )
        .resolve("devonian")
        .resolve("dianamobtracker.json")
    private val currentStats =
        if (statsFile.exists() && statsFile.readText().isNotEmpty())
            JsonUtils.gson.fromJson(statsFile.readText(), DianaMobData::class.java)
        else DianaMobData()

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
        if (!statsFile.exists()) {
            statsFile.parentFile.mkdirs()
            statsFile.createNewFile()
        }

        JsonUtils.preSave {
            FileWriter(statsFile).use { JsonUtils.gson.toJson(currentStats, it) }
        }

        DevonianCommand.command.subcommand("rsdianamobtracker") { _, args ->
            currentStats.minosHunter = 0
            currentStats.gaiaConstruct = 0
            currentStats.strandedNymph = 0
            currentStats.lynxes = 0
            currentStats.cretanBull = 0
            currentStats.harpy = 0
            currentStats.minotaur = 0
            currentStats.champion = 0
            currentStats.inquisitor = 0
            currentStats.sphinx = 0
            currentStats.kingMinos = 0
            1
        }
    }

    override fun initialize() {
        on<ChatEvent> { event ->
            val match = event.matches(mobCriteria) ?: return@on
            val dropType = match[0]
            if (!mobNames.contains(dropType)) return@on

            when (dropType) {
                "Minos Hunter" -> currentStats.minosHunter++
                "Gaia Construct" -> currentStats.gaiaConstruct++
                "Stranded Nymph" -> currentStats.strandedNymph++
                "Siamese Lynxes" -> currentStats.lynxes++
                "Cretan Bull" -> currentStats.cretanBull++
                "Harpy" -> currentStats.harpy++
                "Minotaur" -> currentStats.minotaur++
                "Minos Champion" -> currentStats.champion++
                "Minos Inquisitor" -> currentStats.inquisitor++
                "Sphinx" -> currentStats.sphinx++
                "King Minos" -> currentStats.kingMinos++
            }
        }

        on<RenderOverlayEvent> { event ->
            Render2D.drawStringNW(
                event.ctx,
                "&aMinos Hunters&f: &b${currentStats.minosHunter}" +
                    "\n&aGaia Construct&f: &b${currentStats.gaiaConstruct}" +
                    "\n&aStranded Nymph&f: &b${currentStats.strandedNymph}" +
                    "\n&aSiamese Lynxes&f: &b${currentStats.lynxes}" +
                    "\n&aCretan Bull&f: &b${currentStats.cretanBull}" +
                    "\n&aHarpy&f: &b${currentStats.harpy}" +
                    "\n&6Minotaur&f: &b${currentStats.minotaur}" +
                    "\n&5Minos Champion&f: &b${currentStats.champion}" +
                    "\n&6Minos Inquisitor&f: &b${currentStats.inquisitor}" +
                    "\n&6Sphinx&f: &b${currentStats.sphinx}" +
                    "\n&6King Minos&f: &b${currentStats.kingMinos}",
                hud.x, hud.y, hud.scale
            )
        }
    }
}