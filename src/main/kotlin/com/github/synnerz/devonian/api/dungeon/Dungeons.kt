package com.github.synnerz.devonian.api.dungeon

import com.github.synnerz.devonian.api.events.EventBus
import com.github.synnerz.devonian.api.events.TabUpdateEvent
import com.github.synnerz.devonian.utils.Location

object Dungeons {
    // TODO: check if this matches with every possible tab name as well as class (like player being dead)
    private val playerInfoRegex = "^\\[\\d+] (\\w+)(?:.+?)? \\((\\w+) ?([IVXLCDM]+)?\\)$".toRegex()
    val partyMembers = mutableListOf<String>()

//    data class PlayerInfoData(val name: String, val className: String, val classLevel: Int)

    init {
        EventBus.on<TabUpdateEvent> { event ->
            if (Location.area != "catacombs") return@on
            val match = event.matches(playerInfoRegex) ?: return@on
            val name = match[0]

            if (partyMembers.contains(name)) return@on

            partyMembers.add(name)
        }
    }
}