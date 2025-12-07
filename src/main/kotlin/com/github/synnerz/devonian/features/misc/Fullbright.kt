package com.github.synnerz.devonian.features.misc

import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.mixin.accessor.GlDeviceAccessor
import com.mojang.blaze3d.systems.RenderSystem

object Fullbright : Feature("fullbright") {
    override fun onToggle(state: Boolean) {
        val device = RenderSystem.tryGetDevice() as? GlDeviceAccessor ?: return
        device.pipelineCache.clear()
        device.shaderCache.clear()
    }
}
