package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.devonian.api.ChatUtils
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.features.Feature

object PrinceKilled : Feature(
    "princeKilled",
    "Announces whenever you killed a prince",
    "Dungeons",
    "catacombs"
) {
    private var messageSent = false

    fun sendMessage() {
        if (!isEnabled()) return
        if (messageSent) return
        ChatUtils.command("pc Prince Killed!")
        messageSent = true
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        messageSent = false
    }
}