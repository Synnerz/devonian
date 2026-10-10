package com.github.synnerz.devonian.features.misc

import com.github.synnerz.devonian.api.Scheduler
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.mixin.accessor.RenderSystemAccessor
import com.github.synnerz.devonian.utils.Toggleable

object Fullbright : Feature(
    "fullbright",
    category = Categories.VANILLA_TWEAKS,
) {
    override fun initialize() {
        children.add(
            object : Toggleable() {
                override fun add() { }
                override fun remove() {}

                override fun change() {
                    Scheduler.scheduleTask {
                        RenderSystemAccessor.getCurrentPipelineCache()?.clear()
                        RenderSystemAccessor.getFallbackPipelineCache()?.clear()
                    }
                }
            }
        )
    }
}
