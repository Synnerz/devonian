package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.devonian.api.ItemUtils
import com.github.synnerz.devonian.api.events.PostRenderHotbarSlotEvent
import com.github.synnerz.devonian.api.events.RenderSlotEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature

object StarsStackSize : Feature(
    "starsStackSize",
    "Uses stars of the item as stack size",
    Categories.DUNGEONS,
    subcategory = "QOL"
) {
    override fun initialize() {
        on<PostRenderHotbarSlotEvent> { event ->
            val itemStack = event.item
            val extraAttributes = ItemUtils.extraAttributes(itemStack) ?: return@on
            val stars = extraAttributes.get("upgrade_level")?.asInt() ?: return@on
            if (!stars.isPresent) return@on
            val amount = "${stars.get()}"
            val width = minecraft.font.width(amount)

            event.ctx.drawString(
                minecraft.font,
                amount,
                event.x + 17 - width,
                event.y + 9,
                -1,
                true
            )
        }.prio = 30

        on<RenderSlotEvent> { event ->
            val slot = event.slot
            val itemStack = slot.item
            val extraAttributes = ItemUtils.extraAttributes(itemStack) ?: return@on
            val stars = extraAttributes.get("upgrade_level")?.asInt() ?: return@on
            if (!stars.isPresent) return@on
            val amount = "${stars.get()}"
            val width = minecraft.font.width(amount)

            event.ctx.drawString(
                minecraft.font,
                amount,
                slot.x + 17 - width,
                slot.y + 9,
                -1,
                true
            )
        }.prio = 30
    }
}