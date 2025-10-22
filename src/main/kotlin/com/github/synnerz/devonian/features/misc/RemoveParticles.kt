package com.github.synnerz.devonian.features.misc

import com.github.synnerz.devonian.api.events.ParticleSpawnEvent
import com.github.synnerz.devonian.features.Feature
import net.minecraft.client.particle.BlockDustParticle
import net.minecraft.client.particle.ExplosionLargeParticle

object RemoveBlockBreakParticle : Feature("removeBlockBreakParticle") {
    override fun initialize() {
        on<ParticleSpawnEvent> { event ->
            if (event.particle !is BlockDustParticle) return@on

            event.ci.cancel()
        }
    }
}

object RemoveExplosionParticle : Feature("removeExplosionParticle") {
    override fun initialize() {
        on<ParticleSpawnEvent> { event ->
            if (event.particle !is ExplosionLargeParticle) return@on

            event.ci.cancel()
        }
    }
}