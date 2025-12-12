package com.github.synnerz.devonian.features.misc

import com.github.synnerz.devonian.api.events.PacketReceivedEvent
import com.github.synnerz.devonian.features.Feature
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket

object DoubleAnimationFix : Feature(
    "doubleAnimationFix",
    "fix animations playing twice sometimes (sneak)",
    subcategory = "Tweaks",
    cheeto = true,
) {
    override fun initialize() {
        on<PacketReceivedEvent> { event ->
            val packet = event.packet as? ClientboundSetEntityDataPacket ?: return@on

            if (packet.id != minecraft.player?.id) return@on
            packet.packedItems.removeIf {
                it.id == 6
                // it.serializer == EntityDataSerializers.POSE
            }
        }
    }
}