package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.devonian.api.ChatUtils
import com.github.synnerz.devonian.api.Scheduler
import com.github.synnerz.devonian.api.dungeon.DungeonEvent
import com.github.synnerz.devonian.api.events.ChatEvent
import com.github.synnerz.devonian.commands.DevonianCommand
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.utils.CustomSounds
import net.minecraft.core.registries.BuiltInRegistries

object SecretsSound : Feature(
    "secretsSound",
    "Plays a sound whenever you click, pick up (a secret) or kill a bat (This also plays an anvil sound whenever the chest is locked). /dv secretsound to customize",
    Categories.DUNGEONS,
    "catacombs",
    subcategory = "QOL",
) {
    private val lockedChestRegex = "^That chest is locked!$".toRegex()
    private val successOpt = CustomSounds.create("SecretsSoundSuccess", "minecraft:entity.blaze.hurt")
    private val declineOpt = CustomSounds.create("SecretsSoundDecline", "minecraft:block.anvil.place")

    override fun initialize() {
        DevonianCommand.command.subcommand("secretsound") { _, args ->
            val type = args.firstOrNull() ?: return@subcommand 0
            val volume = args.getOrNull(1) as? String?
            val pitch = args.getOrNull(2) as? String?
            var soundName = args.getOrNull(3) as? String
            if (soundName.isNullOrEmpty()) {
                soundName = if (type == "SUCCESS") "minecraft:entity.blaze.hurt"
                else "minecraft:block.anvil.place"
            }
            if (type == "SUCCESS")
                successOpt.setValues(
                    soundName,
                    if (volume.isNullOrEmpty()) 1f else volume.toFloatOrNull() ?: 1f,
                    if (pitch.isNullOrEmpty()) 1f else pitch.toFloatOrNull() ?: 1f
                )
            else
                declineOpt.setValues(
                    soundName,
                    if (volume.isNullOrEmpty()) 1f else volume.toFloatOrNull() ?: 1f,
                    if (pitch.isNullOrEmpty()) 1f else pitch.toFloatOrNull() ?: 1f
                )
            ChatUtils.sendMessage("&aSuccessfully set SecretsSound to &6$soundName", true)
            1
        }
            .string("type")
            .float("volume", 0f, 10f)
            .float("pitch", 0f, 10f)
            .greedyString("sound")
            .suggest("type", *listOf("SUCCESS", "DECLINE").toTypedArray())
            .suggest("sound", *BuiltInRegistries.SOUND_EVENT.entrySet().map { "${it.value.location.namespace}:${it.value.location.path}" }.toTypedArray())

        on<DungeonEvent.SecretPickup> { playSound() }
        on<DungeonEvent.SecretClicked> { playSound() }
        on<DungeonEvent.SecretBatSound> {
            it.cancel()
            Scheduler.scheduleTask {
                playSound()
            }
        }

        on<ChatEvent> { event ->
            event.matches(lockedChestRegex) ?: return@on
            playSound(true)
        }
    }

    private fun playSound(declined: Boolean = false) {
        if (declined) return declineOpt.play()
        successOpt.play()
    }
}