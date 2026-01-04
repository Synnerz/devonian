package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.devonian.api.ChatUtils
import com.github.synnerz.devonian.api.events.ChatChannelEvent
import com.github.synnerz.devonian.api.events.ChatEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import java.util.*
import java.util.concurrent.ConcurrentHashMap

object AutoRequeueDungeons : Feature(
    "autoRequeueDungeons",
    "Automatically calls the /instancerequeue command at the end of a run.",
    Categories.DUNGEONS,
    subcategory = "QOL",
) {
    private val extraStatsRegex = "^ *> EXTRA STATS <\$".toRegex()
    private var needsDowntime = Collections.newSetFromMap<String>(ConcurrentHashMap())

    private fun requeue() {
        ChatUtils.command("instancerequeue")
    }

    override fun initialize() {
        on<ChatChannelEvent.PartyChatEvent> { event ->
            val msg = event.userMessage.lowercase()
            if (msg == "r") {
                if (!needsDowntime.remove(event.name)) return@on

                ChatUtils.sendMessage("&a${event.name} is ready", true)
                if (needsDowntime.isEmpty()) requeue()
            } else if (msg.startsWith("dt") || msg.startsWith("!dt")) {
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
        if (needsDowntime.isEmpty()) return
        ChatUtils.sendMessage("&aDowntime has been reset.", true)
        needsDowntime.clear()
    }
}