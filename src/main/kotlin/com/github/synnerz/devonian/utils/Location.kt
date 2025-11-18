package com.github.synnerz.devonian.utils

import com.github.synnerz.devonian.api.events.*
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket

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
                is ClientboundPlayerInfoUpdatePacket -> {
                    packet.entries().forEach {
                        val name = it.displayName ?: return@forEach
                        val line = name.string.replace(emoteRegex, "")
                        if (!line.matches(areaRegex)) return@forEach
                        val newArea = areaRegex.matchEntire(line)?.groupValues?.get(1) ?: return@forEach
                        if (newArea !== area) EventBus.post(AreaEvent(newArea))

                        area = newArea.lowercase()
                    }
                }
                // Scoreboard
                is ClientboundSetPlayerTeamPacket -> {
                    if (packet.parameters.isEmpty) return@on
                    val team = packet.parameters?.get() ?: return@on
                    val teamPrefix = team.playerPrefix.string
                    val teamSuffix = team.playerSuffix.string
                    if (teamPrefix.isEmpty()) return@on
                    val teamName = packet.name
                    if (!teamName.matches(teamRegex)) return@on

                    val line = "${teamPrefix}${teamSuffix}"
                    if (!line.matches(subAreaRegex)) return@on
                    val oldSubarea = subarea

                    subarea = line.lowercase()
                    if (line !== oldSubarea) EventBus.post(SubAreaEvent(line))
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