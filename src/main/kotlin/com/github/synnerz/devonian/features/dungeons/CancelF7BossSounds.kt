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
            if (
                event.sound == "minecraft:entity.wither.ambient" &&
                (event.pitch == 0.4920635f && event.volume == 0.1f ||
                event.pitch == 1.1904762f && event.volume == 5.0f)
            ) event.cancel()

            // Louad wither ambient
            if (
                event.sound == "minecraft:entity.wither.ambient" &&
                event.volume == 30.0f &&
                event.pitch == 0.6984127f
            ) event.cancel()

            if (
                event.sound == "minecraft:entity.lightning_bolt.thunder" &&
                event.volume == 10000.0f &&
                event.pitch == 0.8888889f
            ) event.cancel()

            // Loud wither hurt
            if (
                event.sound == "minecraft:entity.wither.hurt" &&
                event.volume == 15f &&
                (event.pitch == 1f || event.pitch == 0.4920635f)
            ) event.cancel()

            // Lower wither hurt
            if (
                event.sound == "minecraft:entity.wither.hurt" &&
                (event.volume == 0.5f || event.volume == 5.0f) &&
                (event.pitch == 1.0f || event.pitch == 0.93650794f || event.pitch == 1.0158731f || event.pitch == 1.1111112f)
            ) event.cancel()

            if (event.sound == "minecraft:entity.generic.explode") {
                if (event.volume in 2f..40f && event.pitch == 0.4920635f) event.cancel()
                if (event.volume in 2f..4f && event.pitch in 0.52380955f..0.8095238f) event.cancel()
            }

            if (
                event.sound == "minecraft:entity.wither.death" &&
                event.volume == 0.5f &&
                (event.pitch in 0.8730159f..1.1587301f)
            ) event.cancel()
        }
    }
}