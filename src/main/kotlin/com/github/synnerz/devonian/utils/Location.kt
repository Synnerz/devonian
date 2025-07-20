package com.github.synnerz.devonian.utils

import com.github.synnerz.devonian.events.*
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket
import net.minecraft.network.packet.s2c.play.TeamS2CPacket

object Location {
    val teamRegex = "^team_(\\d+)$".toRegex()
    val emoteRegex = "[^\\u0000-\\u007F]".toRegex()
    val areaRegex = "^(?:Area|Dungeon): ([\\w ]+)\$".toRegex()
    val subAreaRegex = "^ (⏣|ф) .*".toRegex()
    var area: String? = null
    var subarea: String? = null

    fun initialize() {
        EventBus.on<PacketReceivedEvent> { event ->
            when (val packet = event.packet) {
                // TabList
                is PlayerListS2CPacket -> {
                    packet.entries.forEach {
                        val name = it.displayName ?: return@forEach
                        val line = name.string.replace(emoteRegex, "")
                        if (!line.matches(areaRegex)) return@forEach
                        val newArea = areaRegex.matchEntire(line)?.groupValues?.get(1) ?: return@forEach
                        if (newArea !== area) EventBus.post(AreaEvent(newArea))

                        area = newArea.lowercase()
                    }
                }
                // Scoreboard
                is TeamS2CPacket -> {
                    if (packet.team.isEmpty) return@on
                    val team = packet.team?.get() ?: return@on
                    val teamPrefix = team.prefix.string
                    val teamSuffix = team.suffix.string
                    if (teamPrefix.isEmpty()) return@on
                    val teamName = packet.teamName
                    if (!teamName.matches(teamRegex)) return@on

                    val line = "${teamPrefix}${teamSuffix}"
                    if (!line.matches(subAreaRegex)) return@on
                    if (line !== subarea) EventBus.post(SubAreaEvent(line))

                    subarea = line.lowercase()
                }
            }
        }

        EventBus.on<WorldChangeEvent> {
            if (area !== null) {
                EventBus.post(AreaEvent(null))
                area = null
            }
            if (subarea !== null) {
                EventBus.post(SubAreaEvent(null))
                subarea = null
            }
        }
    }
}