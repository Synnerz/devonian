package com.github.synnerz.devonian.api

import com.github.synnerz.devonian.Devonian
import com.github.synnerz.devonian.utils.PersistentJsonClass
import kotlinx.coroutines.*
import java.io.File
import java.util.concurrent.TimeUnit

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

    private val loader = object : PersistentJsonClass<PriceData>(pricesFile, PriceData::class.java) {
        override fun onLoadDefault() {
            data = PriceData(BazaarData(true, 0L, mapOf()), mapOf(), 0L)
        }
    }

    fun initialize() {
        Scheduler.schedulePool.scheduleWithFixedDelay(::update, 1L, 21L, TimeUnit.MINUTES)
    }

    private fun update() {
        if (System.currentTimeMillis() - (loader.data?.lastSave ?: 0) <= (1000 * 60) * 20) return

        WebRequests.ioScope.launch {
            val bzRequest = WebRequests.get("https://api.hypixel.net/skyblock/bazaar")
            val ahRequest = WebRequests.get("https://lowestbin.docilelm.workers.dev/")
            val str = "{ bazaarData: $bzRequest, auctionData: $ahRequest, lastSave: ${System.currentTimeMillis()} }"

            loader.onLoad(str.byteInputStream())
        }
    }

    fun sellPrice(name: String): Float {
        val data = loader.data ?: return 0f
        if (data.bazaarData.products.containsKey(name))
            return data.bazaarData.products[name]?.quick_status?.sellPrice ?: return 0f

        val auctionData = data.auctionData[name] ?: return 0f
        return auctionData
    }

    fun buyPrice(name: String): Float {
        val data = loader.data ?: return 0f
        if (data.bazaarData.products.containsKey(name))
            return data.bazaarData.products[name]?.quick_status?.buyPrice ?: return 0f

        val auctionData = data.auctionData[name] ?: return 0f
        return auctionData
    }
}