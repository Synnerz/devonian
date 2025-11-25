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
    val stateArea = BasicState<String?>(null)
    val stateSubarea = BasicState<String?>(null)

    fun stateInArea(vararg area: String?) = stateArea.map { area.contains(it) }
    fun stateInSubarea(vararg subarea: String?) = stateSubarea.map { subarea.contains(it) }

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
                        if (newArea !== area) AreaEvent(newArea).post()

                        area = newArea.lowercase()
                        stateArea.value = area
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
                    if (line !== oldSubarea) SubAreaEvent(line).post()

                    subarea = line.lowercase()
                    stateSubarea.value = subarea
                }
            }
        }

        EventBus.on<WorldChangeEvent> {
            if (area !== null) {
                AreaEvent(null).post()
                area = null
                stateArea.value = null
            }
            if (subarea !== null) {
                SubAreaEvent(null).post()
                subarea = null
                stateSubarea.value = null
            }
        }
    }
}