package com.github.synnerz.devonian.features.dungeons.f7

import com.github.synnerz.devonian.api.Scheduler
import com.github.synnerz.devonian.api.dungeon.Stages
import com.github.synnerz.devonian.api.events.SoundPlayEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.utils.BasicState

object CancelF7BossSounds : Feature(
    "customF7Sounds",
    "Mutes the loud boss sounds played in f7 boss fight.",
    Categories.DUNGEONS,
    "catacombs",
    displayName = "Cancel F7 Boss Sounds",
    subcategory = "F7",
    searchTags = setOf("mute", "quiet"),
) {
    override fun createRequirements(): List<BasicState<Boolean>?> {
        return super.createRequirements() + listOf(Stages.F7.isActiveState)
    }

    private val SETTING_CANCEL_ABOVE_CAP = addSwitch(
        "cancelAboveCap",
        false,
        "Cancels the sounds that are above the cap volume which is 1 (some sounds like arrow hits are 1.4)",
        "Cancel Above Cap F7Sounds"
    )
    private val SETTING_ONLY_LOWER = addSwitch(
        "onlyLower",
        false,
        "Caps the volume at max rather than going over it for louder sounds (WILL NO LONGER CANCEL THE SOUNDS)",
        "Cap F7Sounds"
    )

    override fun initialize() {
        on<SoundPlayEvent> { event ->
            if (SETTING_ONLY_LOWER.get() && event.volume > 1f) {
                Scheduler.scheduleTask(0) {
                    minecraft.level?.playLocalSound(
                        event.x, event.y, event.z,
                        event.underlyingEvent, event.category,
                        1f, event.pitch,
                        false
                    )
                }
                return@on
            }

            if (SETTING_CANCEL_ABOVE_CAP.get() && event.volume > 1f || event.volume >= 2f) {
                event.cancel()
                return@on
            }

            if (
                event.sound == "minecraft:entity.wither.hurt" &&
                event.volume == 0.5f &&
                (event.pitch == 1.0f || event.pitch == 0.93650794f || event.pitch == 1.0158731f || event.pitch == 1.1111112f)
            ) {
                event.cancel()
                return@on
            }

            if (
                event.sound == "minecraft:entity.wither.death" &&
                event.volume == 0.5f &&
                (event.pitch in 0.8730159f..1.1587301f)
            ) event.cancel()
        }
    }
}