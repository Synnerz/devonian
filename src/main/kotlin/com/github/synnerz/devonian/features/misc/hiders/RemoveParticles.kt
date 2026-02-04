package com.github.synnerz.devonian.features.misc.hiders

import com.github.synnerz.devonian.api.events.ParticleSpawnEvent
import com.github.synnerz.devonian.features.Feature
import net.minecraft.client.particle.HugeExplosionParticle
import net.minecraft.client.particle.SmokeParticle
import net.minecraft.client.particle.TerrainParticle

object RemoveBlockBreakParticle : Feature("removeBlockBreakParticle", subcategory = "Hiders") {
    override fun initialize() {
        on<ParticleSpawnEvent> { event ->
            if (event.particle !is TerrainParticle) return@on

            event.cancel()
        }
    }
}

object RemoveExplosionParticle : Feature("removeExplosionParticle", subcategory = "Hiders") {
    override fun initialize() {
        on<ParticleSpawnEvent> { event ->
            if (event.particle !is HugeExplosionParticle) return@on

            event.cancel()
        }
    }
}

object RemoveSmokeParticle : Feature("removeSmokeParticle", subcategory = "Hiders") {
    override fun initialize() {
        on<ParticleSpawnEvent> { event ->
            if (event.particle !is SmokeParticle) return@on

            event.cancel()
        }
    }
}