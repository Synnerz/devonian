package com.github.synnerz.devonian.features.dungeons.clear

import com.github.synnerz.devonian.api.ChatUtils
import com.github.synnerz.devonian.api.Location
import com.github.synnerz.devonian.api.Scheduler
import com.github.synnerz.devonian.api.WebRequests
import com.github.synnerz.devonian.api.dungeon.DungeonScanner
import com.github.synnerz.devonian.api.events.MouseReleaseEvent
import com.github.synnerz.devonian.api.events.RenderWorldEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.utils.PersistentJson
import com.github.synnerz.devonian.utils.StringUtils
import com.github.synnerz.devonian.utils.StringUtils.replaceCodes
import com.github.synnerz.devonian.utils.render.Render3DImmediate
import com.google.gson.reflect.TypeToken
import java.util.concurrent.TimeUnit
import kotlin.math.abs

object DevonianLeaderboard : Feature(
    "devonianLeaderboard",
    "Enables some features to show our leaderboard for record holders",
    Categories.GLOBAL,
    subcategory = "Mod"
) {
    private val f7Records = mutableListOf<NormalLeaderboardData>()
    private val m7Records = mutableListOf<NormalLeaderboardData>()
    private val soloF7ClearRecords = mutableListOf<SoloClearLeaderboardData>()
    private val soloM7ClearRecords = mutableListOf<SoloClearLeaderboardData>()
    private val cachedNames = hashMapOf<String, String>()
    private var currentCategory = LeaderboardCategory.F7
    private var currentSoloCategory = LeaderboardCategory.SOLO_F7
    private val normalDataType = object : TypeToken<List<NormalLeaderboardData>>() {}
    private val soloDataType = object : TypeToken<List<SoloClearLeaderboardData>>() {}

    data class NormalLeaderboardData(
        val teamName: String,
        val time: Int, // time in seconds
        val players: List<String>, // list of uuid for each player
        val hypixelLegit: Boolean, // is it hypixel legit?
        val displayNames: Boolean, // whether to display the names instead of the team name
        val uploadedAt: String = "0/0/0", // this can be left out empty, however the submitted date should be here in string
    )
    data class SoloClearLeaderboardData(
        val player: String, // the player's uuid
        val time: Int, // time in seconds
        val hypixelLegit: Boolean,
        val uploadedAt: String = "0/0/0",
    )
    data class PlayerDBData(
        val code: String,
        val message: String,
        val data: Map<String, Any>
    )

    enum class LeaderboardCategory(val formatted: String) {
        SOLO_F7("&cSoloF7"),
        SOLO_M7("&4SoloM7"),
        F7("&cF7"),
        M7("&4M7"),
    }

    init {
        Scheduler.schedulePool.scheduleWithFixedDelay({
            WebRequests.withName("FetchLeaderboard") {
                val f7 = WebRequests.get("https://raw.githubusercontent.com/Synnerz/devonianleaderboard/refs/heads/main/dungeons/f7/data.json")
                val m7 = WebRequests.get("https://raw.githubusercontent.com/Synnerz/devonianleaderboard/refs/heads/main/dungeons/m7/data.json")
                val soloF7 = WebRequests.get("https://raw.githubusercontent.com/Synnerz/devonianleaderboard/refs/heads/main/dungeons/soloclear/f7/data.json")
                val soloM7 = WebRequests.get("https://raw.githubusercontent.com/Synnerz/devonianleaderboard/refs/heads/main/dungeons/soloclear/m7/data.json")

                val f7res = PersistentJson.gson.fromJson(f7, normalDataType)?.sortedBy { it.time } ?: return@withName
                val m7res = PersistentJson.gson.fromJson(m7, normalDataType)?.sortedBy { it.time } ?: return@withName
                val soloF7Res = PersistentJson.gson.fromJson(soloF7, soloDataType)?.sortedBy { it.time } ?: return@withName
                val soloM7Res = PersistentJson.gson.fromJson(soloM7, soloDataType)?.sortedBy { it.time } ?: return@withName

                f7res.forEach { data ->
                    data.players.forEach { fetchUsername(it) }
                    f7Records.add(data)
                }

                m7res.forEach { data ->
                    data.players.forEach { fetchUsername(it) }
                    m7Records.add(data)
                }

                soloF7Res.forEach { data ->
                    fetchUsername(data.player)
                    soloF7ClearRecords.add(data)
                }

                soloM7Res.forEach { data ->
                    fetchUsername(data.player)
                    soloM7ClearRecords.add(data)
                }
            }
        }, 1L, 30L, TimeUnit.MINUTES)
    }

    override fun initialize() {
        on<RenderWorldEvent> {
            val player = minecraft.player ?: return@on

            Render3DImmediate.renderString(
                "${ChatUtils.prefix} ${currentCategory.formatted} &6S+ &eLeaderboard".replaceCodes(),
                -27.5, 125.0, -28.5,
                maxDist = 24.0,
                phase = player.y in 112.0..150.0
            )

            val list = when (currentCategory) {
                LeaderboardCategory.F7 -> f7Records
                LeaderboardCategory.M7 -> m7Records
                else -> return@on
            }
            list.forEachIndexed { idx, it ->
                if (idx > 9) return@forEachIndexed
                val format = forColor(idx)
                val emblem = emblem(idx)
                val legit = if (it.hypixelLegit) "" else "* "
                val name = if (it.displayNames) it.players.joinToString(" ") { cachedNames[it] ?: "NULL" } else it.teamName

                Render3DImmediate.renderString(
                    "$legit$format$emblem $format$name &e${StringUtils.formatSeconds(it.time.toLong())}".replaceCodes(),
                    -27.5, 124.5 - (if (idx == 0) 0.0 else idx * 0.5), -28.5,
                    maxDist = 24.0,
                    phase = player.y in 112.0..150.0
                )
            }
        }.setEnabled(Location.stateInArea("dungeon hub"))

        on<MouseReleaseEvent> { event ->
            if (event.button != 1) return@on
            val player = minecraft.player ?: return@on
            val dist = abs(player.x - -27.5) + abs(player.z - -28.5)
            if (dist > 5) return@on

            currentCategory = when (currentCategory) {
                LeaderboardCategory.F7 -> LeaderboardCategory.M7
                LeaderboardCategory.M7 -> LeaderboardCategory.F7
                else -> return@on
            }
            ChatUtils.sendMessage("&bSwitched leaderboard category to ${currentCategory.formatted}", true)
        }.setEnabled(Location.stateInArea("dungeon hub"))

        on<MouseReleaseEvent> { event ->
            if (event.button != 1) return@on
            val room = DungeonScanner.currentRoom ?: return@on
            if (room.roomID != 12) return@on
            val ( x, z ) = room.fromComp(25, 8) ?: return@on

            val player = minecraft.player ?: return@on
            val dist = abs(player.x - x) + abs(player.z - z)
            if (dist > 5) return@on

            currentSoloCategory = when (currentSoloCategory) {
                LeaderboardCategory.SOLO_F7 -> LeaderboardCategory.SOLO_M7
                LeaderboardCategory.SOLO_M7 -> LeaderboardCategory.SOLO_F7
                else -> return@on
            }
            ChatUtils.sendMessage("&bSwitched leaderboard category to ${currentSoloCategory.formatted}", true)
        }.setEnabled(Location.stateInArea("catacombs"))

        on<RenderWorldEvent> {
            val room = DungeonScanner.currentRoom ?: return@on
            if (room.roomID != 12) return@on
            val ( x, z ) = room.fromComp(25, 8) ?: return@on

            Render3DImmediate.renderString(
                "${ChatUtils.prefix} ${currentSoloCategory.formatted} &eLeaderboard".replaceCodes(),
                x + 0.5, 75.0, z + 0.5,
                maxDist = 24.0,
                phase = true,
            )

            val list = when (currentSoloCategory) {
                LeaderboardCategory.SOLO_F7 -> soloF7ClearRecords
                LeaderboardCategory.SOLO_M7 -> soloM7ClearRecords
                else -> return@on
            }
            list.forEachIndexed { idx, data ->
                if (idx > 9) return@forEachIndexed
                val format = forColor(idx)
                val emblem = emblem(idx)
                val legit = if (data.hypixelLegit) "" else "* "
                val name = cachedNames[data.player] ?: "NULL"

                Render3DImmediate.renderString(
                    "$legit$format$emblem $format$name &e${StringUtils.formatSeconds(data.time.toLong())}".replaceCodes(),
                    x + 0.5, 74.5 - (if (idx == 0) 0.0 else idx * 0.5), z + 0.5,
                    maxDist = 24.0,
                    phase = true,
                )
            }
        }.setEnabled(Location.stateInArea("catacombs"))

        configSwitch.set(true)
    }

    private fun emblem(idx: Int) = when (idx) {
        0 -> "\uD83E\uDD47" // gold
        1 -> "\uD83E\uDD48" // silver
        2 -> "\uD83E\uDD49" // bronze
        else -> "▷" // top >3
    }

    private fun forColor(idx: Int) = when (idx) {
        0 -> "&6"
        1 -> "&f"
        2 -> "&7"
        else -> "&8"
    }

    private suspend fun fetchUsername(uuid: String) {
        val res = WebRequests.get("https://playerdb.co/api/player/minecraft/$uuid")
        val json = PersistentJson.gson.fromJson(res, PlayerDBData::class.java)
        if (json.code != "player.found") return
        val name = (json.data["player"] as? Map<*, *>?)?.get("username") as? String? ?: return

        cachedNames[uuid] = name
    }
}