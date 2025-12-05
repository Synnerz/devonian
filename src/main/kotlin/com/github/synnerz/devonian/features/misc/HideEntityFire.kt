package com.github.synnerz.devonian.features.misc

import com.github.synnerz.devonian.api.events.RenderEntityEvent
import com.github.synnerz.devonian.features.Feature

object HideEntityFire : Feature("hideEntityFire") {
    override fun initialize() {
        on<RenderEntityEvent> { event ->
            event.entityState.displayFireAnimation = false
        }
    }
}