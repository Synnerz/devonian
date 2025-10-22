package com.github.synnerz.devonian.commands

import com.github.synnerz.devonian.Devonian
import com.github.synnerz.devonian.api.ChatUtils

object DevonianCommand {
    private val commandListeners = mutableListOf<() -> Int>()
    val command = BaseCommand("devonian") {
        for (cb in commandListeners) {
            val res = cb()
            if (res != 1) return@BaseCommand res
        }
        1
    }
    private val sendCoordsSub = command.subcommand("sendcoords") { _, args ->
        val pos = Devonian.minecraft.player ?: return@subcommand 0
        val x = pos.x.toInt()
        val y = pos.y.toInt()
        val z = pos.z.toInt()
        ChatUtils.say("x: $x, y: $y, z: $z")
        1
    }

    fun initialize() {
        command.register()
    }

    fun onRun(cb: () -> Int) {
        commandListeners.add(cb)
    }
}