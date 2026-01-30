package com.github.synnerz.devonian.features.misc

import com.github.synnerz.devonian.api.events.ClientSoundPlayEvent
import com.github.synnerz.devonian.api.events.SoundPlayEvent
import com.github.synnerz.devonian.features.Feature

object NoBonzoStaffSound : Feature(
    "noBonzoStaffSound",
    "Removes the bonzo staff sounds.",
    subcategory = "General",
    searchTags = setOf("mute", "quiet"),
) {
    private val bonzoFireworks = listOf(
        "minecraft:entity.firework_rocket.blast_far",
        "minecraft:entity.firework_rocket.blast"
    )

    override fun initialize() {
        on<ClientSoundPlayEvent> { event ->
            if (!bonzoFireworks.contains(event.sound)) return@on
            if (event.pitch !in 0.9f..1.1f) return@on
            event.cancel()
        }

        on<SoundPlayEvent> { event ->
            if (event.sound != "minecraft:entity.ghast.ambient") return@on
            if (event.volume != 1f) return@on
            if (event.pitch !in 1.3968254f..1.7936507f) return@on

            event.cancel()
        }
    }
}