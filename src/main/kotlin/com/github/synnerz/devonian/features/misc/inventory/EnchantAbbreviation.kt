package com.github.synnerz.devonian.features.misc.inventory

import com.github.synnerz.devonian.api.ItemUtils
import com.github.synnerz.devonian.api.events.PostRenderSlotsEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.utils.EnchantRegistry
import com.github.synnerz.devonian.utils.Enchantment
import com.github.synnerz.devonian.utils.UltimateEnchant
import com.github.synnerz.devonian.utils.UnknownEnchant
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import java.util.*
import kotlin.jvm.optionals.getOrNull

object EnchantAbbreviation : Feature(
    "enchantAbbreviation",
    "Display an abbreviated name of an enchantment on the book",
    Categories.MISC,
    subcategory = "Inventory",
) {
    private val SETTING_NAME_SCALE = addDecimalSlider(
        "nameScale",
        0.6,
        0.1, 3.0,
        "The Name scale for enchants",
        "Name Scale"
    )
    private val SETTING_LEVEL_SCALE = addDecimalSlider(
        "levelScale",
        0.8,
        0.1, 3.0,
        "The Level scale for enchants",
        "Level Scale"
    )
    private val EMPTY = UnknownEnchant("_", "_") to 0
    private val cache = IdentityHashMap<ItemStack, Pair<Enchantment, Int>>()

    override fun initialize() {
        on<PostRenderSlotsEvent> { event ->
            event.container.menu.slots.forEach {
                val item = it.item
                if (item.isEmpty) return@forEach

                val data = cache.getOrPut(item) {
                    val data = ItemUtils.extraAttributes(item) ?: return@getOrPut EMPTY

                    val id = data.getString("id")?.getOrNull() ?: return@getOrPut EMPTY
                    if (id != "ENCHANTED_BOOK") return@getOrPut EMPTY

                    val enchs = data.getCompound("enchantments")?.getOrNull() ?: return@getOrPut EMPTY
                    if (enchs.size() != 1) return@getOrPut EMPTY

                    val entry = enchs.entrySet().firstOrNull() ?: return@getOrPut EMPTY
                    val name = entry.key
                    val ench = EnchantRegistry.getOrUnknownNbt(name)

                    val tier = entry.value.asInt()?.getOrNull() ?: 1
                    return@getOrPut ench to tier
                } ?: return@forEach
                if (data === EMPTY) return@forEach

                val f = minecraft.font

                event.ctx.pose()
                    .pushMatrix()
                    .translate(it.x + 14f, it.y + 10f)
                    .scale(SETTING_LEVEL_SCALE.get().toFloat())
                event.ctx.drawCenteredString(
                    f, "${data.second}",
                    0, 0, -1,
                )
                event.ctx.pose().popMatrix()

                event.ctx.pose()
                    .pushMatrix()
                    .translate(it.x + 0f, it.y + 0f)
                    .scale(SETTING_NAME_SCALE.get().toFloat())
                event.ctx.drawString(
                    f,
                    if (data.first is UltimateEnchant) data.first.getFormatted(1, data.first.abbreviation)
                    else Component.literal(data.first.abbreviation),
                    0, 0, -1,
                )
                event.ctx.pose().popMatrix()
            }
        }
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        cache.clear()
    }
}