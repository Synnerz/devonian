package com.github.synnerz.devonian.features.garden

import com.github.synnerz.devonian.api.events.ChatEvent
import com.github.synnerz.devonian.api.events.ClientThreadServerTickEvent
import com.github.synnerz.devonian.api.events.RenderOverlayEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.hud.texthud.TextHudFeature

object CropFeverTimer : TextHudFeature(
    "cropFeverTimer",
    "Displays a timer whenever you proc Crop Fever enchant",
    Categories.GARDEN,
    "garden"
) {
    private val procRegex = "^WOAH! You caught a case of the CROP FEVER for 60 seconds!$".toRegex()
    private val overRegex = "^GONE! Your CROP FEVER has been cured!$".toRegex()
    private var timer = -1L

    override fun initialize() {
        on<ChatEvent> { event ->
            if (event.matches(procRegex) != null) {
                timer = System.currentTimeMillis() + 60_000
                return@on
            }

            event.matches(overRegex) ?: return@on
            timer = -1L
        }

        on<ClientThreadServerTickEvent> {
            if (timer == -1L) return@on
            val ms = timer - System.currentTimeMillis()
            val seconds = ms / 1000.0

            setLine("&d&l${"%.2fs".format(seconds)}")

            if (ms < 0) timer = -1L
        }

        on<RenderOverlayEvent> {
            if (timer == -1L) return@on
            draw(it.ctx)
        }
    }

    override fun getEditText(): List<String> = listOf("&d&l60.00s")
}