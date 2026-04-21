package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.devonian.api.ChatUtils
import com.github.synnerz.devonian.api.HypixelModApi
import com.github.synnerz.devonian.api.Party
import com.github.synnerz.devonian.api.dungeon.Dungeons
import com.github.synnerz.devonian.api.events.ChatChannelEvent
import com.github.synnerz.devonian.api.events.ChatEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import java.util.*
import java.util.concurrent.ConcurrentHashMap

object AutoRequeueDungeons : Feature(
    "autoRequeueDungeons",
    "Automatically runs the /instancerequeue command at the end of a run.",
    Categories.DUNGEONS,
    subcategory = "QOL",
) {
    private val extraStatsRegex = "^ *> EXTRA STATS <\$".toRegex()
    private var needsDowntime = Collections.newSetFromMap<String>(ConcurrentHashMap())
    private var lastQueue = 0
    private var startedRun = false

    private fun requeue() {
        if (!Party.isLeader && Party.inParty) return
        if (lastQueue > 0 && lastQueue != Party.members.size) {
            lastQueue = 0
            return
        }

        ChatUtils.command("instancerequeue")
        lastQueue = 0
    }

    override fun initialize() {
        Dungeons.timeElapsed.listen {
            if (it <= 0 || startedRun) return@listen

            startedRun = true
            HypixelModApi.requestPartyInfo()
            lastQueue = Party.members.size
        }

        on<ChatChannelEvent.PartyChatEvent> { event ->
            val msg = event.userMessage.lowercase()
            if (msg == "r" || msg == "!r") {
                if (!needsDowntime.remove(event.name)) return@on

                ChatUtils.sendMessage("&a${event.name} is ready", true)
                if (needsDowntime.isEmpty()) requeue()
            } else if (msg == "dt" || msg.startsWith("!dt")) {
                if (needsDowntime.add(event.name)) {
                    ChatUtils.sendMessage("&bUser &6${event.name} &bneeds downtime", true)
                }
            }
        }

        on<ChatEvent> { event ->
            if (!extraStatsRegex.matches(event.message)) return@on

            if (needsDowntime.isEmpty()) requeue()
            else ChatUtils.command("pc ${needsDowntime.joinToString(", ")} needs downtime")
        }
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        startedRun = false
        lastQueue = 0

        if (needsDowntime.isEmpty()) return
        ChatUtils.sendMessage("&aDowntime has been reset.", true)
        needsDowntime.clear()
    }
}