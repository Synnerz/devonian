package com.github.synnerz.devonian.api.dungeon

import com.github.synnerz.devonian.api.Scheduler
import com.github.synnerz.devonian.api.WebRequests
import com.github.synnerz.devonian.utils.PersistentJson
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit

object DungeonsApi {
    private const val DUNGEONS_API = "https://api.docilelm.top/v2/dungeons/"
    private val playerQueue = CopyOnWriteArrayList<String>()
    private val playerData = ConcurrentHashMap<String, DungeonsApiResult>()
    private val requestListeners = CopyOnWriteArrayList<(String, DungeonsApiResult) -> Unit>()
    private val customCooldowns = ConcurrentHashMap<String, Int>()

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
    )
    data class DungeonsApiResult(
        var timeTaken: Long,
        val success: Boolean,
        val status: String,
        val data: UserDungeonsData?
    ) {
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

    fun initialize() {
        Scheduler.schedulePool.scheduleWithFixedDelay({
            WebRequests.withName("DungeonsApi") {
                val names = playerQueue
                    .filter {
                        val _cache = playerData[it]
                        _cache == null ||
                        System.currentTimeMillis() - _cache.timeTaken >= 1000 * 60 * (customCooldowns[it] ?: 10)
                    }
                if (names.isEmpty()) return@withName

                val result = WebRequests.get("${DUNGEONS_API}${names.joinToString(",")}")
                val response: MultiDungeonApiResult = PersistentJson.gson.fromJson(result, MultiDungeonApiResult::class.java) ?: return@withName

                playerQueue.removeIf { names.contains(it) }

                response.result?.entries?.forEach { (k, v) ->
                    if (!v.success) {
                        playerQueue.add(k)
                        println("DungeonsApi unsuccessful request $k - ${v.status}")
                        return@forEach
                    }
                    v.timeTaken = System.currentTimeMillis()
                    playerData[k] = v
                    requestListeners.forEach { it(k.lowercase(), v) }
                }
            }
        }, 5L, 5L, TimeUnit.SECONDS)
    }

    fun on(cb: (String, DungeonsApiResult) -> Unit) {
        requestListeners.add(cb)
    }

    fun requestPlayer(name: String) {
        if (playerQueue.contains(name.lowercase())) return
        playerQueue.add(name.lowercase())
    }

    fun requestPlayers(names: List<String>) {
        names.forEach {
            if (playerQueue.contains(it)) return@forEach

            playerQueue.add(it)
        }
    }

    fun player(name: String, cooldown: Int = 10): DungeonsApiResult? {
        customCooldowns[name.lowercase()] = cooldown
        return playerData[name.lowercase()]
    }

    fun playerOrRequest(name: String, cooldown: Int = 10): DungeonsApiResult? {
        val _cache = playerData[name.lowercase()]
        if (_cache == null)
            playerQueue.add(name.lowercase())
        customCooldowns[name.lowercase()] = cooldown

        return _cache
    }
}