package com.github.synnerz.devonian.features.misc

import com.github.synnerz.devonian.api.ChatUtils
import com.github.synnerz.devonian.api.ItemUtils
import com.github.synnerz.devonian.api.Scheduler
import com.github.synnerz.devonian.api.events.EventBus
import com.github.synnerz.devonian.api.events.PacketSentEvent
import com.github.synnerz.devonian.api.events.SoundPlayEvent
import com.github.synnerz.devonian.commands.DevonianCommand
import com.github.synnerz.devonian.config.Config
import com.github.synnerz.devonian.features.Feature
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.protocol.game.ServerboundUseItemPacket
import net.minecraft.resources.ResourceLocation
import net.minecraft.sounds.SoundEvents

object CustomHypeSound : Feature(
    "customHypeSound",
    "Changes the explosion sound made from wither blades to a custom one",
    subcategory = "General"
) {
    private const val KEY = "witherBladeSound"
    private const val KEY_VOLUME = "$KEY\$Volume"
    private const val KEY_PITCH = "$KEY\$Pitch"
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
    private var soundEvent = SoundEvents.NOTE_BLOCK_IRON_XYLOPHONE.value()
    private var volume = 1f
    private var pitch = 1f
    private val witherBlades = listOf("HYPERION", "VALKYRIE", "SCYLLA", "ASTRAEA")
    private var lastClick = -1

    override fun initialize() {
        Config.set(KEY, "minecraft:block.note_block.iron_xylophone")
        Config.set(KEY_VOLUME, 1f)
        Config.set(KEY_PITCH, 1f)

        DevonianCommand.command.subcommand("hypesound") { _, args ->
            if (args.isEmpty()) return@subcommand 0
            val argVolume = args.first() as Float
            val argPitch = args.getOrNull(1) as? Float ?: 1f
            val soundRegistry = args.getOrNull(2) as? String ?: "minecraft:block.note_block.iron_xylophone"

            soundEvent = BuiltInRegistries.SOUND_EVENT.getValue(ResourceLocation.parse(soundRegistry))
            volume = argVolume
            pitch = argPitch

            Config.set(KEY, soundRegistry)
            ChatUtils.sendMessage("&aSuccessfully set wither blade sound to &6$soundRegistry", true)
            1
        }
            .float("volume", 0f, 1f)
            .float("pitch", 0f, 1f)
            .greedyString("sound")
            .suggest(
                "sound",
                *soundOptions.toTypedArray()
            )

        Config.onAfterLoad {
            val savedRegistry = Config.get<String>(KEY) ?: "minecraft:block.note_block.iron_xylophone"
            soundEvent = BuiltInRegistries.SOUND_EVENT.getValue(ResourceLocation.parse(savedRegistry))
            volume = Config.get<Float>(KEY_VOLUME) ?: 1f
            pitch = Config.get<Float>(KEY_PITCH) ?: 1f
        }

        on<SoundPlayEvent> { event ->
            if (lastClick == -1) return@on
            val ticks = EventBus.serverTicks() - lastClick
            if (ticks > 3) {
                lastClick = -1
                return@on
            }
            if (event.sound == "minecraft:entity.zombie_villager.cure" && event.volume == 1f && event.pitch == 0.6984127f) {
                event.cancel()
                return@on
            }

            if (event.sound != "minecraft:entity.generic.explode" || event.volume != 1f || event.pitch != 1f) return@on

            event.cancel()
            Scheduler.scheduleTask {
                minecraft.level?.playLocalSound(
                    event.x, event.y, event.z,
                    soundEvent, event.category,
                    volume, pitch,
                    false
                )
            }
        }

        on<PacketSentEvent> { event ->
            val packet = event.packet
            if (packet !is ServerboundUseItemPacket) return@on
            val player = minecraft.player ?: return@on
            val sbId = ItemUtils.skyblockId(player.getItemInHand(packet.hand)) ?: return@on
            if (!witherBlades.contains(sbId)) return@on

            lastClick = EventBus.serverTicks(true)
        }
    }
}