package com.github.synnerz.devonian.features.inventory

import com.github.synnerz.devonian.api.events.RenderOverlayEvent
import com.github.synnerz.devonian.api.events.TickEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.hud.texthud.TextHudFeature
import net.minecraft.text.Style
import net.minecraft.text.Text
import net.minecraft.text.TextColor
import net.minecraft.util.Formatting
import java.util.Optional
import kotlin.math.abs

object InventoryHistoryLog : TextHudFeature(
    "inventoryHistoryLog",
    "Displays the items changed, removed or added to your inventory"
) {
    private const val SETTING_ITEM_DISPLAY_TIME = 100

    data class ItemizedDifference(val name: String, var quantity: Int) {
        var ttl = SETTING_ITEM_DISPLAY_TIME

        fun add(q: Int) {
            quantity += q
            ttl = SETTING_ITEM_DISPLAY_TIME
        }

        override fun toString(): String = "${if (quantity < 0) "&c-" else "&a+"}${"%,d".format(abs(quantity))}x&r $name"

        data class Key(val name: String, val increase: Boolean)
    }

    val receipt = linkedMapOf<ItemizedDifference.Key, ItemizedDifference>()
    var inventory: MutableMap<String, Int>? = null

    override fun initialize() {
        on<TickEvent> {
            receipt.entries.removeIf { --it.value.ttl <= 0 }

            if (minecraft.currentScreen != null) return@on

            val inv = minecraft.player?.inventory ?: return@on

            val newInv = mutableMapOf<String, Int>()
            inv.forEachIndexed { i, v ->
                // if (i == PlayerInventory.OFF_HAND_SLOT) return@forEachIndexed
                if (v.isEmpty) return@forEachIndexed

                val name = v.customName?.format() ?: v.name.string
                val count = v.count
                newInv.merge(name, count, Int::plus)
                inventory?.merge(name, -count, Int::plus)
            }

            inventory?.forEach { (k, v) ->
                if (v == 0) return@forEach
                val q = -v
                val key = ItemizedDifference.Key(k, q > 0)
                receipt.getOrPut(key) { ItemizedDifference (k, 0) }.add(q)
            }

            inventory = newInv
        }

        on<RenderOverlayEvent> { event ->
            setLines(receipt.map { it.value.toString() })
            draw(event.ctx)
        }

        on<WorldChangeEvent> { inventory = null }
    }

    override fun getEditText(): List<String> = listOf("&c-100&r &eSocial Credit")

    private val colorToFormat = Formatting.entries.mapNotNull { format ->
        TextColor.fromFormatting(format)?.let { it to format }
    }.toMap()

    private fun parseStyle(style: Style): String = buildString {
        append("§r")

        style.color?.let(colorToFormat::get)?.run(::append)

        when {
            style.isBold -> append("§l")
            style.isItalic -> append("§o")
            style.isUnderlined -> append("§n")
            style.isStrikethrough -> append("§m")
            style.isObfuscated -> append("§k")
        }
    }

    private fun parseFormat(_text: Text): String {
        var str = ""

        _text.content.visit({ style, text ->
            val styleFormat = parseStyle(style)
            str += "${styleFormat}$text"
            Optional.empty<Any>()
        }, _text.style)

        return str
    }

    private fun Text.format(): String {
        var str = parseFormat(this)

        str += this.siblings.joinToString("", transform = ::parseFormat)

        return str
    }
}