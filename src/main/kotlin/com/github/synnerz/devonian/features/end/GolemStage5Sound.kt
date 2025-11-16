package com.github.synnerz.devonian.features.end

import com.github.synnerz.devonian.api.events.ChatEvent
import com.github.synnerz.devonian.features.Feature
import net.minecraft.sound.SoundEvents

object GolemStage5Sound : Feature(
    "golemStage5Sound",
    "Plays an Anvil Place sound whenever the golem hits stage 5",
    "end",
    "the end"
) {
    private val golemSpawnRegex = "^The ground begins to shake as an End Stone Protector rises from below!$".toRegex()
    private val soundEvent = SoundEvents.BLOCK_ANVIL_PLACE

    override fun initialize() {
        on<ChatEvent> { event ->
            event.matches(golemSpawnRegex) ?: return@on
            minecraft.player?.playSound(soundEvent, 1f, 1f)
        }
    }
}