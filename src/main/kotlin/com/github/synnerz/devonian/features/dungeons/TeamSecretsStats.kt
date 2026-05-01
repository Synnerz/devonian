package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.devonian.api.ChatUtils
import com.github.synnerz.devonian.api.Scheduler
import com.github.synnerz.devonian.api.dungeon.Dungeons
import com.github.synnerz.devonian.api.dungeon.DungeonsApi
import com.github.synnerz.devonian.api.events.ChatEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

object TeamSecretsStats : Feature(
    "TeamSecretsStats",
    "Displays the amount of secrets done (if possible) at the end of a run of each team member (note: requires §bShowExtraStats§r to be enabled. if the last fetch failed for a player it might add up the last secrets)",
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
    private val startingRunRegex = "^Starting in 2 seconds\\.$".toRegex()
    private val secretsFoundRegex = "^ *Secrets Found: (\\d+)$".toRegex()
    private val cachedSecrets = ConcurrentHashMap<String, Int>()
    private val memberSecrets = ConcurrentHashMap<String, Int>()
    private val requesting = CopyOnWriteArrayList<String>()
    private val playerName get() = Dungeons.selfPlayer.name
    private var requested = 0

    override fun initialize() {
        DungeonsApi.on { name, data ->
            if (!requesting.contains(name)) return@on
            onData(data, name)
            requesting.remove(name)
        }

        on<ChatEvent> { event ->
            event.matches(startingRunRegex)?.let {
                requestData()
                return@on
            }
            event.matches(secretsFoundRegex)?.let {
                val secrets = it.first().toIntOrNull() ?: 0
                memberSecrets[playerName] = secrets
                requested++
                if (requested == Dungeons.players.size)
                    onRequested()
                return@on
            }
            if (event.matches(floorStatsRegex) == null) return@on
            requestData()
        }
    }

    private fun requestData() {
        Dungeons.players.forEach { (name, _) ->
            if (name == playerName) return@forEach
            val response = DungeonsApi.player(name, 2)
            if (response == null || System.currentTimeMillis() - response.timeTaken >= 1000 * 60 * 2) {
                requesting.add(name)
                DungeonsApi.requestPlayer(name)
                return@forEach
            }
            onData(response, name, true)
        }
    }

    private fun onData(data: DungeonsApi.DungeonsApiResult, name: String, isCache: Boolean = false) {
        val secrets = cachedSecrets[name]
        if (secrets != null) {
            memberSecrets[name] = if (isCache && data.secrets() == secrets) -1 else data.secrets() - secrets
            if (Dungeons.timeElapsed.value > 0)
                requested++
        }
        cachedSecrets[name] = data.secrets()
        if (requested == Dungeons.players.size)
            onRequested()
    }

    private fun onRequested() {
        if (!memberSecrets.containsKey(playerName)) return

        requested = 0
        Scheduler.scheduleTask(10) {
            memberSecrets.forEach { (name, secrets) ->
                val format =
                    if (SETTING_USE_ROLE_COLOR.get())
                        Dungeons.playerClasses[name]?.colorCode ?: "&e"
                    else
                        "&e"
                val _secrets = if (secrets == -1) 0 else secrets
                val strike = if (secrets == -1) "&m" else ""
                ChatUtils.sendMessage("$format${strike}$name &6$strike$_secrets &e${strike}Secrets", true)
            }
        }
    }
}