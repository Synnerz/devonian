package com.github.synnerz.devonian.api

import com.github.synnerz.devonian.Devonian
import com.github.synnerz.devonian.utils.JsonUtils
import kotlinx.coroutines.*
import java.io.File
import java.io.FileWriter

object SkyblockPrices {
    data class PriceData(
        val bazaarData: BazaarData,
        val auctionData: Map<String, Float>,
        val lastSave: Long
    )
    private val pricesFile = File(
        Devonian.minecraft.gameDirectory,
        "config"
    )
        .resolve("devonian")
        .resolve("prices.json")
    private var savedData =
        if (pricesFile.exists() && pricesFile.readText().isNotEmpty())
            JsonUtils.gson.fromJson(pricesFile.readText(), PriceData::class.java)
        else
            PriceData(BazaarData(true, 0L, mapOf()), mapOf(), 0L)

    init {
        if (!pricesFile.exists()) {
            pricesFile.parentFile.mkdirs()
            pricesFile.createNewFile()
        }

        launchRepeat((1000L * 60L) * 21) {
            update()
        }

        JsonUtils.preSave {
            FileWriter(pricesFile).use { JsonUtils.gson.toJson(savedData, it) }
        }
    }

    private fun update() {
        if (System.currentTimeMillis() - savedData.lastSave <= (1000 * 60) * 20) return

        WebRequests.ioScope.launch {
            val bzRequest = WebRequests.get("https://api.hypixel.net/skyblock/bazaar")
            val ahRequest = WebRequests.get("https://moulberry.codes/lowestbin.json")
            val str = "{ bazaarData: $bzRequest, auctionData: $ahRequest, lastSave: ${System.currentTimeMillis()} }"

            savedData = JsonUtils.gson.fromJson(str, PriceData::class.java)
        }
    }

    // TODO: perhaps make this its own utility ?
    private fun launchRepeat(delayTime: Long, cb: () -> Unit) {
        CoroutineScope(Dispatchers.Default).launch {
            while (isActive) {
                cb()
                delay(delayTime)
            }
        }
    }

    fun sellPrice(name: String): Float {
        if (savedData.bazaarData.products.containsKey(name))
            return savedData.bazaarData.products[name]!!.sell_summary.last().pricePerUnit

        val auctionData = savedData.auctionData[name] ?: return 0f
        return auctionData
    }

    fun buyPrice(name: String): Float {
        if (savedData.bazaarData.products.containsKey(name))
            return savedData.bazaarData.products[name]?.buy_summary?.lastOrNull()?.pricePerUnit ?: return 0f

        val auctionData = savedData.auctionData[name] ?: return 0f
        return auctionData
    }
}