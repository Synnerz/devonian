package com.github.synnerz.devonian.api

import com.github.synnerz.devonian.utils.PersistentJsonClass
import java.util.concurrent.TimeUnit

object SkyblockPrices {
    data class PriceData(
        val bazaarData: BazaarData,
        val auctionData: Map<String, Float>,
        val lastSave: Long
    )
    data class CustomPriceData(
        val price: Float,
        val auction: Boolean = false,
        val bazaarData: BazaarData.Products = BazaarData.Products.EMPTY,
    ) {
        companion object {
            val EMPTY = CustomPriceData(-1f)
        }
    }

    private val loader = object : PersistentJsonClass<PriceData>(
        "devonian/prices.json",
        PriceData::class.java,
        false
    ) {
        override fun onLoadDefault() {
            data = PriceData(BazaarData(true, 0L, mapOf()), mapOf(), 0L)
        }
    }

    fun initialize() {
        loader.load()
        Scheduler.schedulePool.scheduleWithFixedDelay(::update, 1L, 5L, TimeUnit.MINUTES)
    }

    private fun update() {
        if (System.currentTimeMillis() - (loader.data?.lastSave ?: 0) <= (1000 * 60) * 5) return

        WebRequests.withName("SkyblockPrices") {
            val bzRequest = WebRequests.get("https://api.hypixel.net/skyblock/bazaar")
            val ahRequest = WebRequests.get("https://api.docilelm.top/v2/lowestbin")
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

    fun priceData(name: String): CustomPriceData {
        val data = loader.data ?: return CustomPriceData.EMPTY
        if (data.bazaarData.products.containsKey(name)) {
            val bzdata = data.bazaarData.products[name]
            val price = bzdata?.quick_status?.buyPrice ?: 0f
            return CustomPriceData(price, bazaarData = bzdata ?: BazaarData.Products.EMPTY)
        }

        val price = data.auctionData[name] ?: -1f
        return CustomPriceData(price, true, BazaarData.Products.EMPTY)
    }
}