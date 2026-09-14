package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.devonian.api.ChatUtils
import com.github.synnerz.devonian.api.Scheduler
import com.github.synnerz.devonian.api.dungeon.DungeonClass
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
import com.github.synnerz.devonian.utils.StringUtils
import com.google.gson.reflect.TypeToken
import java.time.LocalDateTime

object RunsLogger : Feature(
    "runsLogger",
    "Logs your completed dungeon runs (note: it will not work if you do not have ShowExtraStats enabled). /dv runslogger <mode> <floor> <date>",
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
            val mode = args.getOrNull(0) as? String?
            val floor = args.getOrNull(1) as? String?
            val dates = (args.getOrNull(2) as? String?)?.replace("*", "")
            val date = dates?.split(" ")?.getOrNull(0)
            val date2 = dates?.split(" ")?.getOrNull(1)
            if (mode.isNullOrEmpty()) {
                ChatUtils.sendMessage("&cRunsLogger not a valid mode was set", true)
                return@subcommand 0
            }
            if (floor.isNullOrEmpty()) {
                ChatUtils.sendMessage("&cRunsLogger no valid floor set", true)
                return@subcommand 0
            }
            if (date.isNullOrEmpty()) {
                ChatUtils.sendMessage("&cRunsLogger You did not set a valid date here are the current ones&7: &7${dungeonsData.data!!.keys.joinToString(", ")}", true)
                return@subcommand 0
            }

            val list =
                if (date2.isNullOrEmpty())
                    dungeonsData.data!![date]?.get(floor)
                else {
                    buildList {
                        val fromDate = date.split("/")
                        val fromMM = fromDate.getOrNull(0)?.toIntOrNull()
                        val fromDD = fromDate.getOrNull(1)?.toIntOrNull()
                        val fromYY = fromDate.getOrNull(2)?.toIntOrNull()
                        if (fromMM == null || fromDD == null || fromYY == null) {
                            ChatUtils.sendMessage("&cRunsLogger You did not provide a valid \"from\" date the correct order should be \"MM/DD/YY\"", true)
                            return@subcommand 0
                        }
                        val toDate = date2.split("/")
                        val toMM = toDate.getOrNull(0)?.toIntOrNull() ?: fromMM
                        val toDD = toDate.getOrNull(1)?.toIntOrNull() ?: (fromDD + 1)
                        val toYY = toDate.getOrNull(2)?.toIntOrNull() ?: fromYY

                        var foundStartDay = false
                        for (year in fromYY..toYY) {
                            for (month in fromMM..toMM) {
                                for (day in 1..31) {
                                    if (month == fromMM && day == fromDD) foundStartDay = true
                                    if (!foundStartDay) continue

                                    dungeonsData.data!!["$month/$day/$year"]?.get(floor)?.forEach {
                                        add(it)
                                    }
                                    if (day == toDD && month == toMM) break
                                }
                            }
                        }
                    }
                }
            if (list.isNullOrEmpty()) {
                ChatUtils.sendMessage("&cRunsLogger list for date \"$date\" and floor \"$floor\" is empty", true)
                return@subcommand 0
            }

            if (mode == "PLAYERS") {
                val players = mutableMapOf<String, Int>()

                for (idx in 0..list.lastIndex) {
                    val run = list.getOrNull(idx) ?: continue

                    run.currentParty.forEach { (name, role) ->
                        if (!players.containsKey(name)) players[name] = 0

                        players[name] = players[name]!! + 1
                    }
                }

                // TODO: maybe add a list of classes they've played
                ChatUtils.sendMessage("&bRunsLogger player stats for &a$floor", true)
                players.entries
                    // TODO: add filter % base maybe
//                    .filter { it.value > 10 }
                    .sortedByDescending { it.value }.forEach { (name, count) ->
                    ChatUtils.sendMessage("&7- &6$name&f: &e$count")
                }
                return@subcommand 1
            }

            if (mode == "FASTEST") {
                val run = list.filter { it.rank == "S+" }.minByOrNull { StringUtils.parseTimer(it.time) } ?: return@subcommand 0
                val roles = run.currentParty.map { it.key to DungeonClass.from(it.value.lowercase()[0]) }

                ChatUtils.sendMessage("&bRunsLogger fastest &6S+&b for" +
                        " &a$floor" +
                        " &dTime&f: &6${run.time}" +
                        roles.joinToString { " &e[${it.second.colorCode}${it.second.singleLetter.uppercase()}&e] ${it.second.colorCode}${it.first}&r" } +
                        " &eSecrets&f: &6${run.secrets}" +
                        if (run.deaths > 0) " &4Deaths&f: &c${run.deaths}"
                        else "",
                    true)
                return@subcommand 1
            }

            if (mode == "STATS") {
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

                ChatUtils.sendMessage("&bRunsLogger stats for" +
                        " &a$floor" +
                        " &b| &eS $sRuns" +
                        " &b| &6S+ $sPlusRuns" +
                        " &b| &5Other $otherRuns" +
                        " &b| &bSPR &a${"%.2f".format(spr)} &7(${"%.2f".format(sprRelevant)})",
                    true)
                return@subcommand 1
            }
            if (mode != "TIME") {
                ChatUtils.sendMessage("&cRunsLogger not a valid mode was set", true)
                return@subcommand 0
            }

            var lastFastest = 0L
            var realTime = 0L
            var runsTime = 0L

            for (idx in 0..list.lastIndex) {
                val current = list.getOrNull(idx) ?: continue
                val time = StringUtils.parseTimer(current.time)
                if (lastFastest == 0L || lastFastest > time)
                    lastFastest = time.toLong()
                runsTime += time

                if (idx == list.lastIndex) break
                val next = list.getOrNull(idx + 1) ?: continue

                realTime += next.snapshotAt - current.snapshotAt
            }

            ChatUtils.sendMessage("&bRunsLogger time for" +
                    " &a$floor" +
                    " &b| &dFastest ${StringUtils.formatSeconds(lastFastest)}" +
                    " &b| &6Avg ${StringUtils.formatSeconds(runsTime / list.size - 1)}" +
                    " &b| &eTime ${StringUtils.formatSeconds(runsTime)} &7(${StringUtils.formatSeconds(realTime / 1000)})",
            true)
            1
        }
            .word("mode")
            .word("floor")
            .suggest("mode", *listOf(
                "*STATS",
                "TIME",
                "PLAYERS",
                "FASTEST",
            ).toTypedArray())
            .suggest("floor", *listOf(
                "E", "F1", "F2", "F3", "F4", "F5", "F6", "F7",
                "M1", "M2", "M3", "M4", "M5", "M6", "M7"
            ).toTypedArray())
            .greedyString("date")
            .suggest("date") {
                buildList {
                    val current = "*${localTime.monthValue}/${localTime.dayOfMonth}/${localTime.year}"

                    add(current)

                    dungeonsData.data!!.mapNotNull {
                        if (it.key == current) null
                        else it.key
                    }.forEach {
                        add(it)
                    }
                }.toMutableList()
            }

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