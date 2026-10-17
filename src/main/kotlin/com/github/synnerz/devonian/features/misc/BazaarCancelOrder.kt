package com.github.synnerz.devonian.features.misc

import com.github.synnerz.devonian.api.ChatUtils
import com.github.synnerz.devonian.api.ItemUtils
import com.github.synnerz.devonian.api.Scheduler
import com.github.synnerz.devonian.api.events.ChatEvent
import com.github.synnerz.devonian.api.events.ServerContainerOpenEvent
import com.github.synnerz.devonian.api.events.ServerContainerSetSlotEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import net.minecraft.world.item.Items

object BazaarCancelOrder : Feature(
    "bazaarCancelOrder",
    "Sends a message with the missing items in the buy order that you just cancelled",
    Categories.BAZAAR,
) {
    private val SETTING_COPY = addSwitch(
        "copy",
        false,
        "Copies the missing items amount to your clipboard",
        "Copy"
    )
    private val SETTING_ONLY_CANCEL = addSwitch(
        "onlyCancel",
        true,
        "Only shows/copies after the cancel message instead of when you open the gui",
        "Only Cancel"
    )
    private val fullMessageRegex = "^You will be refunded [\\d,.]+ coins from ([\\d,]+)x missing items\\.$".toRegex()
    private val cancelMessageRegex = "^\\[Bazaar] Cancelling order\\.\\.\\.$".toRegex()
    private var inGui = false
    private var num = ""

    override fun initialize() {
        on<ServerContainerOpenEvent> { event ->
            inGui = event.titleStr == "Order options"
        }

        on<ServerContainerSetSlotEvent> { event ->
            if (!inGui) return@on
            val slot = event.slot
            if (slot > 30) return@on
            val itemStack = event.itemStack
            if (itemStack.item != Items.DYED_TERRACOTTA.green) return@on
            val lore = ItemUtils.lore(itemStack)?.joinToString(" ") ?: return@on

            val match = fullMessageRegex.matchEntire(lore) ?: return@on
            val amount = match.groupValues.getOrNull(1) ?: return@on

            if (SETTING_ONLY_CANCEL.get()) {
                num = amount
                return@on
            }

            ChatUtils.sendMessage("&bBazaarOrder missing &6x$amount&b items", true)

            if (!SETTING_COPY.get()) return@on
            val clearNumber = amount.replace(",", "").replace(".", "").toIntOrNull() ?: 1
            Scheduler.scheduleTask { minecraft.keyboardHandler.clipboard = "$clearNumber" }
        }

        on<ChatEvent> { event ->
            if (event.matches(cancelMessageRegex) == null || num.isEmpty()) return@on

            ChatUtils.sendMessage("&bBazaarOrder missing &6x$num&b items", true)
            if (SETTING_COPY.get()) {
                val clearNumber = num.replace(",", "").replace(".", "").toIntOrNull() ?: 1
                minecraft.keyboardHandler.clipboard = "$clearNumber"
            }
            num = ""
        }
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        num = ""
    }
}