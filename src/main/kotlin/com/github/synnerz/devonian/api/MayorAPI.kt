package com.github.synnerz.devonian.api

import com.github.synnerz.devonian.api.dungeon.Dungeons
import com.github.synnerz.devonian.utils.PersistentJson
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

object MayorAPI {
    var mayorData: MayorData? = null

    data class MayorPerkData(
        val name: String,
        val description: String,
    )
    data class MinisterPerkData(
        val name: String,
        val description: String,
        val minister: Boolean,
    )
    data class MinisterData(
        val key: String, // category
        val name: String,
        val perk: MinisterPerkData
    )
    data class MayorData(
        val key: String, // category
        val name: String,
        val perks: List<MayorPerkData>,
        val minister: MinisterData,
        val election: Any, // doesn't matter currently
    )
    data class MayorResponse(
        val success: Boolean,
        val lastUpdated: Long,
        val mayor: MayorData,
        val current: Any, // doesn't matter currently
    )

    fun initialize() {
        Scheduler.schedulePool.scheduleWithFixedDelay(::update, 1L, 21L, TimeUnit.MINUTES)
    }

    fun update() {
        WebRequests.ioScope.launch {
            val response = WebRequests.get("https://api.hypixel.net/v2/resources/skyblock/election")
            if (response.isEmpty()) return@launch

            val data = PersistentJson.gson.fromJson(response, MayorResponse::class.java)
            val mayor = data.mayor
            mayorData = mayor
            // TODO: maybe make this an event update thing so the apis themselves can hook onto it
            if (hasEzPz()) Dungeons.isPaul.value = true
            println("Devonian\$MayorAPI(name=${mayor.name}, perks=${mayor.perks.joinToString { "${it.name}, " }}, minister=${mayor.minister.name}, ministerPerk=${mayor.minister.perk.name})")
        }
    }

    fun hasEzPz(): Boolean {
        if (mayorData == null) return false
        return mayorData!!.perks.any { it.name.lowercase() == "ezpz" } || mayorData!!.minister.perk.name.lowercase() == "ezpz"
    }

    fun hasRitual(): Boolean {
        if (mayorData == null) return false
        return mayorData!!.perks.any { it.name.lowercase() == "mythological ritual" } || mayorData!!.minister.perk.name.lowercase() == "mythological ritual"
    }
}