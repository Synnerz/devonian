package com.github.synnerz.devonian.features.misc.tooltip

import com.github.synnerz.devonian.api.ItemUtils
import com.github.synnerz.devonian.api.SkyblockPrices
import com.github.synnerz.devonian.api.events.TooltipRenderEvent
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.utils.StringUtils
import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.ChatFormatting
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent
import net.minecraft.network.chat.Style
import net.minecraft.util.FormattedCharSequence
import net.minecraft.world.item.ItemStack
import org.lwjgl.glfw.GLFW
import kotlin.jvm.optionals.getOrNull

object ItemValue : Feature(
    "itemValue",
    "Shows the value of the currently hovered item in lore.",
    subcategory = "Tooltip",
) {
    override fun initialize() {
        on<TooltipRenderEvent> { event ->
            val item = event.item ?: return@on
            val sbId = ItemUtils.skyblockId(item) ?: name(item) ?: return@on
            var priceData = SkyblockPrices.priceData(sbId)
            var isShard = false
            if (priceData.price == -1f) {
                val name = name(item) ?: return@on
                val possiblePrice = SkyblockPrices.priceData(name)
                if (possiblePrice.price == -1f) {
                    // possibly a shard
                    val shardPrice = SkyblockPrices.priceData("SHARD_${sbId}")
                    if (shardPrice.price == -1f) return@on
                    priceData = shardPrice
                    isShard = true
                } else priceData = possiblePrice
            }
            val isShiftDown =
                    InputConstants.isKeyDown(minecraft.window, GLFW.GLFW_KEY_LEFT_SHIFT) ||
                    InputConstants.isKeyDown(minecraft.window, GLFW.GLFW_KEY_RIGHT_SHIFT)

            if (!priceData.auction) {
                val buyPrice = priceData.bazaarData.quick_status.buyPrice
                val sellPrice = priceData.bazaarData.quick_status.sellPrice
                val count = if (isShard) 64 else item.count
                val buy = if (isShiftDown) buyPrice * count else buyPrice
                val sell = if (isShiftDown) sellPrice * count else sellPrice

                event.lore.add(ClientTooltipComponent.create(
                    FormattedCharSequence.composite(
                        FormattedCharSequence.forward("Bazaar Insta-Buy", Style.EMPTY.withColor(ChatFormatting.GOLD)),
                        FormattedCharSequence.forward(": ", Style.EMPTY.withColor(ChatFormatting.WHITE)),
                        FormattedCharSequence.forward(
                            StringUtils.addCommasTruncate(buy),
                            Style.EMPTY.withColor(ChatFormatting.YELLOW)
                        ),
                        FormattedCharSequence.forward(if (isShiftDown || count == 1) "" else " (shift)", Style.EMPTY.withColor(ChatFormatting.DARK_GRAY)),
                    )
                ))
                event.lore.add(ClientTooltipComponent.create(
                    FormattedCharSequence.composite(
                        FormattedCharSequence.forward("Bazaar Insta-Sell", Style.EMPTY.withColor(ChatFormatting.GOLD)),
                        FormattedCharSequence.forward(": ", Style.EMPTY.withColor(ChatFormatting.WHITE)),
                        FormattedCharSequence.forward(
                            StringUtils.addCommasTruncate(sell),
                            Style.EMPTY.withColor(ChatFormatting.YELLOW)
                        ),
                        FormattedCharSequence.forward(if (isShiftDown || count == 1) "" else " (shift)", Style.EMPTY.withColor(ChatFormatting.DARK_GRAY)),
                    )
                ))
                return@on
            }

            val priceSequence = FormattedCharSequence.composite(
                FormattedCharSequence.forward("LowestBin", Style.EMPTY.withColor(ChatFormatting.GOLD)),
                FormattedCharSequence.forward(": ", Style.EMPTY.withColor(ChatFormatting.WHITE)),
                FormattedCharSequence.forward(StringUtils.addCommasTruncate(priceData.price), Style.EMPTY.withColor(ChatFormatting.YELLOW)),
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