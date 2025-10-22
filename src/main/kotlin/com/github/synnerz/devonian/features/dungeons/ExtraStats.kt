package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.devonian.api.events.ChatEvent
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.utils.ChatUtils

object ExtraStats : Feature("showExtraStats", "catacombs") {
    private val extraStatsRegex = "^ *> EXTRA STATS <\$".toRegex()

    override fun initialize() {
        on<ChatEvent> { event ->
            if (event.matches(extraStatsRegex) == null) return@on
            ChatUtils.command("showextrastats")
        }
    }
}