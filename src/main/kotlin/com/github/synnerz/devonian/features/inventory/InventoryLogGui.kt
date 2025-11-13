package com.github.synnerz.devonian.features.inventory

import com.github.synnerz.devonian.api.events.RenderOverlayEvent
import com.github.synnerz.devonian.api.events.TickEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.hud.HudManager
import com.github.synnerz.devonian.utils.Render2D
import net.minecraft.entity.player.PlayerInventory
import kotlin.math.abs

object InventoryLogGui : Feature("inventoryLogGui") {
    private const val SETTING_ITEM_DISPLAY_TIME = 100

    val hud = HudManager.createHud("inventoryLogGui", "&c-100&r &eSocial Credit")

    data class ItemizedDifference(val name: String, var quantity: Int) {
        var ttl = SETTING_ITEM_DISPLAY_TIME

        fun add(q: Int) {
            quantity += q
            ttl = SETTING_ITEM_DISPLAY_TIME
        }

        override fun toString(): String = "${if (quantity < 0) "&c-" else "&a+"} ${"%,d".format(abs(quantity))}x&r $name"

        data class Key(val name: String, val increase: Boolean)
    }

    val receipt = linkedMapOf<ItemizedDifference.Key, ItemizedDifference>()
    var inventory: MutableMap<String, Int>? = null

    override fun initialize() {
        on<TickEvent> {
            receipt.entries.removeIf { --it.value.ttl <= 0 }

            if (minecraft.currentScreen != null) return@on

            val inv = minecraft.player?.inventory ?: return@on

            val newInv = mutableMapOf<String, Int>()
            inv.forEachIndexed { i, v ->
                if (i == PlayerInventory.OFF_HAND_SLOT) return@forEachIndexed
                if (v.isEmpty) return@forEachIndexed

                val name = v.name.string
                val count = v.count
                newInv.merge(name, count, Int::plus)
                inventory?.merge(name, -count, Int::plus)
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
            val text = receipt.map { it.value.toString() }
            Render2D.drawStringNW(event.ctx, text.joinToString("\n"), hud.x, hud.y, hud.scale)
        }

        on<WorldChangeEvent> { inventory = null }
    }
}