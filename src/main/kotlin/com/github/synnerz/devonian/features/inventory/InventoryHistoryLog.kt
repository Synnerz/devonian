package com.github.synnerz.devonian.features.inventory

import com.github.synnerz.devonian.api.Scheduler
import com.github.synnerz.devonian.api.events.RenderOverlayEvent
import com.github.synnerz.devonian.api.events.TickEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.hud.texthud.TextHudFeature
import com.github.synnerz.devonian.utils.Location
import com.github.synnerz.devonian.utils.StringUtils.colorCodes
import kotlin.math.abs

object InventoryHistoryLog : TextHudFeature(
    "inventoryHistoryLog",
    "Displays the items changed, removed or added to your inventory"
) {
    private val SETTING_ITEM_DISPLAY_TIME = addSlider(
        "itemDisplayTime",
        "Duration (in seconds) for entries to stay",
        "Item Display Time",
        0.0, 30.0,
        4.0
    )

    data class ItemizedDifference(val name: String, var quantity: Int) {
        var ttl = (SETTING_ITEM_DISPLAY_TIME.get() * 20.0).toInt()

        fun add(q: Int) {
            quantity += q
            ttl = (SETTING_ITEM_DISPLAY_TIME.get() * 20.0).toInt()
        }

        override fun toString(): String = "${if (quantity < 0) "&c-" else "&a+"}${"%,d".format(abs(quantity))}x&r $name"

        data class Key(val name: String, val increase: Boolean)
    }

    val receipt = linkedMapOf<ItemizedDifference.Key, ItemizedDifference>()
    var inventory: MutableMap<String, Int>? = null
    var worldSwap = false

    override fun initialize() {
        on<TickEvent> {
            if (Location.area == null) return@on
            receipt.entries.removeIf { --it.value.ttl <= 0 }

            val inv = minecraft.player?.inventory ?: return@on
            if (worldSwap) return@on

            val newInv = mutableMapOf<String, Int>()
            inv.forEachIndexed { i, v ->
                if (i == 8) return@forEachIndexed
                if (v.isEmpty) return@forEachIndexed

                val name = v.customName?.colorCodes() ?: v.itemName.string
                val count = v.count
                newInv.merge(name.clearName(), count, Int::plus)
                inventory?.merge(name.clearName(), -count, Int::plus)
            }

            inventory?.forEach { (k, v) ->
                if (v == 0) return@forEach
                val q = -v
                val key = ItemizedDifference.Key(k, q > 0)
                receipt.getOrPut(key) { ItemizedDifference (k, 0) }.add(q)
            }

            inventory = newInv
        }

        on<RenderOverlayEvent> { event ->
            setLines(receipt.map { it.value.toString() })
            draw(event.ctx)
        }

        on<WorldChangeEvent> {
            worldSwap = true
            Scheduler.scheduleServerTask(20) { worldSwap = false }
        }
    }

    private fun String.clearName(): String = this.replace(" ?§r§8 ?x\\d+".toRegex(), "").trim()

    override fun getEditText(): List<String> = listOf("&c-100&r &eSocial Credit")
}