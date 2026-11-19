package com.github.synnerz.devonian.features.dungeons.clear

import com.github.synnerz.devonian.api.dungeon.DungeonStages
import com.github.synnerz.devonian.api.events.PacketReceivedEvent
import com.github.synnerz.devonian.api.events.SoundPlayEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.utils.BasicState
import kotlinx.atomicfu.atomic
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket

object MuteRareDungeonDrops : Feature(
    "muteRareDungeonDrops",
    "Mutes the rare drop sound after receiving a rare drop chat message in dungeons",
    Categories.DUNGEONS,
    "catacombs",
    subcategory = "Hiders",
) {
    override fun createRequirements(): List<BasicState<Boolean>?> {
        return super.createRequirements() + listOf(DungeonStages.Clear.isActiveState)
    }

    private val rareDropRegex = "^RARE DROP! ([\\w ]+) \\(\\+\\d+. Magic Find\\)$".toRegex()
    private val validPlingPitch = listOf(
        0.5873016f,
        0.7936508f,
        1.0476191f,
        1.1746032f,
    )
    private val validPickupPitch = listOf(
        0.6984127f,
        0.93650794f,
        1.2539682f,
        1.4126984f,
    )
    private val hasMessage = atomic(false)

    override fun initialize() {
        on<PacketReceivedEvent> { event ->
            val packet = event.packet
            if (packet !is ClientboundSystemChatPacket) return@on
            if (packet.overlay) return@on
            val msg = packet.content.string
            if (!rareDropRegex.matches(msg)) return@on

            hasMessage.value = true
        }

        on<SoundPlayEvent> { event ->
            if (!hasMessage.value) return@on
            if (
                event.sound == "minecraft:entity.experience_orb.pickup" &&
                event.pitch in validPickupPitch &&
                event.volume == 0.2f
            ) {
                event.cancel()

                if (event.pitch == validPickupPitch.last())
                    hasMessage.value = false

                return@on
            }

            if (
                event.sound != "minecraft:block.note_block.pling" ||
                (event.volume != 0.4f && event.volume != 0.8f) ||
                event.pitch !in validPlingPitch
            ) return@on

            event.cancel()
        }
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        hasMessage.value = false
    }
}
