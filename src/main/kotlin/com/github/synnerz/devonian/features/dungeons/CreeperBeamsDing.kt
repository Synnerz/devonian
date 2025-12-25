package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.devonian.api.ChatUtils
import com.github.synnerz.devonian.api.Scheduler
import com.github.synnerz.devonian.api.dungeon.DungeonScanner
import com.github.synnerz.devonian.api.dungeon.Dungeons
import com.github.synnerz.devonian.api.events.SoundPlayEvent
import com.github.synnerz.devonian.commands.DevonianCommand
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.config.Config
import com.github.synnerz.devonian.features.Feature
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.sounds.SoundEvents

object CreeperBeamsDing : Feature(
    "creeperBeamsDing",
    "Plays a ding sound whenever you hit a lantern in creeper beams room",
    Categories.DUNGEONS,
    "catacombs",
    subcategory = "QOL"
) {
    private const val KEY = "creeperBeamsDing"
    private val soundOptions = listOf(
        "minecraft:entity.blaze.hurt",
        "minecraft:entity.experience_orb.pickup",
        "minecraft:block.vault.break",
        "minecraft:entity.elder_guardian.hurt_land",
        "minecraft:item.totem.use",
        "minecraft:block.sculk_catalyst.hit",
        "minecraft:block.ender_chest.close",
        "minecraft:block.note_block.iron_xylophone",
    )
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
    private var soundEvent = SoundEvents.NOTE_BLOCK_IRON_XYLOPHONE.value()

    override fun initialize() {
        Config.set(KEY, "minecraft:block.note_block.iron_xylophone")

        DevonianCommand.command.subcommand("creeperBeamsSound") { _, args ->
            if (args.isEmpty()) return@subcommand 0
            val soundRegistry = args.first() as String


            val sound = BuiltInRegistries.SOUND_EVENT.getValue(ResourceLocation.parse(soundRegistry))
            if (sound == null) {
                ChatUtils.sendMessage("&4Cannot find sound: &6$soundRegistry", true)
                return@subcommand 0
            }
            soundEvent = sound

            Config.set(KEY, soundRegistry)
            ChatUtils.sendMessage("&aSuccessfully set creeper beams ding sound to &6$soundRegistry", true)
            1
        }
            .greedyString("sound")
            .suggest(
                "sound",
                *soundOptions.toTypedArray()
            )

        Config.onAfterLoad {
            val savedRegistry = Config.get<String>(KEY) ?: "minecraft:block.note_block.iron_xylophone"
            soundEvent = BuiltInRegistries.SOUND_EVENT.getValue(ResourceLocation.parse(savedRegistry))
                ?: SoundEvents.NOTE_BLOCK_IRON_XYLOPHONE.value()
        }

        on<SoundPlayEvent> { event ->
            if (DungeonScanner.currentRoom?.name != "Creeper Beams" || Dungeons.inBoss.value) return@on
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
                        soundEvent, event.category,
                        1f, 1f,
                        false
                    )
                }
                return@on
            }
        }
    }
}