package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.devonian.api.ChatUtils
import com.github.synnerz.devonian.api.events.ChatEvent
import com.github.synnerz.devonian.features.Feature

object ExtraStats : Feature(
    "showExtraStats",
    "Automatically calls the /showextrastats command at the end of a run.",
    "Dungeons",
    "catacombs"
) {
    private val extraStatsRegex = "^ *> EXTRA STATS <\$".toRegex()

    override fun initialize() {
        on<ChatEvent> { event ->
            if (event.matches(extraStatsRegex) == null) return@on
            ChatUtils.command("showextrastats")
        }
    }
}