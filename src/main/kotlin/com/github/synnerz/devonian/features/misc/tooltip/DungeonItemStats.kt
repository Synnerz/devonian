package com.github.synnerz.devonian.features.misc.tooltip

import com.github.synnerz.devonian.api.ItemUtils
import com.github.synnerz.devonian.api.events.TooltipRenderEvent
import com.github.synnerz.devonian.features.Feature
import net.minecraft.ChatFormatting
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent
import net.minecraft.network.chat.Style
import net.minecraft.util.FormattedCharSequence
import kotlin.jvm.optionals.getOrNull

object DungeonItemStats : Feature(
    "dungeonItemStats",
    "show item quality/floor obtained in tooltips",
    subcategory = "Tooltip",
) {
    private val SETTING_STYLE = addSelection(
        "style",
        0,
        listOf("50/50", "50%", "100%"),
        "what should be the max item quality",
        "Item Quality Style",
    )

    override fun initialize() {
        on<TooltipRenderEvent> { event ->
            val item = event.item ?: return@on
            val data = ItemUtils.extraAttributes(item) ?: return@on

            val boost = data.getInt("baseStatBoostPercentage").getOrNull() ?: return@on
            val floor = data.getInt("item_tier").getOrNull() ?: return@on

            val formatted = FormattedCharSequence.composite(
                FormattedCharSequence.forward("Item Quality: ", Style.EMPTY.withColor(ChatFormatting.GRAY)),
                FormattedCharSequence.forward(
                    when (SETTING_STYLE.get()) {
                        0 -> "$boost/50"
                        1 -> "$boost%"
                        2 -> "${boost * 2}%"
                        else -> return@on
                    },
                    Style.EMPTY.withColor(ChatFormatting.LIGHT_PURPLE)
                ),
                FormattedCharSequence.forward(" (Floor ", Style.EMPTY.withColor(ChatFormatting.GRAY)),
                FormattedCharSequence.forward(floor.toString(), Style.EMPTY.withColor(ChatFormatting.LIGHT_PURPLE)),
                FormattedCharSequence.forward(")", Style.EMPTY.withColor(ChatFormatting.GRAY)),
            )

            event.lore.add(1, ClientTooltipComponent.create(formatted))
        }
    }
}