package com.github.synnerz.devonian.features.misc.hiders

import com.github.synnerz.devonian.api.events.PacketReceivedEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket
import net.minecraft.world.effect.MobEffects

object DisableBlindness : Feature(
    "disableBlindness",
    "Disables the blindness effect (from the server idk you can still get blind).",
    Categories.VANILLA_TWEAKS,
    subcategory = "Hider",
    cheeto = true,
) {
    override fun initialize() {
        on<PacketReceivedEvent> { event ->
            val packet = event.packet as? ClientboundUpdateMobEffectPacket ?: return@on
            val player = minecraft.player ?: return@on

            if (packet.entityId != player.id) return@on
            if (packet.effect != MobEffects.BLINDNESS) return@on

            event.cancel()
        }
    }
}