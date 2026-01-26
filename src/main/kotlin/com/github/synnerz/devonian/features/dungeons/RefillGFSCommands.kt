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
            var stacks = 0
            var amount = 0
            inv.forEach {
                if (ItemUtils.skyblockId(it) != "ENDER_PEARL") return@forEach
                stacks++
                amount += it.count
            }
            if (amount == 0) stacks = 1
            val remaining = stacks * 16 - amount
            if (remaining > 0) ChatUtils.command("gfs ender pearl $remaining")
            1
        }

        DevonianCommand.command.subcommand("leaps") { _, args ->
            val inv = inventory ?: return@subcommand 0
            var stacks = 0
            var amount = 0
            inv.forEach {
                if (ItemUtils.skyblockId(it) != "SPIRIT_LEAP") return@forEach
                stacks++
                amount += it.count
            }
            if (amount == 0) stacks = 1
            val remaining = stacks * 64 - amount
            if (remaining > 0) ChatUtils.command("gfs spirit leap $remaining")
            1
        }

        DevonianCommand.command.subcommand("superbooms") { _, args ->
            val inv = inventory ?: return@subcommand 0
            var stacks = 0
            var amount = 0
            inv.forEach {
                if (ItemUtils.skyblockId(it) != "SUPERBOOM_TNT") return@forEach
                stacks++
                amount += it.count
            }
            if (amount == 0) stacks = 1
            val remaining = stacks * 64 - amount
            if (remaining > 0) ChatUtils.command("gfs superboom tnt $remaining")
            1
        }
    }
}