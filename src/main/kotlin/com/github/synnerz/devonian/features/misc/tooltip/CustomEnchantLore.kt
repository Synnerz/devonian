package com.github.synnerz.devonian.features.misc.tooltip

import com.github.synnerz.devonian.ClientTextTooltipStringAccessor
import com.github.synnerz.devonian.api.ItemUtils
import com.github.synnerz.devonian.api.events.TooltipRenderEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.utils.EnchantRegistry
import com.github.synnerz.devonian.utils.Enchantment
import com.github.synnerz.devonian.utils.StringUtils
import com.github.synnerz.devonian.utils.UltimateEnchant
import com.github.synnerz.devonian.utils.UnknownEnchant
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent
import net.minecraft.util.FormattedCharSequence
import net.minecraft.world.item.ItemStack
import java.util.Comparator
import java.util.IdentityHashMap
import kotlin.jvm.optionals.getOrElse
import kotlin.jvm.optionals.getOrNull

object CustomEnchantLore : Feature(
    "customEnchantLore",
    "Custom enchantment colors and formatting",
    Categories.MISC,
    subcategory = "Tooltip",
    searchTags = setOf("parsing"),
) {
    private val SETTING_MODE = addSelection(
        "mode",
        0,
        listOf("Vanilla", "Compact", "CompactRemoveDesc"),
        "Vanilla: behavior is similar to vanilla. " +
        "Compact: always shorten to as few lines as possible. " +
        "CompactRemoveDesc: same as vanilla, except enchantment descriptions are removed.",
        "Enchantment Format",
    )
    private val SETTING_LINE_WIDTH = addSlider(
        "maxWidth",
        200.0,
        0.0, 1000.0,
        "",
        "Max Line Width",
    )

    private val enchantNameRegex = "^([\\w ]+) ([IVX]+)$".toRegex()
    private val looseEnchantRegex = "^(?:[A-Z][a-z]+ )+[IVX]+(?:,|$)".toRegex()
    private data class EnchantBundle(val enchant: Enchantment, val level: Int) {
        fun str() = "${enchant.loreName} ${StringUtils.formatRoman(level)}"
    }
    private val enchantSort = Comparator.comparingInt<EnchantBundle> {
        if (it.enchant is UltimateEnchant) 0 else 1
    }.then(compareBy(String.CASE_INSENSITIVE_ORDER) { it.enchant.loreName })

    private val cachedLines = IdentityHashMap<ItemStack, List<ClientTooltipComponent>>()
    private val cachedDescriptions = mutableMapOf<EnchantBundle, List<ClientTooltipComponent>>()
    private val cachedLengths = mutableMapOf<EnchantBundle, Int>()
    override fun initialize() {
        SETTING_MODE.state.listen { clearCache() }
        SETTING_LINE_WIDTH.state.listen { clearCache() }

        on<TooltipRenderEvent> { event ->
            val item = event.item ?: return@on
            if (item.isEmpty) return@on

            val lines = cachedLines.getOrPut(item) {
                val data = ItemUtils.extraAttributes(item) ?: return@getOrPut emptyList()
                val ench = data.getCompound("enchantments").getOrNull() ?: return@getOrPut emptyList()
                if (ench.isEmpty) return@getOrPut emptyList()

                val parsed = ench.entrySet().map {
                    EnchantBundle(
                        EnchantRegistry.getOrUnknownNbt(it.key),
                        it.value.asInt().getOrElse { 1 }
                    )
                }.sortedWith(enchantSort)

                val mode = SETTING_MODE.get()
                if (mode == 0 && parsed.size < 6) return@getOrPut layoutDescription(parsed, event.lore)
                if (mode != 1 && parsed.size < 10) return@getOrPut layoutLines(parsed)
                return@getOrPut layoutPacked(parsed)
            } ?: return@on
            if (lines.isEmpty()) return@on

            val (s, e) = findBounds(event.lore) ?: return@on
            event.lore.subList(s, e).clear()
            event.lore.addAll(s, lines)
        }
    }

    private fun findBounds(lore: List<ClientTooltipComponent>): Pair<Int, Int>? {
        var s = -1
        lore.forEachIndexed { i, tt ->
            val str = (tt as? ClientTextTooltipStringAccessor)?.`devonian$asString`() ?: return@forEachIndexed
            if (s == i - 1) {
                if (!looseEnchantRegex.matchesAt(str, 0)) s = -1
            }
            if (!str.isEmpty()) return@forEachIndexed
            if (s == -1) s = i
            else return s + 1 to i
        }
        return null
    }

    private fun layoutDescription(ench: List<EnchantBundle>, lore: List<ClientTooltipComponent>): List<ClientTooltipComponent> {
        if (!ench.all { it.enchant is UnknownEnchant || cachedDescriptions.containsKey(it) }) {
            val (s, e) = findBounds(lore) ?: return emptyList()
            var arr = mutableListOf<ClientTooltipComponent>()
            var ench: EnchantBundle? = null
            for (i in s until e) {
                val l = lore[i]
                val str = (l as? ClientTextTooltipStringAccessor)?.`devonian$asString`()

                val m = str?.let { enchantNameRegex.matchEntire(it) }
                val name = m?.groupValues?.getOrNull(1)
                val level = m?.groupValues?.getOrNull(2)
                if (name != null && level != null) {
                    if (ench != null && ench.enchant !is UnknownEnchant) cachedDescriptions[ench] = arr
                    arr = mutableListOf()

                    val e = EnchantRegistry.getOrUnknownLore(name)
                    val l = StringUtils.parseRoman(level)
                    ench = EnchantBundle(e, l)
                } else arr.add(l)
            }
            if (ench != null && ench.enchant !is UnknownEnchant) cachedDescriptions[ench] = arr
        }

        return ench.flatMap {
            listOf(
                ClientTooltipComponent.create(
                    FormattedCharSequence.forward(
                        it.str(),
                        it.enchant.getStyle(it.level),
                    )
                )
            ) + cachedDescriptions.getOrElse(it) { emptyList() }
        }
    }

    private fun layoutLines(ench: List<EnchantBundle>): List<ClientTooltipComponent> {
        return ench.map {
            ClientTooltipComponent.create(
                FormattedCharSequence.forward(
                    it.str(),
                    it.enchant.getStyle(it.level),
                )
            )
        }
    }

    private fun layoutPacked(ench: List<EnchantBundle>): List<ClientTooltipComponent> {
        val f = minecraft.font ?: return emptyList()
        val spacer = ", "
        val spacerW = f.width(spacer)
        val maxW = SETTING_LINE_WIDTH.get()

        val lines = mutableListOf<List<EnchantBundle>>()
        var currLine = mutableListOf<EnchantBundle>()
        var currW = 0.0

        ench.forEach {
            val w = cachedLengths.getOrPut(it) { f.width(it.str()) }
            if (currLine.isNotEmpty() && currW + w > maxW) {
                lines.add(currLine)
                currLine = mutableListOf()
                currW = 0.0
            }
            currLine.add(it)
            currW += w + spacerW
        }
        if (currLine.isNotEmpty()) lines.add(currLine)

        return lines.map {
            ClientTooltipComponent.create(
                FormattedCharSequence.composite(
                    it.map {
                        FormattedCharSequence.forward(
                            "${it.str()}, ",
                            it.enchant.getStyle(it.level),
                        )
                    }
                )
            )
        }
    }

    private fun clearCache() {
        cachedLines.clear()
        cachedDescriptions.clear()
        cachedLengths.clear()
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        clearCache()
    }
}