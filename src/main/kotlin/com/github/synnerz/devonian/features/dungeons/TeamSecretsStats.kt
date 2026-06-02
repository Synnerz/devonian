package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.devonian.api.ChatUtils
import com.github.synnerz.devonian.api.Party
import com.github.synnerz.devonian.api.Scheduler
import com.github.synnerz.devonian.api.dungeon.Dungeons
import com.github.synnerz.devonian.api.events.ChatEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.features.WebsocketClient

object TeamSecretsStats : Feature(
    "TeamSecretsStats",
    "Displays the amount of secrets done (if possible) at the end of a run of each team member (note: requires §bShowExtraStats§r and §bWebsocket§r to be enabled)",
    Categories.DUNGEONS,
    "catacombs",
    subcategory = "QOL",
    searchTags = setOf("secrets", "party secrets"),
) {
    private val SETTING_USE_ROLE_COLOR = addSwitch(
        "useRoleColor",
        true,
        "Uses the player's class colors to display its name",
        "Use Role Color"
    )
    private val floorStatsRegex = "^ *(Master Mode )?The Catacombs - (?:Floor ([IV]+)|Entrance)? Stats$".toRegex()
    private val secretsFoundRegex = "^ *Secrets Found: (\\d+)$".toRegex()
    private val socketSecretsDoneRegex = "^SecretTracker\\[(\\w{1,16}), (\\d+)]$".toRegex()
    private val memberSecrets = mutableMapOf<String, Int>()
    private val playerName get() = Dungeons.selfPlayer.name
    private var sent = false

    override fun initialize() {
        on<ChatEvent> { event ->
            event.matches(floorStatsRegex)?.let {
                Scheduler.scheduleTask(30) { sendMessage() }
                return@on
            }

            val match = event.matches(secretsFoundRegex) ?: return@on
            val secrets = match[0].toIntOrNull() ?: 0
            memberSecrets[playerName] = secrets
            WebsocketClient.send("SecretTracker[$playerName, $secrets]")

            if (memberSecrets.size == Party.members.size)
                Scheduler.scheduleTask(30) { sendMessage() }
        }

        on<WebsocketClient.MessageEvent> { event ->
            val secretsMatch = event.matches(socketSecretsDoneRegex) ?: return@on
            memberSecrets[secretsMatch[0]] = secretsMatch[1].toIntOrNull() ?: 0

            if (memberSecrets.size == Party.members.size)
                Scheduler.scheduleTask(30) { sendMessage() }
        }.setEnabled(WebsocketClient.configSwitch.state)
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        sent = false
    }

    private fun sendMessage() {
        if (sent) return
        sent = true

        memberSecrets.forEach { (name, secrets) ->
            val format =
                if (SETTING_USE_ROLE_COLOR.get())
                    Dungeons.playerClasses[name]?.colorCode ?: "&e"
                else
                    "&e"
            ChatUtils.sendMessage("$format$name &6$secrets &eSecrets", true)
        }

        memberSecrets.clear()
    }
}