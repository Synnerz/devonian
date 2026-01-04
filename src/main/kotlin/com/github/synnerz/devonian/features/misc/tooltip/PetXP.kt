package com.github.synnerz.devonian.features.misc.tooltip

import com.github.synnerz.devonian.ClientTextTooltipStringAccessor
import com.github.synnerz.devonian.api.ItemUtils
import com.github.synnerz.devonian.api.events.TooltipRenderEvent
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.utils.StringUtils
import com.google.gson.JsonParser
import net.minecraft.ChatFormatting
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent
import net.minecraft.network.chat.Style
import net.minecraft.util.FormattedCharSequence
import kotlin.jvm.optionals.getOrNull

object PetXP : Feature(
    "petXP",
    "shows pet experience",
    subcategory = "Tooltip",
) {
    override fun initialize() {
        on<TooltipRenderEvent> { event ->
            val item = event.item ?: return@on
            val data = ItemUtils.extraAttributes(item) ?: return@on

            val id = data.getString("id").getOrNull()
            if (id != "PET") return@on

            val info = data.getString("petInfo").getOrNull() ?: return@on
            val xp = try {
                val infoObj = JsonParser.parseString(info).asJsonObject
                infoObj.get("exp")?.asDouble
            } catch (_: Exception) {
                return@on
            } ?: return@on

            // max level pets already have: "▸ 25,363,191 XP"
            val idx = event.lore.indexOfFirst {
                (it as? ClientTextTooltipStringAccessor)
                    ?.`devonian$asString`()
                    ?.startsWith("Progress to Level") == true
            }
            if (idx < 0) return@on

            val formatted = FormattedCharSequence.composite(
                FormattedCharSequence.forward("Total pet exp: ", Style.EMPTY.withColor(ChatFormatting.GRAY)),
                FormattedCharSequence.forward(
                    StringUtils.addCommas(xp.toLong()),
                    Style.EMPTY.withColor(ChatFormatting.GOLD),
                ),
            )

            event.lore.add(idx + 2, ClientTooltipComponent.create(formatted))
        }
    }
}