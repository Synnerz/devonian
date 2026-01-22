package com.github.synnerz.devonian.features.misc

import com.github.synnerz.devonian.api.ChatUtils
import com.github.synnerz.devonian.api.events.ChatEvent
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.utils.StringUtils

object PreviousLobby : Feature(
    "previousLobby",
    "Alerts you whenever you join the same server (lobby) and tells you how long its been since you were last seen in it, if the time is above 60s it will be removed (from the list) after the alert."
) {
    private val lobbySwapRegex = "^Sending to server (\\w+)\\.\\.\\.$".toRegex()
    val previousLobbyList = mutableMapOf<String, Long>()

    override fun initialize() {
        on<ChatEvent> { event ->
            val match = event.matches(lobbySwapRegex) ?: return@on
            val ( serverId ) = match

            val savedAt = previousLobbyList[serverId]
            val t = System.currentTimeMillis()
            previousLobbyList[serverId] = t

            if (savedAt == null) return@on

            val timeSince = t - savedAt
            if (timeSince < 1000L) return@on

            ChatUtils.sendMessage("&cYou were in this server &b${StringUtils.formatTime(timeSince, 0)} &cago", true)
        }
    }
}