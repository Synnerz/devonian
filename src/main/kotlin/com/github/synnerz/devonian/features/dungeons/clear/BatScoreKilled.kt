package com.github.synnerz.devonian.features.dungeons.clear

import com.github.synnerz.devonian.api.ChatUtils
import com.github.synnerz.devonian.api.dungeon.DungeonEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature

object BatScoreKilled : Feature(
    "batScoreKilled",
    "Announces whenever you killed the bat score",
    Categories.DUNGEONS,
    "catacombs",
    subcategory = "QOL",
    searchTags = setOf("party"),
) {
    private var messageSent = false

    override fun initialize() {
        on<DungeonEvent.BatScoreKilled> {
            if (messageSent) return@on
            ChatUtils.command("pc Bat Killed!")
            messageSent = true
        }
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        messageSent = false
    }
}