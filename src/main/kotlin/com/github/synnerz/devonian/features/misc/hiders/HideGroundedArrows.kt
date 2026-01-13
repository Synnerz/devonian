package com.github.synnerz.devonian.features.misc.hiders

import com.github.synnerz.devonian.api.events.PreExtractRenderEntityEvent
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.mixin.accessor.AbstractArrowAccessor

object HideGroundedArrows : Feature("hideGroundedArrows", subcategory = "Hiders") {
    override fun initialize() {
        on<PreExtractRenderEntityEvent> { event ->
            val arrow = event.entity as? AbstractArrowAccessor ?: return@on
            if (arrow.isInGround) event.cancel()
        }
    }
}