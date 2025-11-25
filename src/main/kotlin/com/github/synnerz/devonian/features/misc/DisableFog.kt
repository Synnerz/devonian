package com.github.synnerz.devonian.features.misc

import com.github.synnerz.devonian.features.Feature
import net.minecraft.client.renderer.fog.environment.FogEnvironment
import net.minecraft.world.entity.Entity
import net.minecraft.world.level.material.FogType

object DisableFog : Feature("disableFog") {
    fun setupFog(
        instance: FogEnvironment,
        fogType: FogType,
        entity: Entity
    ): Boolean? = when (fogType) {
        FogType.ATMOSPHERIC -> false
        else -> null
    }
}