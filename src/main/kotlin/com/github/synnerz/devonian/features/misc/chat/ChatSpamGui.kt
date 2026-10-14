package com.github.synnerz.devonian.features.misc.chat

import com.github.synnerz.devonian.api.events.ChatEvent
import com.github.synnerz.devonian.api.events.RenderOverlayEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.hud.texthud.OverflowTextHud
import com.github.synnerz.devonian.hud.texthud.StylizedTextHud
import com.github.synnerz.devonian.hud.texthud.TextHudFeature
import com.github.synnerz.devonian.utils.StringUtils.colorCodes

object ChatSpamGui : TextHudFeature(
    "chatSpamGui",
    "do not use",
    Categories.MISC,
    subcategory = "Chat",
) {
    override fun createHud(): StylizedTextHud = OverflowTextHud(configName, this)

    override fun getEditText(): List<String> = listOf("lots of messages here", "trust")

    override fun initialize() {
        on<ChatEvent> { event ->
            event.cancel()
            addLine(event.text.colorCodes())
        }

        on<RenderOverlayEvent> { event ->
            draw(event.ctx)
        }
    }
}