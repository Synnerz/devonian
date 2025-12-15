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
            ChatUtils.command("gfs ender pearl ${16 - amount}")
            1
        }

        DevonianCommand.command.subcommand("leaps") { _, args ->
            val inv = inventory ?: return@subcommand 0
            val amount = inv.sumOf { if (ItemUtils.skyblockId(it) == "SPIRIT_LEAP") it.count else 0 }
            ChatUtils.command("gfs spirit leap ${16 - amount}")
            1
        }

        DevonianCommand.command.subcommand("superbooms") { _, args ->
            val inv = inventory ?: return@subcommand 0
            val amount = inv.sumOf { if (ItemUtils.skyblockId(it) == "SUPERBOOM_TNT") it.count else 0 }
            ChatUtils.command("gfs superboom tnt ${64 - amount}")
            1
        }
    }
}