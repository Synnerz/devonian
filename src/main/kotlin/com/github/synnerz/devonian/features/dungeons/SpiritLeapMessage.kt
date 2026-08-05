package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.devonian.api.ChatUtils
import com.github.synnerz.devonian.api.events.ChatEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature

object SpiritLeapMessage : Feature(
    "spiritLeapMessage",
    "sends a leap message in party chat whenever you leap to someone",
    Categories.DUNGEONS,
    "catacombs",
    subcategory = "QOL",
    searchTags = setOf("leap")
) {
    private val teleportMessageRegex = "^You have teleported to (\\w{1,16})!$".toRegex()

    override fun initialize() {
        on<ChatEvent> { event ->
            val ( name ) = event.matches(teleportMessageRegex) ?: return@on

            ChatUtils.command("pc Leaped to $name")
        }
    }
}