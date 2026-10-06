package com.github.synnerz.devonian.api.dungeon

import com.github.synnerz.devonian.api.ChatUtils
import com.github.synnerz.devonian.api.Scheduler
import com.github.synnerz.devonian.api.WebRequests
import com.github.synnerz.devonian.utils.PersistentJson
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

object DungeonsApi {
    private const val DUNGEONS_API = "https://api.docilelm.top/v2/dungeons/"
    private val playerQueue = LinkedHashMap<String, MutableList<(DungeonsApiResult) -> Unit>>()
    private val playerData = ConcurrentHashMap<String, DungeonsApiResult>()

    data class UserDungeonsData(
        val cataXP: Double,
        val level: Double,
        val roles: Map<String, Map<String, Double>>,
        val secrets: Int,
        val averageSecrets: Double,
        val spirit: Map<String, String?>,
        val goldenDragon: List<Map<String, String?>>, // only has heldItem
        val enderDragon: List<Map<String, String?>>,
        val magical_power: Int,
        val personal_best_normal: Map<String, Map<String, String>>?, // { s: { "floor_1": "1:15" }, s_plus: { "floor_1": "1:15" } }
        val personal_best_master: Map<String, Map<String, String>>?,
    ) {
        companion object {
            val EMPTY = UserDungeonsData(
                0.0,
                0.0,
                emptyMap(),
                0,
                0.0,
                emptyMap(),
                emptyList(),
                emptyList(),
                0,
                null,
                null,
            )
        }
    }
    data class DungeonsApiResult(
        val success: Boolean,
        val status: String,
        val data: UserDungeonsData?
    ) {
        var snapshotTime = 0L

        fun cataXP(): Double = data?.cataXP ?: 0.0

        fun level(): Double = data?.level ?: 0.0

        fun roles(): Map<String, Map<String, Double>> = data?.roles ?: emptyMap()

        fun secrets(): Int = data?.secrets ?: 0

        fun averageSecrets(): Double = data?.averageSecrets ?: 0.0

        fun spirit(): Map<String, String?> = data?.spirit ?: emptyMap()

        fun goldenDragon(): List<Map<String, String?>> = data?.goldenDragon ?: emptyList()

        fun enderDragon(): List<Map<String, String?>> = data?.enderDragon ?: emptyList()

        fun normalPBs(): Map<String, Map<String, String>> = data?.personal_best_normal ?: emptyMap()

        fun masterPBs(): Map<String, Map<String, String>> = data?.personal_best_master ?: emptyMap()

        fun magicalPower(): Int = data?.magical_power ?: 0
    }
    data class MultiDungeonApiResult(val result: Map<String /* player's name */, DungeonsApiResult>?)

    data class CacheResult(val player: DungeonsApiResult, val isOutdated: Boolean)

    fun getFromCache(player: String, cooldown: Int = 10): CacheResult? {
        val key = player.lowercase()
        val cache = playerData[key] ?: return null
        val t = System.currentTimeMillis()
        return CacheResult(cache, t - cache.snapshotTime >= 1000 * 60 * cooldown)
    }

    fun ensureCache(players: List<String>, cooldown: Int = 10) {
        val needAdd = players.filter {
            getFromCache(it, cooldown)?.isOutdated != false
        }

        synchronized(playerQueue) {
            needAdd.forEach {
                playerQueue.putIfAbsent(it.lowercase(), mutableListOf())
            }
        }
    }

    fun fetchPlayer(player: String, cb: (DungeonsApiResult) -> Unit, cooldown: Int = 10) {
        val cache = getFromCache(player, cooldown)
        cache?.let { cb(it.player) }
        if (cache?.isOutdated != false) synchronized(playerQueue) {
            playerQueue.getOrPut(player.lowercase()) { mutableListOf() }.add(cb)
        }
    }

    fun initialize() {
        Scheduler.schedulePool.scheduleWithFixedDelay({
            WebRequests.withName("DungeonsApi") {
                val names: LinkedHashMap<String, List<(DungeonsApiResult) -> Unit>>

                synchronized(playerQueue) {
                    names = LinkedHashMap(playerQueue)
                    playerQueue.clear()
                }

                if (names.isEmpty()) return@withName

                val result = WebRequests.get("${DUNGEONS_API}${names.keys.joinToString(",")}")
                val response = PersistentJson.gson.fromJson(result, MultiDungeonApiResult::class.java) ?: return@withName

                val t = System.currentTimeMillis()
                response.result?.entries?.forEach { (k, v) ->
                    if (!v.success) {
                        // playerQueue.add(k)
                        println("DungeonsApi unsuccessful request $k - ${v.status}")
                        ChatUtils.sendMessage("&cDungeonsApi failed to fetch data for user &b$k &7(${v.status})", true)
                        return@forEach
                    }
                    v.snapshotTime = t
                    playerData[k] = v

                    names[k]?.forEach { it(v) }
                }
            }
        }, 5L, 5L, TimeUnit.SECONDS)
    }

    fun playerData() = playerData

    fun playerQueue() = playerQueue
}