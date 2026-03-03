package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.devonian.api.ItemUtils
import com.github.synnerz.devonian.api.events.PostRenderHotbarSlotEvent
import com.github.synnerz.devonian.api.events.PostRenderSlotEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.world.item.ItemStack
import java.util.IdentityHashMap
import kotlin.jvm.optionals.getOrNull

object StarsStackSize : Feature(
    "starsStackSize",
    "Uses stars of the item as stack size",
    Categories.DUNGEONS,
    subcategory = "QOL"
) {
    private data class Entry(val str: String, val width: Int) {
        companion object {
            val EMPTY = Entry("", 0)
        }
    }
    private val cache = IdentityHashMap<ItemStack, Entry>()

    private fun draw(stack: ItemStack, x: Int, y: Int, ctx: GuiGraphics) {
        if (stack.isEmpty) return

        val entry = cache.getOrPut(stack) {
            val extraAttributes = ItemUtils.extraAttributes(stack) ?: return@getOrPut Entry.EMPTY
            val stars = extraAttributes.getInt("upgrade_level").getOrNull() ?: return@getOrPut Entry.EMPTY
            val amount = "$stars"
            val width = minecraft.font.width(amount)
            return@getOrPut Entry(amount, width)
        } ?: return
        if (entry === Entry.EMPTY) return

        ctx.drawString(
            minecraft.font,
            entry.str,
            x + 17 - entry.width,
            y + 9,
            -1,
            true
        )
    }

    override fun initialize() {
        on<PostRenderHotbarSlotEvent> { event ->
            draw(event.item, event.x, event.y, event.ctx)
        }.prio = 30

        on<PostRenderSlotEvent> { event ->
            draw(event.slot.item, event.slot.x, event.slot.y, event.ctx)
        }.prio = 30
    }
}