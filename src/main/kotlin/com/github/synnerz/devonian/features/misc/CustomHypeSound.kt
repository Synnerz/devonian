package com.github.synnerz.devonian.features.misc

import com.github.synnerz.devonian.api.ItemUtils
import com.github.synnerz.devonian.api.Scheduler
import com.github.synnerz.devonian.api.events.EventBus
import com.github.synnerz.devonian.api.events.PacketSentEvent
import com.github.synnerz.devonian.api.events.SoundPlayEvent
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.utils.CustomSounds
import net.minecraft.network.protocol.game.ServerboundUseItemPacket

object CustomHypeSound : Feature(
    "customHypeSound",
    "Changes the explosion sound made from wither blades to a custom one. " +
    "`/dv hypesound`",
    subcategory = "General",
) {
    private val customSound = CustomSounds.create("witherBladeSound", "minecraft:block.note_block.iron_xylophone")
    private val witherBlades = listOf("HYPERION", "VALKYRIE", "SCYLLA", "ASTRAEA")
    private var lastClick = -1

    override fun initialize() {
        customSound.registerCommand("hype")

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
            Scheduler.scheduleTask { customSound.playWithEvent(event, false) }
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