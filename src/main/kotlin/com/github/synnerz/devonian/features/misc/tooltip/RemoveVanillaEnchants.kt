package com.github.synnerz.devonian.features.misc.tooltip

import com.github.synnerz.devonian.ClientTextTooltipStringAccessor
import com.github.synnerz.devonian.api.events.TooltipRenderEvent
import com.github.synnerz.devonian.features.Feature

object RemoveVanillaEnchants : Feature(
    "removeVanillaEnchants",
    "hides Aqua Affinity/Depth Strider from lore",
    subcategory = "Tooltip",
) {
    private val enchants = listOf(
        "Aqua Affinity",
        "Depth Strider",
    )

    override fun initialize() {
        on<TooltipRenderEvent> { event ->
            event.lore.removeIf {
                val str = (it as? ClientTextTooltipStringAccessor)?.`devonian$asString`() ?: return@removeIf false
                enchants.any { str.startsWith(it) }
            }
        }.prio = -1
    }
}