package com.github.synnerz.devonian.features.misc

import com.github.synnerz.devonian.api.ItemUtils
import com.github.synnerz.devonian.api.Scheduler
import com.github.synnerz.devonian.api.events.*
import com.github.synnerz.devonian.features.Feature
import java.awt.Color

object BazaarOverlay : Feature(
    "bazaarOverlay",
    "Highlights filled/partially filled/unfilled orders in bazaar",
    subcategory = "General"
) {
    private val SETTING_FILLED_COLOR = addColorPicker(
        "filledColor",
        Color.GREEN.rgb,
        "The color for filled background orders",
        "BazaarOverlay Filled Color"
    )
    private val SETTING_PARTIALLY_FILLED_COLOR = addColorPicker(
        "partiallyFilledColor",
        Color.ORANGE.rgb,
        "The color for partially filled (ex: 1/4) background orders",
        "BazaarOverlay Partially Filled Color"
    )
    private val SETTING_UNFILLED_COLOR = addColorPicker(
        "unFilledColor",
        Color.RED.rgb,
        "The color for unfilled background orders",
        "BazaarOverlay UnFilled Color"
    )
    private val fillRegex = "^Filled: ([\\dk,.]+)/([\\dk,.]+) \\(?([\\d,.%]+)\\)?!?$".toRegex()
    private val orders = mutableListOf<BazaarOrder>()
    private var inOrders = false

    data class BazaarOrder(val slot: Int, val state: BazaarState)
    enum class BazaarState {
        FILLED,
        PARTIAL,
        UNFILLED
    }

    override fun initialize() {
        on<ServerContainerOpenEvent> { event ->
            inOrders = event.titleStr == "Co-op Bazaar Orders" || event.titleStr == "Your Bazaar Orders"
        }

        on<ServerContainerCloseEvent> {
            if (!inOrders) return@on
            inOrders = false
            Scheduler.scheduleTask { orders.clear() }
        }

        on<ClientContainerCloseEvent> {
            if (!inOrders) return@on
            inOrders = false
            orders.clear()
        }

        on<ServerContainerSetSlotEvent> { event ->
            if (!inOrders) return@on
            val slot = event.slot
            if (slot > 45) return@on
            val itemStack = event.itemStack
            if (itemStack.isEmpty) return@on
            val name = itemStack.customName?.string ?: return@on
            if (!name.startsWith("BUY") && !name.startsWith("SELL")) return@on

            val lore = ItemUtils.lore(itemStack) ?: return@on
            var value = 0
            var total = 0

            for (line in lore) {
                val match = fillRegex.matchEntire(line)?.groupValues?.drop(1) ?: continue
                value = match.getOrNull(0)?.replace(".", "")?.replace("k", "")?.toIntOrNull() ?: 0
                total = match.getOrNull(1)?.replace(".", "")?.replace("k", "")?.toIntOrNull() ?: 1
            }

            val state = when {
                value > 0 && value == total -> BazaarState.FILLED
                value > 0 && value != total -> BazaarState.PARTIAL
                else -> BazaarState.UNFILLED
            }

            Scheduler.scheduleTask {
                orders.removeIf { it.slot == slot }
                orders.add(BazaarOrder(slot, state))
            }
        }

        on<RenderSlotEvent> { event ->
            if (!inOrders) return@on
            val slot = event.slot
            if (slot.container == minecraft.player?.inventory) return@on
            val data = orders.find { it.slot == slot.containerSlot } ?: return@on

            event.ctx.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, when (data.state) {
                BazaarState.FILLED -> SETTING_FILLED_COLOR.get()
                BazaarState.PARTIAL -> SETTING_PARTIALLY_FILLED_COLOR.get()
                BazaarState.UNFILLED -> SETTING_UNFILLED_COLOR.get()
            })
        }.prio = 30
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        inOrders = false
        orders.clear()
    }
}