package com.github.synnerz.devonian.features.misc.tooltip

import com.github.synnerz.devonian.ClientTextTooltipStringAccessor
import com.github.synnerz.devonian.api.ItemUtils
import com.github.synnerz.devonian.api.events.TooltipRenderEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.utils.EnchantRegistry
import com.github.synnerz.devonian.utils.StackingEnchant
import com.github.synnerz.devonian.utils.StringUtils
import net.minecraft.ChatFormatting
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent
import net.minecraft.network.chat.Style
import net.minecraft.util.FormattedCharSequence
import net.minecraft.world.item.ItemStack
import java.util.IdentityHashMap
import kotlin.jvm.optionals.getOrElse
import kotlin.jvm.optionals.getOrNull

object StackingEnchantProgress : Feature(
    "stackingEnchantProgress",
    "Shows the progress of a stacking enchantment in an item's lore.",
    Categories.MISC,
    subcategory = "Tooltip",
) {
    private val SETTING_COMPACT = addSwitch(
        "compact",
        false,
        "Abbreviates the number (1.47k instead of 1,473).",
        "Compact Number",
    )

    private val EMPTY = StackingEnchant("", "", "", "", "", listOf())

    private val STYLE_BG = Style.EMPTY.withColor(ChatFormatting.GRAY)
    private val STYLE_FG = Style.EMPTY.withColor(ChatFormatting.GREEN)

    private val cache = IdentityHashMap<ItemStack, StackingEnchant>()
    override fun initialize() {
        on<TooltipRenderEvent> { event ->
            val held = event.item ?: return@on
            val type = cache.getOrPut(held) {
                val data = ItemUtils.extraAttributes(held) ?: return@getOrPut EMPTY
                val ench = data.getCompound("enchantments").getOrNull() ?: return@getOrPut EMPTY
                if (ench.isEmpty) return@getOrPut EMPTY

                return@getOrPut ench.keySet().stream()
                    .map { EnchantRegistry.getOrUnknownNbt(it) }
                    .filter { it is StackingEnchant }
                    .findFirst()
                    .getOrElse { EMPTY } as StackingEnchant?
            } ?: return@on
            if (type === EMPTY) return@on

            val data = ItemUtils.extraAttributes(held) ?: return@on
            val num = data.getInt(type.nbtTag).getOrNull() ?: 0

            val upper = type.progressTree.higher(num)
            val curr = if (SETTING_COMPACT.get()) StringUtils.shortenNumber(num)
                else StringUtils.addCommas(num)
            val progress = FormattedCharSequence.composite(
                FormattedCharSequence.forward(curr, STYLE_FG),
                FormattedCharSequence.forward(
                    if (upper == null) " (Maxed)"
                    else " / ${StringUtils.formatShortest(upper)}",
                    STYLE_BG,
                )
            )

            val idx = event.lore.indexOfLast {
                val str = (it as? ClientTextTooltipStringAccessor)?.`devonian$asString`() ?: return@indexOfLast false
                return@indexOfLast str.isEmpty()
            }
            if (idx < 0) return@on

            event.lore.add(
                idx,
                ClientTooltipComponent.create(
                    FormattedCharSequence.composite(
                        FormattedCharSequence.forward(
                            "${type.displayName}: ",
                            STYLE_BG,
                        ),
                        progress,
                    )
                )
            )
            // adding in reverse order
            event.lore.add(idx, ClientTooltipComponent.create(FormattedCharSequence.EMPTY))
        }
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        cache.clear()
    }
}