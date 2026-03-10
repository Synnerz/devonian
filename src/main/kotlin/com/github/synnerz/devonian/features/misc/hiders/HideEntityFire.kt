package com.github.synnerz.devonian.features.misc.hiders

import com.github.synnerz.devonian.api.events.PreRenderEntityEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature

object HideEntityFire : Feature(
    "hideEntityFire",
    "Do not render entities as being on fire.",
    Categories.VANILLA_TWEAKS,
    subcategory = "Hider",
) {
    override fun initialize() {
        on<PreRenderEntityEvent> { event ->
            event.entityState.displayFireAnimation = false
        }
    }
}