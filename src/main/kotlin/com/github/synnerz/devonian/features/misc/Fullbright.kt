package com.github.synnerz.devonian.features.misc

import com.github.synnerz.devonian.api.Scheduler
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.mixin.accessor.GlDeviceAccessor
import com.mojang.blaze3d.systems.RenderSystem

object Fullbright : Feature("fullbright") {
    override fun onToggle(state: Boolean) {
        Scheduler.scheduleTask {
            val device = RenderSystem.tryGetDevice() as? GlDeviceAccessor ?: return@scheduleTask
            device.pipelineCache.clear()
            device.shaderCache.clear()
        }
    }
}
