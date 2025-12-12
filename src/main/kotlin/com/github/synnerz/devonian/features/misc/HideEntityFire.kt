package com.github.synnerz.devonian.features.misc

import com.github.synnerz.devonian.api.events.PreRenderEntityEvent
import com.github.synnerz.devonian.features.Feature

object HideEntityFire : Feature("hideEntityFire", subcategory = "Hiders") {
    override fun initialize() {
        on<PreRenderEntityEvent> { event ->
            event.entityState.displayFireAnimation = false
        }
    }
}