package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.devonian.api.Scheduler
import com.github.synnerz.devonian.api.dungeon.DungeonScanner
import com.github.synnerz.devonian.api.events.SoundPlayEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import net.minecraft.sounds.SoundEvents

object CreeperBeamsDing : Feature(
    "creeperBeamsDing",
    "Plays a ding sound whenever you hit a lantern in creeper beams room",
    Categories.DUNGEONS,
    "catacombs",
    subcategory = "QOL"
) {
    private val SETTING_REMOVE_CREEPER_HURT = addSwitch(
        "removeCreeperHurt",
        false,
        "Removes the sounds that the creeper makes whenever you successfully connect two lanterns",
        "Remove Creeper Hurt"
    )
    private val SETTING_REMOVE_EXPLOSION = addSwitch(
        "removeExplosion",
        false,
        "Removes the explosion sound whenever the puzzle is completed",
        "Remove Explosion Sound"
    )
    private val soundEvent = SoundEvents.NOTE_BLOCK_IRON_XYLOPHONE

    override fun initialize() {
        on<SoundPlayEvent> { event ->
            if (DungeonScanner.currentRoom?.name != "Creeper Beams") return@on
            if (
                SETTING_REMOVE_CREEPER_HURT.get() &&
                (event.sound == "minecraft:entity.creeper.hurt" || event.sound == "minecraft:entity.creeper.primed")
            ) {
                event.cancel()
                return@on
            }
            if (SETTING_REMOVE_EXPLOSION.get() && event.sound == "minecraft:entity.generic.explode") {
                event.cancel()
                return@on
            }
            // pitch
            // 1.3968254f - normal hit sound
            // 2.0f - successful two hit sound
            if (
                (event.sound == "minecraft:entity.experience_orb.pickup" &&
                event.pitch == 0.7936508f) ||
                (event.sound == "minecraft:entity.elder_guardian.hurt" &&
                event.pitch == 1.3968254f || event.pitch == 2.0f)
            ) {
                event.cancel()

                Scheduler.scheduleTask {
                    minecraft.level?.playLocalSound(
                        event.x, event.y, event.z,
                        soundEvent.value(), event.category,
                        1f, 1f,
                        false
                    )
                }
                return@on
            }
        }
    }
}