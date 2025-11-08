package com.github.synnerz.devonian.api

data class BazaarData(
    val success: Boolean,
    val lastUpdate: Long,
    val products: Map<String, Products>
) {
    data class QuickStatus(
        val productId: String,
        val sellPrice: Float,
        val sellVolume: Int,
        val sellMovingWeek: Int,
        val sellOrders: Int,
        val buyPrice: Float,
        val buyVolume: Int,
        val buyMovingWeek: Int,
        val buyOrders: Int,
    )

    data class SellSummary(
        val amount: Int,
        val pricePerUnit: Float,
        val orders: Int
    )

    data class BuySummary(
        val amount: Int,
        val pricePerUnit: Float,
        val orders: Int
    )

    data class Products(
        val product_id: String,
        val sell_summary: List<SellSummary>,
        val buy_summary: List<BuySummary>,
        val quick_status: QuickStatus
    )
}
