package com.github.synnerz.devonian.features.misc

import com.github.synnerz.devonian.api.ChatUtils
import com.github.synnerz.devonian.api.ItemUtils
import com.github.synnerz.devonian.api.Scheduler
import com.github.synnerz.devonian.api.events.EventBus
import com.github.synnerz.devonian.api.events.PacketSentEvent
import com.github.synnerz.devonian.api.events.SoundPlayEvent
import com.github.synnerz.devonian.commands.DevonianCommand
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.utils.CustomSounds
import net.minecraft.network.protocol.game.ServerboundUseItemPacket

object CustomHypeSound : Feature(
    "customHypeSound",
    "Changes the explosion sound made from wither blades to a custom one. " +
    "`/dv hypesound`",
    subcategory = "General",
) {
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
    val customSound = CustomSounds.create("witherBladeSound", "minecraft:block.note_block.iron_xylophone")
    private val witherBlades = listOf("HYPERION", "VALKYRIE", "SCYLLA", "ASTRAEA")
    private var lastClick = -1
    var playSound = false

    override fun initialize() {
        DevonianCommand.command.subcommand("hypesound") { _, args ->
            val volume = args.firstOrNull() as? String?
            val pitch = args.getOrNull(1) as? String
            var soundName = args.getOrNull(2) as? String
            if (soundName.isNullOrEmpty()) soundName = "minecraft:block.note_block.iron_xylophone"

            customSound.setValues(
                soundName,
                if (volume.isNullOrEmpty()) 1f else volume.toFloatOrNull() ?: 1f,
                if (pitch.isNullOrEmpty()) 1f else pitch.toFloatOrNull() ?: 1f
            )

            if (customSound.soundEvent == null) {
                ChatUtils.sendMessage("&4Cannot find sound: &6$soundName", true)
                return@subcommand 0
            }

            ChatUtils.sendMessage("&aSuccessfully set wither blade sound to &6$soundName", true)
            1
        }
            .float("volume", 0f, 10f)
            .float("pitch", 0f, 10f)
            .greedyString("sound")
            .suggest(
                "sound",
                *soundOptions.toTypedArray()
            )

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
            playSound = true
            Scheduler.scheduleTask {
                customSound.playWithEvent(event)
                playSound = false
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