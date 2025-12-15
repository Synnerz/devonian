package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.devonian.Devonian
import com.github.synnerz.devonian.api.ChatUtils
import com.github.synnerz.devonian.commands.DevonianCommand

object RefillGFSCommands {
    val inventory get() = Devonian.minecraft.player?.inventory

    fun initialize() {
        DevonianCommand.command.subcommand("pearls") { _, args ->
            inventory ?: return@subcommand 0
            val amount = inventory!!.sumOf { if ((it.customName?.string ?: it.displayName.string).contains("Ender Pearl")) it.count else 0 }
            ChatUtils.command("gfs ender pearl ${16 - amount}")
            1
        }

        DevonianCommand.command.subcommand("leaps") { _, args ->
            inventory ?: return@subcommand 0
            val amount = inventory!!.sumOf { if (it.customName?.string?.contains("Spirit Leap") == true) it.count else 0 }
            ChatUtils.command("gfs spirit leap ${16 - amount}")
            1
        }

        DevonianCommand.command.subcommand("superbooms") { _, args ->
            inventory ?: return@subcommand 0
            val amount = inventory!!.sumOf { if (it.customName?.string?.contains("Superboom TNT") == true) it.count else 0 }
            ChatUtils.command("gfs superboom tnt ${64 - amount}")
            1
        }
    }
}