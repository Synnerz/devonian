package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.devonian.api.ChatUtils
import com.github.synnerz.devonian.api.dungeon.DungeonEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.features.Feature

object PrinceKilled : Feature(
    "princeKilled",
    "Announces whenever you killed a prince",
    "Dungeons",
    "catacombs"
) {
    private var messageSent = false

    override fun initialize() {
        on<DungeonEvent.PrinceKilled> {
            if (messageSent) return@on
            ChatUtils.command("pc Prince Killed!")
            messageSent = true
        }
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        messageSent = false
    }
}