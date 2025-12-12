package com.github.synnerz.devonian.features.misc

import com.github.synnerz.devonian.api.Scheduler
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.mixin.accessor.GlDeviceAccessor
import com.github.synnerz.devonian.utils.Toggleable
import com.mojang.blaze3d.opengl.GlDevice
import com.mojang.blaze3d.shaders.ShaderType
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.renderer.RenderPipelines

object Fullbright : Feature("fullbright", subcategory = "Tweaks") {
    override fun initialize() {
        children.add(
            object : Toggleable() {
                override fun add() { }
                override fun remove() {}

                override fun change() {
                    Scheduler.scheduleTask {
                        val device = RenderSystem.tryGetDevice() as? GlDeviceAccessor ?: return@scheduleTask
                        device.pipelineCache.remove(RenderPipelines.LIGHTMAP)
                        val key = GlDevice.ShaderCompilationKey(
                            RenderPipelines.LIGHTMAP.fragmentShader,
                            ShaderType.FRAGMENT,
                            RenderPipelines.LIGHTMAP.shaderDefines
                        )
                        device.shaderCache.remove(key)
                    }
                }
            }
        )
    }
}
