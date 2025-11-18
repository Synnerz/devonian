package com.github.synnerz.devonian.api.dungeon

import com.github.synnerz.devonian.api.events.EventBus
import com.github.synnerz.devonian.api.events.TabUpdateEvent
import com.github.synnerz.devonian.utils.Location

object Dungeons {
    // TODO: check if this matches with every possible tab name as well as class (like player being dead)
    private val playerInfoRegex = "^\\[\\d+] (\\w+)(?:.+?)? \\((\\w+) ?([IVXLCDM]+)?\\)$".toRegex()
    val partyMembers = mutableListOf<String>()

    fun initialize() {
        DungeonScanner.init()
    }

    init {
        EventBus.on<TabUpdateEvent> { event ->
            if (Location.area != "catacombs") return@on
            val match = event.matches(playerInfoRegex) ?: return@on
        EventBus.on<AreaEvent> { event ->
            val area = event.area
            if (area == null || area != "Catacombs") {
                players.clear()
                DungeonScanner.reset()
                DungeonMapScanner.reset()
            }
        }
    }
}