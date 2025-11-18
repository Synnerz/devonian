package com.github.synnerz.devonian.api.dungeon

import com.github.synnerz.devonian.Devonian
import com.github.synnerz.devonian.api.events.AreaEvent
import com.github.synnerz.devonian.api.events.EventBus
import com.github.synnerz.devonian.api.events.TabUpdateEvent
import com.github.synnerz.devonian.api.events.TickEvent
import com.github.synnerz.devonian.utils.Location
import com.github.synnerz.devonian.utils.StringUtils

object Dungeons {
    private val playerInfoRegex = "^\\[\\d+] (\\w+)(?:.+?)? \\((\\w+) ?([IVXLCDM]+)?\\)$".toRegex()
    val players = linkedMapOf<String, DungeonPlayer>()

    fun initialize() {
        DungeonScanner.init()
    }

    init {
        EventBus.on<TabUpdateEvent> { event ->
            if (Location.area != "catacombs") return@on
            val match = event.matches(playerInfoRegex) ?: return@on
            val (name, role) = match

            val player = players.getOrPut(name) {
                DungeonPlayer(
                    name,
                    DungeonClass.Unknown,
                    0,
                    false
                )
            }

            if (role == "DEAD") player.isDead = true
            else {
                player.isDead = false
                player.role = DungeonClass.from(role)

                val level = match.getOrNull(2)
                if (level != null) player.classLevel = StringUtils.parseRoman(level)
            }
        }

        EventBus.on<TickEvent> {
            if (Location.area != "catacombs") return@on
            val mc = Devonian.minecraft

            players.forEach { it.value.tick() }

            mc.world?.players?.forEach {
                val ping = mc.networkHandler?.getPlayerListEntry(it.uuid)?.latency ?: return@forEach
                if (ping == -1) return@forEach

                val player = players[it.name.string] ?: return@forEach
                player.entity = it
            }
        }

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