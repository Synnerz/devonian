package com.github.synnerz.devonian.features.misc.tooltip

import com.github.synnerz.devonian.ClientTextTooltipStringAccessor
import com.github.synnerz.devonian.api.events.TooltipRenderEvent
import com.github.synnerz.devonian.features.Feature

object RemoveGearScore : Feature(
    "removeGearScore",
    subcategory = "Tooltip",
) {
    override fun initialize() {
        on<TooltipRenderEvent> { event ->
            event.lore.removeIf {
                (it as? ClientTextTooltipStringAccessor)
                    ?.`devonian$asString`()
                    ?.startsWith("Gear Score: ") == true
            }
        }.prio = -1
    }
}