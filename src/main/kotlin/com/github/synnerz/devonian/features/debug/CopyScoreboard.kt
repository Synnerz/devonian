package com.github.synnerz.devonian.features.debug

import com.github.synnerz.devonian.api.ChatUtils
import com.github.synnerz.devonian.commands.DevonianCommand
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.config.json.JsonDataObject
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.utils.StringUtils.colorCodes
import com.google.gson.JsonArray
import net.minecraft.world.scores.DisplaySlot
import net.minecraft.world.scores.PlayerTeam

object CopyScoreboard : Feature(
    "copyScoreboard",
    "Copy the scoreboard lines into the clipboard. /dv wiots",
    Categories.DEBUG,
    subcategory = "Misc",
) {
    override fun initialize() {
        DevonianCommand.command.subcommand("wiots") { _, _ ->
            val scoreboard = minecraft.level?.scoreboard ?: return@subcommand 1
            val objective = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR) ?: return@subcommand 1
            val scores = scoreboard.listPlayerScores(objective)
            val comps = scores.map {
                val team = scoreboard.getPlayersTeam(it.owner)
                PlayerTeam.formatNameForTeam(team, it.ownerName())
            }

            val obj = JsonDataObject()
            obj.set("lines", JsonArray().also { arr -> comps.forEach { arr.add(it.colorCodes()) } })
            obj.set("lines_", JsonArray().also { arr -> comps.forEach { arr.add(it.string) } })

            minecraft.keyboardHandler.clipboard = obj.toString()
            ChatUtils.sendMessage("&aCopied scoreboard to clipboard")

            0
        }
    }
}