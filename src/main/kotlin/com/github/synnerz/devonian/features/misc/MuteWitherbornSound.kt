package com.github.synnerz.devonian.features.misc

import com.github.synnerz.devonian.api.events.SoundPlayEvent
import com.github.synnerz.devonian.features.Feature

object MuteWitherbornSound : Feature(
    "muteWitherbornSound",
    "I hate witherborn I hate witherborn I hate witherborn I hate witherborn I hate witherborn",
    subcategory = "General",
    searchTags = setOf("cancel"),
) {
    override fun initialize() {
        on<SoundPlayEvent> { event ->
            if (event.sound != "minecraft:entity.wither.death") return@on
            if (event.pitch != 1.1904762f) return@on
            if (event.volume != 1.0f) return@on
            event.cancel()
        }
    }
}