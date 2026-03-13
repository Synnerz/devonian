package com.github.synnerz.devonian.features.misc

import com.github.synnerz.devonian.api.ItemUtils
import com.github.synnerz.devonian.api.events.RenderOverlayEvent
import com.github.synnerz.devonian.api.events.TickEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.hud.texthud.TextHudFeature
import com.github.synnerz.devonian.utils.EnchantRegistry
import com.github.synnerz.devonian.utils.FixedIdentityMap
import com.github.synnerz.devonian.utils.StackingEnchant
import com.github.synnerz.devonian.utils.StringUtils
import net.minecraft.world.item.ItemStack
import kotlin.jvm.optionals.getOrElse
import kotlin.jvm.optionals.getOrNull

object StackingEnchantProgressHud : TextHudFeature(
    "stackingEnchantProgressHud",
    "Shows the progress of the stacking enchant on the currently held item as a hud.",
    Categories.MISC,
    subcategory = "General",
) {
    private val SETTING_COMPACT = addSwitch(
        "compact",
        true,
        "Abbreviates the number (1.47k instead of 1,473).",
        "Compact Number",
    )

    private val EMPTY = StackingEnchant("", "", "", "", "", listOf())

    private val cache = FixedIdentityMap<ItemStack, StackingEnchant>(128)
    private var display = false
    override fun initialize() {
        on<TickEvent> {
            display = false
            val player = minecraft.player ?: return@on
            val held = player.mainHandItem
            if (held.isEmpty) return@on

            val type = cache.getOrPut(held) {
                val data = ItemUtils.extraAttributes(held) ?: return@getOrPut EMPTY
                val ench = data.getCompound("enchantments").getOrNull() ?: return@getOrPut EMPTY
                if (ench.isEmpty) return@getOrPut EMPTY

                return@getOrPut ench.keySet().stream()
                    .map { EnchantRegistry.getOrUnknownNbt(it) }
                    .filter { it is StackingEnchant }
                    .findFirst()
                    .getOrElse { EMPTY } as StackingEnchant
            }
            if (type === EMPTY) return@on

            val data = ItemUtils.extraAttributes(held) ?: return@on
            val num = data.getInt(type.nbtTag).getOrNull() ?: 0
            val tier = type.progress.indexOf(type.progressTree.floor(num) ?: 0) + 1

            val upper = type.progressTree.higher(num)
            val curr = if (SETTING_COMPACT.get()) StringUtils.shortenNumber(num)
                else StringUtils.addCommas(num)
            val progress = if (upper == null) "&e$curr"
                else "&c$curr &f/ &a${StringUtils.formatShortest(upper)}"

            setLine("&b${type.loreName} &e${tier} &f($progress&f)")
            display = true
        }

        on<RenderOverlayEvent> { event ->
            if (!display) return@on

            draw(event.ctx)
        }
    }

    override fun getEditText(): List<String> = listOf("&bExpertise &eIX &f(&c14.9k &f/ &a15.0k&f)")

    override fun onWorldChange(event: WorldChangeEvent) {
        cache.clear()
        display = false
    }
}