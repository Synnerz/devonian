package com.github.synnerz.devonian.features.misc

import com.github.synnerz.devonian.api.ItemUtils
import com.github.synnerz.devonian.api.Scheduler
import com.github.synnerz.devonian.api.dungeon.Dungeons
import com.github.synnerz.devonian.api.events.RenderOverlayEvent
import com.github.synnerz.devonian.api.events.ServerContainerSetContentEvent
import com.github.synnerz.devonian.api.events.ServerContainerSetSlotEvent
import com.github.synnerz.devonian.hud.texthud.Alert
import com.github.synnerz.devonian.hud.texthud.TextHudFeature
import com.github.synnerz.devonian.utils.StringUtils
import com.github.synnerz.devonian.utils.StringUtils.colorCodes
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items

object QuiverDisplay : TextHudFeature(
    "quiverDisplay",
    searchTags = setOf("arrow"),
) {
    private val SETTING_COLOR_AMOUNT = addSwitch(
        "colorAmount",
        false,
        "color the amount of arrows remaining based on number",
        "Color Arrow Count",
    )
    private val SETTING_QUIVER_SIZE = addSelection(
        "quiverSize",
        0,
        listOf("Giant", "Large", "Medium"),
        "",
        "Quiver Size",
    )
    private val SETTING_ALERT_BELOW = addSlider(
        "alertBelow",
        50.0,
        0.0, 2880.0,
        "sends a low arrow alert when the number of arrows drops below this amount",
        "Low Arrow Alert",
    )
    // TODO: add quiver refill cost (but no one uses so)
    private val quiverSizes = listOf(
        5 * 9 * 64,
        4 * 9 * 64,
        3 * 9 * 64
    )
    private val arrowRegex = "^Arrows Remaining: ([\\d,]+)$".toRegex()

    private var sentAlert = false

    override fun initialize() {
        on<ServerContainerSetSlotEvent> { event ->
            if (event.containerId != 0) return@on
            val arrowSlot = if (Dungeons.timeElapsed.value > 0) 9 else 44
            if (event.slot != arrowSlot) return@on

            setArrows(event.itemStack)
        }

        on<ServerContainerSetContentEvent> { event ->
            if (event.containerId != 0) return@on
            setArrows(event.items.getOrNull(if (Dungeons.timeElapsed.value > 0) 9 else 44) ?: return@on)
        }

        on<RenderOverlayEvent> { event ->
            draw(event.ctx)
        }
    }

    override fun getEditText(): List<String> = listOf("&5Flint Arrow &fx&26969")

    private fun setArrows(itemStack: ItemStack) {
        if (itemStack.item != Items.ARROW && itemStack.item != Items.FEATHER) return
        val name = itemStack.customName?.colorCodes() ?: return
        val lore = ItemUtils.lore(itemStack) ?: return

        for (line in lore) {
            val match = arrowRegex.matchEntire(line)?.groupValues ?: continue
            val amount = match[1].replace(",", "").toIntOrNull() ?: continue
            if (amount <= SETTING_ALERT_BELOW.get()) {
                if (!sentAlert) Alert.show("&cLow on Arrows", 1000)
                sentAlert = true
            } else sentAlert = false

            val amountFormat =
                if (SETTING_COLOR_AMOUNT.get()) StringUtils.colorForNumber(
                    amount,
                    quiverSizes.getOrElse(SETTING_QUIVER_SIZE.get()) { 2880 }
                ) else "&a"

            Scheduler.scheduleTask {
                setLine("$name &fx$amountFormat$amount")
            }
            break
        }
    }
}