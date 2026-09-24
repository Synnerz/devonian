package com.github.synnerz.devonian.features.misc

import com.github.synnerz.devonian.api.ChatUtils
import com.github.synnerz.devonian.api.events.ChatEvent
import com.github.synnerz.devonian.api.events.ClientThreadServerTickEvent
import com.github.synnerz.devonian.api.events.RenderOverlayEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.hud.texthud.TextHudFeature
import com.github.synnerz.devonian.utils.StringUtils

object KickedMessage : TextHudFeature(
    "kickedMessage",
    "Displays a timer of how long it'll take before you can re-join the server",
    Categories.MISC,
    subcategory = "General",
) {
    private val SETTING_SEND_MESSAGE = addSwitch(
        "sendMessage",
        false,
        "Sends a message to the party chat whenever being kicked",
        "Send Message",
    )
    private val kickedMessageRegex = "^You were kicked while joining that server!$".toRegex()
    private var triggeredAt = 0L

    override fun initialize() {
        on<ChatEvent> { event ->
            if (event.matches(kickedMessageRegex) == null) return@on

            triggeredAt = System.currentTimeMillis() + 60_000

            if (!SETTING_SEND_MESSAGE.get()) return@on
            ChatUtils.command("pc kicked")
        }

        on<ClientThreadServerTickEvent> {
            if (triggeredAt == 0L) return@on

            val seconds = (triggeredAt - System.currentTimeMillis()) / 1000
            setLine("&c${StringUtils.formatSeconds(seconds)}")
            if (seconds <= 0) triggeredAt = 0L
        }

        on<RenderOverlayEvent> {
            if (triggeredAt == 0L) return@on

            draw(it.ctx)
        }
    }

    override fun getEditText(): List<String> = listOf("&c59s")
}