package com.github.synnerz.devonian.features.misc

import com.github.synnerz.devonian.api.events.PacketReceivedEvent
import com.github.synnerz.devonian.features.Feature
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket

object HideHypeHearts : Feature(
    "hideHypeHearts",
    "hides hearts from healing from wither shield",
    subcategory = "Hiders",
) {
    override fun initialize() {
        on<PacketReceivedEvent> { event ->
            val packet = event.packet as? ClientboundLevelParticlesPacket ?: return@on

            if (packet.particle.type != ParticleTypes.HEART) return@on
            if (packet.count != 3) return@on
            if (!packet.alwaysShow()) return@on
            if (!packet.isOverrideLimiter) return@on

            event.cancel()
        }
    }
}