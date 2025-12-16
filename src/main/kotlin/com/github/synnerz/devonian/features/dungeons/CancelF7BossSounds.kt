package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.devonian.api.dungeon.Dungeons
import com.github.synnerz.devonian.api.events.SoundPlayEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature

object CancelF7BossSounds : Feature(
    "customF7Sounds",
    "Cancels the boss sounds played in f7 boss fight",
    Categories.DUNGEONS,
    "catacombs",
    subcategory = "Hiders",
) {
    override fun initialize() {
        on<SoundPlayEvent> { event ->
            if (!Dungeons.inBoss.value || Dungeons.floor.floorNum != 7) return@on
            if (event.volume >= 2f) {
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