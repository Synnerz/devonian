package com.github.synnerz.devonian.features.debug

import com.github.synnerz.devonian.api.ChatUtils
import com.github.synnerz.devonian.commands.DevonianCommand
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.config.json.JsonDataObject
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.utils.StringUtils.colorCodes
import com.google.gson.JsonArray
import net.minecraft.client.multiplayer.PlayerInfo
import net.minecraft.network.chat.Component
import net.minecraft.world.level.GameType
import net.minecraft.world.scores.PlayerTeam

object CopyTablist : Feature(
    "copyTablist",
    "Copy the tablist into the clipboard. /dv wittl",
    Categories.DEBUG,
    subcategory = "Misc",
) {
    private val PLAYER_COMPARATOR = Comparator
        .comparingInt<PlayerInfo> { -it.tabListOrder }
        .thenComparingInt { if (it.gameMode == GameType.SPECTATOR) 1 else 0 }
        .thenComparing { it.team?.name ?: "" }
        .thenComparing({ it.profile.name }, String.CASE_INSENSITIVE_ORDER)

    override fun initialize() {
        DevonianCommand.command.subcommand("wittl") { _, _ ->
            val players = minecraft.player?.connection?.listedOnlinePlayers?.stream()
                ?.sorted(PLAYER_COMPARATOR)
                ?.limit(80L)
                ?.toList() ?: return@subcommand 1

            val comps = players.map {
                it.tabListDisplayName ?:
                PlayerTeam.formatNameForTeam(it.team, Component.literal(it.profile.name))
            }

            val obj = JsonDataObject()
            obj.set("names", JsonArray().also { arr -> comps.forEach { arr.add(it.colorCodes()) }})
            obj.set("names_", JsonArray().also { arr -> comps.forEach { arr.add(it.string) }})

            minecraft.keyboardHandler.clipboard = obj.toString()
            ChatUtils.sendMessage("&aCopied tablist to clipboard")

            0
        }
    }
}