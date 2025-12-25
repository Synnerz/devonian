package com.github.synnerz.devonian.features.misc

import com.github.synnerz.devonian.api.events.SoundPlayEvent
import com.github.synnerz.devonian.features.Feature

object CancelIncorrectSound : Feature(
    "cancelIncorrectSound",
    "Cancels the enderman teleport sound you get whenever you click on a wrong button or out of mana",
    subcategory = "General"
) {
    override fun initialize() {
        on<SoundPlayEvent> { event ->
            if (event.sound != "minecraft:entity.enderman.teleport" || event.volume != 8f || event.pitch != 0f) return@on

            event.cancel()
        }
    }
}