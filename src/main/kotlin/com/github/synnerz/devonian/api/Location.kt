package com.github.synnerz.devonian.api

import com.github.synnerz.devonian.api.events.*
import com.github.synnerz.devonian.commands.DevonianCommand
import com.github.synnerz.devonian.utils.BasicState
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket

object Location {
    val areaRegex = "^(?:Area|Dungeon): ([\\w ']+)\$".toRegex()
    val subAreaRegex = "^ ([⏣ф]) ".toRegex()
    var area: String? = null
    var subarea: String? = null
    val stateArea = BasicState<String?>(null)
    val stateSubarea = BasicState<String?>(null)
    val stateInLatestArea = stateInArea(null, "the park", "galatea")
    val stateInSkyblock = stateArea.map { it != null }

    fun stateInArea(vararg area: String?) = stateArea.map { area.contains(it) }
    fun stateInSubarea(vararg subarea: String?) = stateSubarea.map { subarea.contains(it) }

    fun changeArea(loc: String) {
        val old = area
        val l = loc.lowercase()
        if (old === l) return

        AreaEvent(l).post()
        area = l
        stateArea.value = l
    }

    fun changeSubarea(loc: String) {
        val old = subarea
        val l = loc.lowercase()
        if (old == l) return

        SubAreaEvent(l).post()
        subarea = l
        stateSubarea.value = l
    }

    fun initialize() {
        EventBus.on<TabUpdateEvent> { event ->
            val newArea = event.matches(areaRegex)?.getOrNull(0) ?: return@on

            changeArea(newArea)
        }

        EventBus.on<ScoreboardEvent> { event ->
            if (!subAreaRegex.matchesAt(event.message, 0)) return@on

            changeSubarea(event.message.drop(3))
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

        DevonianCommand.command.subcommand("area") { _, args ->
            val str = (args.firstOrNull() ?: return@subcommand 0) as String
            changeArea(str)
            ChatUtils.sendMessage("&aPosting area event with str &6$str", true)
            1
        }.string("name")

        DevonianCommand.command.subcommand("subarea") { _, args ->
            val str = (args.firstOrNull() ?: return@subcommand 0) as String
            changeSubarea(str)
            ChatUtils.sendMessage("&aPosting subarea event with str &6$str", true)
            1
        }.string("name")
    }
}