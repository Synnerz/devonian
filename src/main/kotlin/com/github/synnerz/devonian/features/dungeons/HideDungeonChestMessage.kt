package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.devonian.api.events.ChatEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature

object HideDungeonChestMessage : Feature(
    "hideDungeonChestMessage",
    "  BEDROCK CHEST REWARDS",
    Categories.DUNGEONS,
    subcategory = "Hiders",
    searchTags = setOf("cancel"),
) {
    private val chestRegex = "^ {2}\\w+ CHEST REWARDS$".toRegex()

    private var inChest = false

    override fun initialize() {
        on<ChatEvent> { event ->
            if (chestRegex.matches(event.message)) inChest = true

            if (inChest) event.cancel()

            if (event.message.isEmpty()) inChest = false
        }
    }
}