package com.github.synnerz.devonian.features.misc

import com.github.synnerz.devonian.api.ChatUtils
import com.github.synnerz.devonian.api.Scheduler
import com.github.synnerz.devonian.api.events.SoundPlayEvent
import com.github.synnerz.devonian.commands.DevonianCommand
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.utils.JsonUtils
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.sounds.SoundEvents

object EtherwarpSound : Feature(
    "etherwarpSound",
    "Changes the sound the etherwarp makes whenever you have etherwarped successfully, customize it via /devonian etherwarpsound"
) {
    private const val KEY = "etherwarpSound"
    private var soundEvent = SoundEvents.ENDER_DRAGON_HURT

    override fun initialize() {
        JsonUtils.set(KEY, "minecraft:entity.ender_dragon.hurt")

        // TODO: too lazy to make pitch/volume customizable, make that later on if requested
        DevonianCommand.command.subcommand("etherwarpsound") { _, args ->
            if (args.isEmpty()) return@subcommand 0
            val soundRegistry = args.first() as String

            soundEvent = BuiltInRegistries.SOUND_EVENT.getValue(ResourceLocation.parse(soundRegistry))

            JsonUtils.set(KEY, soundRegistry)
            ChatUtils.sendMessage("&aSuccessfully set etherwarp sound to &6$soundRegistry", true)
            1
        }
            .greedyString("sound")
            .suggest(
                "sound",
                *BuiltInRegistries.SOUND_EVENT.entrySet().map { "${it.value.location.namespace}:${it.value.location.path}" }.toTypedArray()
            )

        JsonUtils.afterLoad {
            val savedRegistry = JsonUtils.get<String>(KEY) ?: "minecraft:entity.ender_dragon.hurt"
            soundEvent = BuiltInRegistries.SOUND_EVENT.getValue(ResourceLocation.parse(savedRegistry))
        }

        on<SoundPlayEvent> { event ->
            if (
                event.sound != "minecraft:entity.ender_dragon.hurt" ||
                event.volume != 1f ||
                event.pitch != 0.53968257f ||
                soundEvent == SoundEvents.ENDER_DRAGON_HURT
            ) return@on

            event.cancel()
            Scheduler.scheduleTask(0) {
                minecraft.level?.playLocalSound(
                    event.x, event.y, event.z,
                    soundEvent, event.category,
                    1f, 1f,
                    false
                )
            }
        }
    }
}