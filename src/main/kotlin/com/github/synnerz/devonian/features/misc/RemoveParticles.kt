package com.github.synnerz.devonian.features.misc

import com.github.synnerz.devonian.events.Events
import com.github.synnerz.devonian.features.Feature
import net.minecraft.client.particle.BlockDustParticle
import net.minecraft.client.particle.ExplosionLargeParticle

object RemoveParticles : Feature("removeParticles") {
    override fun initialize() {
        Events.onParticleSpawn { particle, callbackInfo ->
            if (!isEnabled()) return@onParticleSpawn
            when (particle) {
                is BlockDustParticle -> callbackInfo.cancel()
                is ExplosionLargeParticle -> callbackInfo.cancel()
            }
        }
    }
}