package com.github.synnerz.devonian.features.misc

import com.github.synnerz.devonian.api.ChatUtils
import com.github.synnerz.devonian.api.events.ChatEvent
import com.github.synnerz.devonian.features.Feature

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

            if (savedAt != null) {
                val timeSince = System.currentTimeMillis() - savedAt
                val seconds = "%.2fs".format((timeSince / 1000).toFloat())

                if (timeSince >= 60000) previousLobbyList.remove(serverId)

                ChatUtils.sendMessage("&cYou were in this server &b${seconds} &cago", true)
                return@on
            }

            previousLobbyList[serverId] = System.currentTimeMillis()
        }
    }
}