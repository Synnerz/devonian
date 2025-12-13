package com.github.synnerz.devonian.features.debug.renderers

import com.github.synnerz.devonian.api.events.RenderSlotEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature

object RenderSlotIndex : Feature(
    "renderSlotIndex",
    "",
    Categories.DEBUG,
    subcategory = "Renderers",
) {
    override fun initialize() {
        on<RenderSlotEvent> { event ->
            event.ctx.drawString(
                minecraft.font,
                event.slot.containerSlot.toString(),
                event.slot.x, event.slot.y,
                -1
            )
        }
    }
}