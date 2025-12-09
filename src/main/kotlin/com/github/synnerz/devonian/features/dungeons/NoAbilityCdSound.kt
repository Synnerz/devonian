package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.devonian.api.events.ChatEvent
import com.github.synnerz.devonian.api.events.SoundPlayEvent
import com.github.synnerz.devonian.features.Feature

object NoAbilityCdSound : Feature(
    "noAbilityCdSound",
    "Removes the ability cooldown sound in dungeons",
    "Dungeons",
    "catacombs"
) {
    val SETTING_NO_MESSAGE = addSwitch(
        "noMessage",
        true,
        "Removes the ability cooldown message (the \"This ability is on cooldown for Ns\" message)",
        "No CD Message"
    )

    override fun initialize() {
        on<ChatEvent> { event ->
            if (!SETTING_NO_MESSAGE.get()) return@on
            event.matches("^This ability is on cooldown for \\d+s.$".toRegex()) ?: return@on
            event.cancel()
        }

        on<SoundPlayEvent> { event ->
            if (event.sound != "minecraft:entity.enderman.teleport" || event.volume != 8.0f) return@on
            event.cancel()
        }
    }
}