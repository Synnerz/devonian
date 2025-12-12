package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.devonian.api.ChatUtils
import com.github.synnerz.devonian.api.dungeon.DungeonEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature

object MimicKilled : Feature(
    "mimicKilled",
    "Whenever a mimic is killed it will send a party message.",
    Categories.DUNGEONS,
    "catacombs",
    subcategory = "QOL",
) {
    private var messageSent = false

    override fun initialize() {
        on<DungeonEvent.MimicKilled> {
            if (messageSent) return@on
            ChatUtils.command("pc Mimic Killed!")
            messageSent = true
        }
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        messageSent = false
    }
}