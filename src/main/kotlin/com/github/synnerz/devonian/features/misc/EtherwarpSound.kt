package com.github.synnerz.devonian.features.misc

import com.github.synnerz.devonian.api.ChatUtils
import com.github.synnerz.devonian.api.events.SoundPlayEvent
import com.github.synnerz.devonian.commands.DevonianCommand
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.utils.JsonUtils
import net.minecraft.registry.Registries
import net.minecraft.sound.SoundEvents
import net.minecraft.util.Identifier

object EtherwarpSound : Feature("etherwarpSound") {
    private const val KEY = "etherwarpSound"
    private var soundEvent = SoundEvents.ENTITY_ENDER_DRAGON_HURT

    override fun initialize() {
        JsonUtils.set(KEY, "minecraft:entity.ender_dragon.hurt")

        // TODO: too lazy to make pitch/volume customizable, make that later on if requested
        DevonianCommand.command.subcommand("etherwarpsound") { _, args ->
            if (args.isEmpty()) return@subcommand 0
            val soundRegistry = args.first() as String

            soundEvent = Registries.SOUND_EVENT.get(Identifier.of(soundRegistry))

            JsonUtils.set(KEY, soundRegistry)
            ChatUtils.sendMessage("&aSuccessfully set etherwarp sound to &6$soundRegistry", true)
            1
        }
            .greedyString("sound")
            .suggest(
                "sound",
                *Registries.SOUND_EVENT.entrySet.map { "${it.value.id.namespace}:${it.value.id.path}" }.toTypedArray()
            )

        JsonUtils.afterLoad {
            val savedRegistry = JsonUtils.get<String>(KEY) ?: "minecraft:entity.ender_dragon.hurt"
            soundEvent = Registries.SOUND_EVENT.get(Identifier.of(savedRegistry))
        }

        on<SoundPlayEvent> { event ->
            if (
                event.sound != "minecraft:entity.ender_dragon.hurt" ||
                event.volume != 1f ||
                event.pitch != 0.53968257f ||
                soundEvent == SoundEvents.ENTITY_ENDER_DRAGON_HURT
            ) return@on

            event.cancel()
            minecraft.player?.playSound(soundEvent, 1f, 1f)
        }
    }
}