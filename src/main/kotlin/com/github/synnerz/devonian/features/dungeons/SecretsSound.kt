package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.devonian.api.Scheduler
import com.github.synnerz.devonian.api.dungeon.DungeonEvent
import com.github.synnerz.devonian.api.events.ChatEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import net.minecraft.sounds.SoundEvents

object SecretsSound : Feature(
    "secretsSound",
    "Plays a sound whenever you click, pick up (a secret) or kill a bat (This also plays an anvil sound whenever the chest is locked)",
    Categories.DUNGEONS,
    "catacombs",
    subcategory = "QOL",
) {
    private val lockedChestRegex = "^That chest is locked!$".toRegex()
    private val successSound = SoundEvents.BLAZE_HURT
    private val declineSound = SoundEvents.ANVIL_PLACE

    override fun initialize() {
        on<DungeonEvent.SecretPickup> { playSound() }
        on<DungeonEvent.SecretClicked> { playSound() }
        on<DungeonEvent.SecretBat> {
            it.cancel()
            playSound()
        }

        on<ChatEvent> { event ->
            event.matches(lockedChestRegex) ?: return@on
            playSound(true)
        }
    }

    private fun playSound(declined: Boolean = false) {
        if (declined) {
            Scheduler.scheduleTask { minecraft.player?.playSound(declineSound, 1f, 1f) }
            return
        }
        Scheduler.scheduleTask { minecraft.player?.playSound(successSound, 1f, 2f) }
    }
}