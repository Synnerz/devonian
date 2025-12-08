package com.github.synnerz.devonian.commands

import com.github.synnerz.devonian.Devonian
import com.github.synnerz.devonian.api.ChatUtils
import com.github.synnerz.devonian.hud.texthud.TextHud

object DevonianCommand {
    private val commandListeners = mutableListOf<() -> Int>()
    val command = BaseCommand("devonian", listOf("dv")) {
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
    private val fontChange = command.subcommand("font") { _, args ->
        val fontName = args.getOrNull(0) as? String ?: return@subcommand 0
        if (!TextHud.Fonts.containsKey(fontName)) {
            ChatUtils.sendMessage("&cInvalid font name", true)
            return@subcommand 0
        }
        TextHud.setActiveFont(fontName)
        1
    }
        .greedyString("name")
        .suggest("name", *TextHud.Fonts.keys.toTypedArray())

    fun initialize() {
        command.register()
    }

    fun onRun(cb: () -> Int) {
        commandListeners.add(cb)
    }
}