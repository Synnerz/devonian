package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.devonian.api.ChatUtils
import com.github.synnerz.devonian.api.Scheduler
import com.github.synnerz.devonian.api.dungeon.DungeonScanner
import com.github.synnerz.devonian.api.dungeon.Dungeons
import com.github.synnerz.devonian.api.dungeon.FloorType
import com.github.synnerz.devonian.api.events.ChatEvent
import com.github.synnerz.devonian.api.events.TabUpdateEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.commands.DevonianCommand
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.utils.PersistentJsonClass
import com.google.gson.reflect.TypeToken
import java.time.LocalDateTime

object RunsLogger : Feature(
    "runsLogger",
    "Logs your completed dungeon runs (note: it will not work if you do not have ShowExtraStats enabled).",
    Categories.DUNGEONS,
    "catacombs",
    subcategory = "QOL",
) {
    private var dungeonsData = object : PersistentJsonClass<MutableMap<String, MutableMap<String, MutableList<RunStats>>>>(
        "devonian/runslogger.json",
        object : TypeToken<MutableMap<String, MutableMap<String, MutableList<RunStats>>>>() {}
    ) {
        override fun onLoadDefault() {
            data = mutableMapOf()
        }
    }
    private val floorStatsRegex = "^ *(Master Mode )?The Catacombs - (?:Floor ([IV]+)|Entrance)? Stats$".toRegex()
    private val teamScoreRegex = "^ *Team Score: (\\d+) \\((.{1,2})\\)(?: \\(NEW RECORD!\\))?$".toRegex()
    private val defeatedRegex = "^ *☠ Defeated [\\w, ]+ in ([\\dms ]+)( \\(NEW RECORD!\\))?$".toRegex()
    private val deathsRegex = "^ *Deaths: (\\d+)$".toRegex()
    private val secretsFoundRegex = "^ *Secrets Found: (\\d+)$".toRegex()
    private val milestoneRegex = "^ Your Milestone: .(.)\$".toRegex()
    private val milestonSymbols = listOf("⓿", "❶", "❷", "❸", "❹", "❺", "❻", "❼", "❽", "❾")
    private val localTime = LocalDateTime.now()
    private val currentDate = "${localTime.monthValue}/${localTime.dayOfMonth}/${localTime.year}"
    private var currentStat: RunStats? = null
    private var milestone = 0
    private var hasAdded = false

    data class RunStats(
        var secrets: Int = 0,
        var deaths: Int = 0,
        var time: String = "",
        var milestone: Int = 0,
        var score: Int = 0,
        var rank: String = "",
        var personalBest: Boolean = false,
        var currentParty: Map<String, String> = mapOf(),
        var currentDungeon: String = "",
        var snapshotAt: Long = -1L
    )

    override fun initialize() {
        dungeonsData.load()

        DevonianCommand.command.subcommand("runslogger") { _, args ->
            val floor = args.getOrNull(0) as? String?
            val date = args.getOrNull(1) as? String?
            if (floor.isNullOrEmpty()) {
                ChatUtils.sendMessage("&cRunsLogger no valid floor set", true)
                return@subcommand 0
            }
            if (date.isNullOrEmpty()) {
                ChatUtils.sendMessage("&cRunsLogger You did not set a valid date here are the current ones&7: &7${dungeonsData.data!!.keys.joinToString(", ")}", true)
                return@subcommand 0
            }

            val list = dungeonsData.data!![date]?.get(floor)
            if (list.isNullOrEmpty()) {
                ChatUtils.sendMessage("&cRunsLogger list for date \"$date\" and floor \"$floor\" is empty", true)
                return@subcommand 0
            }

            var sRuns = 0
            var sPlusRuns = 0
            var otherRuns = 0
            var relevantSecrets = 0
            var totalSecrets = 0

            list.forEach { v ->
                when (v.rank) {
                    "S" -> sRuns++
                    "S+" -> sPlusRuns++
                    else -> otherRuns++
                }
                if (v.rank == "S" || v.rank == "S+")
                    relevantSecrets += v.secrets
                totalSecrets += v.secrets
            }

            val spr = totalSecrets.toDouble() / (sRuns + sPlusRuns + otherRuns).toDouble()
            val sprRelevant = relevantSecrets.toDouble() / (sRuns + sPlusRuns).toDouble()

            ChatUtils.sendMessage("&bRunsLogger stats for &a$floor &b| &eS $sRuns &b| &6S+ $sPlusRuns &b| &5Other $otherRuns &b| &bSPR &a${"%.2f".format(spr)} &7(${"%.2f".format(sprRelevant)})", true)
            1
        }
            .word("floor")
            .suggest("floor", *listOf(
                "E", "F1", "F2", "F3", "F4", "F5", "F6", "F7",
                "M1", "M2", "M3", "M4", "M5", "M6", "M7"
            ).toTypedArray())
            .greedyString("date")
            .suggest("date", *listOf(
                "${localTime.monthValue}/${localTime.dayOfMonth}/${localTime.year}"
            ).toTypedArray())

        on<TabUpdateEvent> { event ->
            val match = event.matches(milestoneRegex) ?: return@on

            milestone = milestonSymbols.indexOf(match[0])
        }

        on<ChatEvent> { event ->
            event.matches(floorStatsRegex)?.let { _ ->
                if (currentStat != null) return@on
                currentStat = RunStats(
                    currentParty = buildMap {
                        Dungeons.players.forEach { (k, v) ->
                            put(v.name, v.role.shortName)
                        }
                    },
                    milestone = milestone,
                    snapshotAt = System.currentTimeMillis()
                )
                formatDungeonMap()?.let { currentStat?.currentDungeon = it }
                return@on
            }

            event.matches(teamScoreRegex)?.let {
                if (currentStat == null) return@on
                currentStat!!.score = it[0].toIntOrNull() ?: 0
                currentStat!!.rank = it[1]
                return@on
            }

            event.matches(defeatedRegex)?.let {
                if (currentStat == null) return@on
                currentStat!!.time = it[0]
                currentStat!!.personalBest = it.getOrNull(1)?.let { it == " (NEW RECORD!)" } ?: false
                return@on
            }

            event.matches(deathsRegex)?.let {
                if (currentStat == null) return@on
                currentStat!!.deaths = it[0].toIntOrNull() ?: 0
                return@on
            }

            val match = event.matches(secretsFoundRegex) ?: return@on
            val amount = match[0].toIntOrNull() ?: 0

            if (currentStat == null) {
                Scheduler.scheduleTask { ChatUtils.sendMessage("&cRunsLogger failed to properly scan the stats.", true) }
                return@on
            }
            currentStat!!.secrets = amount
            if (hasAdded) {
                currentStat = null
                return@on
            }

            dungeonsData.data!!
                .getOrPut(currentDate) { mutableMapOf() }
                .getOrPut(Dungeons.floor.shortName) { mutableListOf() }
                .add(currentStat!!)
            hasAdded = true
        }
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        currentStat = null
        hasAdded = false
    }

    private fun formatDungeonMap(): String? {
        var rooms = ""
        var doors = ""

        return buildString {
            DungeonScanner.rooms.toList().forEach {
                // bloom's logic has 998 for roomID = null, but it's not really used in his internal system
                // which breaks the map builder
                rooms += if (it?.roomID == null) "999" else "${it.roomID}".padStart(3, '0')
            }
            DungeonScanner.doors.toList().forEach {
                doors += if (it == null) "9" else "${it.type.ordinal}"
            }
            if (Dungeons.floor == FloorType.None || rooms.isEmpty() || doors.isEmpty()) return null
            append("${Dungeons.floor.shortName};${System.currentTimeMillis()};$rooms;$doors")
        }
    }
}