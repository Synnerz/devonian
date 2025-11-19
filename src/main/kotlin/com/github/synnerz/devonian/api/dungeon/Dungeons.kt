package com.github.synnerz.devonian.api.dungeon

import com.github.synnerz.devonian.Devonian
import com.github.synnerz.devonian.api.events.*
import com.github.synnerz.devonian.utils.Location
import com.github.synnerz.devonian.utils.StringUtils

object Dungeons {
    private val playerInfoRegex = "^\\[\\d+] (\\w+)(?:.+?)? \\((\\w+) ?([IVXLCDM]+)?\\)$".toRegex()
    private val dungeonFloorRegex = "^ * ⏣ The Catacombs \\((\\w+)\\)$".toRegex()
    private val clearedPercentRegex = "^Cleared: (\\d+)% \\(\\d+\\)\$".toRegex()
    private val timeElapsedRegex = "^Time Elapsed: (?:(\\d+)h)? ?(?:(\\d+)m)? ?(\\d+)s\$".toRegex()
    private val teamDeathsRegex = "^Team Deaths: (\\d+)$".toRegex()
    private val cryptsRegex = "^ Crypts: (\\d+)$".toRegex()
    private val secretsFoundPercentRegex = "^ Secrets Found: ([\\d.]+)%\$".toRegex()
    private val secretsFoundRegex = "^ Secrets Found: (\\d+)\$".toRegex()
    private val completedRoomsRegex = "^ Completed Rooms: (\\d+)\$".toRegex()
    private val openedRoomsRegex = "^ Opened Rooms: (\\d+)\$".toRegex()
    private val discoveriesRegex = "^Discoveries: (\\d+)$".toRegex()
    private val puzzleNameRegex = "^ ([\\w ]+): \\[(✦|✔|✖)\\] ?\\(?(\\w+)?\\)?\$".toRegex()
    private val puzzlesCountRegex = "^Puzzles: \\((\\d+)\\)\$".toRegex()
    val players = linkedMapOf<String, DungeonPlayer>()
    var floor = FloorType.None
    var clearedPercent = 0
    var timeElapsed = 0
    var deaths = 0
    var crypts = 0
    var secretsFoundPercent = 0.0
    var secretsFound = 0
    var completedRooms = 0
    var openedRooms = 0
    var discoveries = 0
    var totalPuzzles = 0
    var completedPuzzles = 0
    val bloodDone = false
    val bossEntered = false
    val mimicKilled = false
    var score = 0

    fun initialize() {
        DungeonScanner.init()
    }

    init {
        EventBus.on<TabUpdateEvent> { event ->
            if (Location.area != "catacombs") return@on

            event.matches(cryptsRegex)?.let {
                crypts = it[0].toInt()
                return@on
            }

            event.matches(teamDeathsRegex)?.let {
                deaths = it[0].toInt()
                return@on
            }

            event.matches(secretsFoundPercentRegex)?.let {
                secretsFoundPercent = it[0].toDouble()
                return@on
            }

            event.matches(completedRoomsRegex)?.let {
                completedRooms = it[0].toInt()
                return@on
            }

            event.matches(secretsFoundRegex)?.let {
                secretsFound = it[0].toInt()
                return@on
            }

            event.matches(openedRoomsRegex)?.let {
                openedRooms = it[0].toInt()
                return@on
            }

            event.matches(discoveriesRegex)?.let {
                discoveries = it[0].toInt()
                return@on
            }

            event.matches(puzzlesCountRegex)?.let {
                totalPuzzles = it[0].toInt()
                return@on
            }

            event.matches(puzzleNameRegex)?.let {
                if (it[1].isEmpty() || it[1] != "✔") return@on
                completedPuzzles++.coerceIn(0, totalPuzzles)
                return@on
            }

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

            mc.level?.players()?.forEach {
                val ping = mc.connection?.getPlayerInfo(it.uuid)?.latency ?: return@forEach
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
                reset()
            }
        }

        EventBus.on<SubAreaEvent> { event ->
            val subarea = event.subarea ?: return@on
            val match = dungeonFloorRegex.matchEntire(subarea) ?: return@on
            val name = match.groups[1]?.value ?: return@on

            floor = FloorType.from(name)
        }

        EventBus.on<ScoreboardEvent> { event ->
            val matchesTime = event.matches(timeElapsedRegex)
            if (matchesTime != null) {
                val hours = matchesTime[0].ifEmpty { "0" }.toInt()
                val minutes = matchesTime[1].ifEmpty { "0" }.toInt()
                val seconds = matchesTime[2].ifEmpty { "0" }.toInt()
                timeElapsed = hours * 3600 + minutes * 60 + seconds
                return@on
            }

            val match = event.matches(clearedPercentRegex) ?: return@on
            clearedPercent = match[0].toInt()
        }
    }

    private fun reset() {
        clearedPercent = 0
        timeElapsed = 0
        deaths = 0
        crypts = 0
        secretsFoundPercent = 0.0
        completedRooms = 0
        openedRooms = 0
        discoveries = 0
        totalPuzzles = 0
        completedPuzzles = 0
        score = 0
    }
}