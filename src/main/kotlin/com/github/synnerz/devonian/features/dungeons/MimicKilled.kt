package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.devonian.api.ChatUtils
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.features.Feature

object MimicKilled : Feature(
    "mimicKilled",
    "Whenever a mimic is killed it will send a party message.",
    "Dungeons",
    "catacombs"
) {
    private var messageSent = false

    fun sendMessage() {
        if (!isEnabled()) return
        if (messageSent) return
        ChatUtils.command("pc Mimic Killed!")
        messageSent = true
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        messageSent = false
    }
}