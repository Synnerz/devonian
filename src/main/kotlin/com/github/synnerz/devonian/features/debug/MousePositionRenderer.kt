package com.github.synnerz.devonian.features.debug

import com.github.synnerz.devonian.api.events.RenderOverlayEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.utils.render.Render2D
import java.awt.Color

object MousePositionRenderer : Feature(
    "mousePositionRenderer",
    "",
    Categories.DEBUG,
    subcategory = "Renderers",
) {
    override fun initialize() {
        on<RenderOverlayEvent> { event ->
            val window = minecraft.window ?: return@on
            val mx = minecraft.mouseHandler.getScaledXPos(window).toInt()
            val my = minecraft.mouseHandler.getScaledYPos(window).toInt()
            Render2D.drawCircle(event.ctx, mx, my, 1, Color.RED)
        }
    }
}