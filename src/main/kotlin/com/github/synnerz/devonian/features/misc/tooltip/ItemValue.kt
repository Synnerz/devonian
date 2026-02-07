package com.github.synnerz.devonian.features.misc.tooltip

import com.github.synnerz.devonian.api.ItemUtils
import com.github.synnerz.devonian.api.SkyblockPrices
import com.github.synnerz.devonian.api.events.TooltipRenderEvent
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.utils.StringUtils
import net.minecraft.ChatFormatting
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent
import net.minecraft.network.chat.Style
import net.minecraft.util.FormattedCharSequence
import net.minecraft.world.item.ItemStack
import kotlin.jvm.optionals.getOrNull

object ItemValue : Feature(
    "itemValue",
    "Shows the value of the currently hovered item in lore.",
    subcategory = "Tooltip",
) {
    override fun initialize() {
        on<TooltipRenderEvent> { event ->
            val item = event.item ?: return@on
            val sbId = ItemUtils.skyblockId(item) ?: return@on
            var price = SkyblockPrices.sellPrice(sbId)
            if (price == 0f) {
                val name = name(item) ?: return@on
                val possiblePrice = SkyblockPrices.sellPrice(name)
                if (possiblePrice == 0f) return@on
                price = possiblePrice
            }
            price *= item.count

            val priceSequence = FormattedCharSequence.composite(
                FormattedCharSequence.forward("Price", Style.EMPTY.withColor(ChatFormatting.GOLD)),
                FormattedCharSequence.forward(": ", Style.EMPTY.withColor(ChatFormatting.WHITE)),
                FormattedCharSequence.forward(StringUtils.addCommasTruncate(price), Style.EMPTY.withColor(ChatFormatting.YELLOW)),
            )

            event.lore.add(
                ClientTooltipComponent.create(priceSequence)
            )
        }
    }

    private fun name(itemStack: ItemStack): String? {
        val name = itemStack.customName?.string?.replace("x\\d+".toRegex(), "") ?: return null
        val reforge = ItemUtils.extraAttributes(itemStack)?.getString("modifier")
        var clearName = "([A-z' ]+)".toRegex().find(name)?.value ?: return null

        clearName = clearName.uppercase().replace("\'".toRegex(), "")
        reforge?.getOrNull()?.let { clearName = clearName.replace(it.uppercase(), "") }

        return clearName.trim().replace(" ".toRegex(), "_")
    }
}