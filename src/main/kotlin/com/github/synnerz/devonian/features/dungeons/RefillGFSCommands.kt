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
            val amount = inv.sumOf { if (ItemUtils.skyblockId(it) == "ENDER_PEARL") it.count else 0 }
            val total = 16 - amount
            if (total == 0) return@subcommand 1
            ChatUtils.command("gfs ender pearl $total")
            1
        }

        DevonianCommand.command.subcommand("leaps") { _, args ->
            val inv = inventory ?: return@subcommand 0
            val amount = inv.sumOf { if (ItemUtils.skyblockId(it) == "SPIRIT_LEAP") it.count else 0 }
            val total = 16 - amount
            if (total == 0) return@subcommand 1
            ChatUtils.command("gfs spirit leap $total")
            1
        }

        DevonianCommand.command.subcommand("superbooms") { _, args ->
            val inv = inventory ?: return@subcommand 0
            val amount = inv.sumOf { if (ItemUtils.skyblockId(it) == "SUPERBOOM_TNT") it.count else 0 }
            val total = 64 - amount
            if (total == 0) return@subcommand 1
            ChatUtils.command("gfs superboom tnt $total")
            1
        }
    }
}