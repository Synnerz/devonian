package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.devonian.api.ChatUtils
import com.github.synnerz.devonian.api.events.ChatEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.utils.Location

object AutoRequeueDungeons : Feature(
    "autoRequeueDungeons",
    "Automatically calls the /instancerequeue command at the end of a run.",
    "Dungeons"
) {
    private val extraStatsRegex = "^ *> EXTRA STATS <\$".toRegex()
    private val partyChatRegex = "^Party > (?:\\[\\d+] .? ?)?(?:\\[[^]]+] )?(\\w{1,16}): !(\\w{1,2})(?: [\\w ]+)?$".toRegex()
    private var needsDowntime: String? = null

    override fun initialize() {
        on<ChatEvent> { event ->
            val partyMatch = event.matches(partyChatRegex)
            if (partyMatch != null) {
                val (name, msg) = partyMatch
                when (msg.lowercase()) {
                    "r" -> {
                        ChatUtils.sendMessage("&a$needsDowntime is ready", true)
                        needsDowntime = null
                        return@on
                    }
                    "dt" -> needsDowntime = name
                }
                ChatUtils.sendMessage("&bUser &6$needsDowntime &bneeds downtime", true)
                return@on
            }
            if (Location.area != "catacombs" || event.matches(extraStatsRegex) == null) return@on
            if (needsDowntime != null) {
                ChatUtils.command("pc $needsDowntime needs downtime")
                return@on
            }
            ChatUtils.command("instancerequeue")
        }

        on<WorldChangeEvent> {
            if (needsDowntime == null) return@on
            ChatUtils.sendMessage("&aDowntime has been resetted.", true)
            needsDowntime = null
        }
    }
}