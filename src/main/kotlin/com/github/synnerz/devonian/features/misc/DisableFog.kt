package com.github.synnerz.devonian.features.misc

import com.github.synnerz.devonian.features.Feature
import net.minecraft.client.Camera
import net.minecraft.client.renderer.FogParameters
import net.minecraft.client.renderer.FogRenderer
import org.joml.Vector4f

object DisableFog : Feature("disableFog") {
    fun setupFog(
        camera: Camera,
        fogMode: FogRenderer.FogMode,
        fogColor: Vector4f,
        renderDistance: Float,
        foggy: Boolean,
        pt: Float
    ): FogParameters? = when (fogMode) {
        FogRenderer.FogMode.FOG_SKY -> null
        FogRenderer.FogMode.FOG_TERRAIN -> FogParameters.NO_FOG
    }
}