package com.github.synnerz.devonian.features.misc

import com.github.synnerz.devonian.api.ChatUtils
import com.github.synnerz.devonian.api.Ping
import com.github.synnerz.devonian.api.events.RenderOverlayEvent
import com.github.synnerz.devonian.commands.DevonianCommand
import com.github.synnerz.devonian.hud.texthud.TextHudFeature

object PingDisplay : TextHudFeature(
    "pingDisplay",
    ""
) {
    private val SETTING_PING_TYPE = addSelection(
        "pingType",
        0,
        listOf("Median", "Mean", "Instantaneous"),
        "",
        "Ping Type",
    )

    fun formatPing(n: Double) = "${
        when {
            n < 50.0 -> "&a"
            n < 100.0 -> "&2"
            n < 150.0 -> "&e"
            n < 200.0 -> "&6"
            else -> "&c"
        }
    }%.2f &7ms".format(n)

    override fun initialize() {
        DevonianCommand.command.subcommand("ping") { _, _ ->
            // TODO: send out another statistics/ping (hypixeapi) packet
            val cur = formatPing(Ping.getLastPing())
            val avg = formatPing(Ping.getMedianPing())
            ChatUtils.sendMessage("Ping: $cur, Avg: $avg", withPrefix = true)
            1
        }

        on<RenderOverlayEvent> { event ->
            setLine("&aPing: ${formatPing(when (SETTING_PING_TYPE.getCurrent()) {
                "Median" -> Ping.getMedianPing()
                "Mean" -> Ping.getAveragePing()
                "Instantaneous" -> Ping.getLastPing()
                else -> Ping.getMedianPing()
            })}")
            draw(event.ctx)
        }
    }

    override fun getEditText(): List<String> = listOf("&aPing: ${formatPing(69.42)}")
}