package com.github.synnerz.devonian.features.misc

import com.github.synnerz.devonian.api.events.RenderOverlayEvent
import com.github.synnerz.devonian.hud.texthud.TextHudFeature
import com.github.synnerz.devonian.utils.StringUtils

object FPSDisplay : TextHudFeature(
    "fpsDisplay",
    "Displays your frames per seconds in a hud.",
    subcategory = "General",
) {
    private val fpsLimit get() = minecraft.options.framerateLimit()

    override fun initialize() {
        on<RenderOverlayEvent> {
            val fps = minecraft.fps
            setLine("&fFPS&f: ${StringUtils.colorForNumber(fps, fpsLimit.get())}$fps")
            draw(it.ctx)
        }
    }

    override fun getEditText(): List<String> = listOf("&fFPS&f: &2100")
}