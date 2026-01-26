package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.devonian.Devonian
import com.github.synnerz.devonian.api.ChatUtils
import com.github.synnerz.devonian.api.ItemUtils
import com.github.synnerz.devonian.commands.DevonianCommand

object RefillGFSCommands {
    val inventory get() = Devonian.minecraft.player?.inventory

    fun initialize() {
        DevonianCommand.command.subcommand("pearls") { _, args ->
            val inv = inventory ?: return@subcommand 0
            var hasMax = false
            var amount = inv.sumOf {
                if (ItemUtils.skyblockId(it) == "ENDER_PEARL") {
                    if (it.count == 16) hasMax = true
                    16 - it.count
                }
                else 0
            }
            if (hasMax) return@subcommand 0
            if (amount <= 0) amount = 16
            ChatUtils.command("gfs ender pearl $amount")
            1
        }

        DevonianCommand.command.subcommand("leaps") { _, args ->
            val inv = inventory ?: return@subcommand 0
            var hasMax = false
            var amount = inv.sumOf {
                if (ItemUtils.skyblockId(it) == "SPIRIT_LEAP") {
                    if (it.count == 16) hasMax = true
                    16 - it.count
                }
                else 0
            }
            if (hasMax) return@subcommand 0
            if (amount <= 0) amount = 16
            ChatUtils.command("gfs spirit leap $amount")
            1
        }

        DevonianCommand.command.subcommand("superbooms") { _, args ->
            val inv = inventory ?: return@subcommand 0
            var hasMax = false
            var amount = inv.sumOf {
                if (ItemUtils.skyblockId(it) == "SUPERBOOM_TNT") {
                    if (it.count == 64) hasMax = true
                    64 - it.count
                }
                else 0
            }
            if (hasMax) return@subcommand 0
            if (amount <= 0) amount = 64
            ChatUtils.command("gfs superboom tnt $amount")
            1
        }
    }
}