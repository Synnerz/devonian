package com.github.synnerz.devonian.features.misc.tooltip

import com.github.synnerz.devonian.api.ItemUtils
import com.github.synnerz.devonian.api.events.TooltipRenderEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.utils.FixedIdentityMap
import net.minecraft.ChatFormatting
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Style
import net.minecraft.network.chat.TextColor
import net.minecraft.util.FormattedCharSequence
import net.minecraft.world.item.ItemStack

object ArmorHexColor : Feature(
    "armorHexColor",
    "Shows the color of an armor piece in the tooltip.",
    Categories.MISC,
    subcategory = "Tooltip",
) {
    private val SETTING_SEYMOUR = addSwitch(
        "onlySeymour",
        false,
        "Only shows the hex color on seymour armor.",
        "Only Seymour Pieces",
    ).also {
        it.state.listen {
            cache.clear()
        }
    }

    private val seymourIds = setOf(
        "VELVET_TOP_HAT",
        "CASHMERE_JACKET",
        "SATIN_TROUSERS",
        "OXFORD_SHOES",
    )

    private val cache = FixedIdentityMap<ItemStack, Int>(128)
    override fun initialize() {
        on<TooltipRenderEvent> { event ->
            val held = event.item ?: return@on
            if (held.isEmpty) return@on

            val color = cache.getOrPut(held) {
                if (SETTING_SEYMOUR.get()) {
                    val id = ItemUtils.skyblockId(held) ?: return@getOrPut -1
                    if (id !in seymourIds) return@getOrPut -1
                }
                return@getOrPut held.get(DataComponents.DYED_COLOR)?.rgb ?: -1
            }
            if (color == -1) return@on

            event.lore.add(
                1,
                ClientTooltipComponent.create(
                    FormattedCharSequence.composite(
                        FormattedCharSequence.forward(
                            "Color: ",
                            Style.EMPTY.withColor(ChatFormatting.GRAY),
                        ),
                        FormattedCharSequence.forward(
                            "#${color.toString(16).padStart(6, '0').uppercase()}",
                            Style.EMPTY.withColor(TextColor.fromRgb(color)),
                        )
                    )
                )
            )
        }.prio = 1
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        cache.clear()
    }
}