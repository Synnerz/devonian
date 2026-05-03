package com.github.synnerz.devonian.features.misc

import com.github.synnerz.devonian.api.ItemUtils
import com.github.synnerz.devonian.api.ScreenUtils
import com.github.synnerz.devonian.api.SkyblockPrices
import com.github.synnerz.devonian.api.events.RenderOverlayEvent
import com.github.synnerz.devonian.api.events.TooltipRenderEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.hud.texthud.TextHudFeature
import com.github.synnerz.devonian.utils.StringUtils.colorCodes
import net.minecraft.ChatFormatting
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent
import net.minecraft.network.chat.Style
import net.minecraft.util.FormattedCharSequence
import net.minecraft.world.item.ItemStack
import java.util.WeakHashMap
import kotlin.jvm.optionals.getOrNull

object EstimatedValue : TextHudFeature(
    "estimatedValue",
    "Shows estimated value of an item",
    Categories.MISC,
    subcategory = "Inventory",
) {
    private val SETTING_SHOW_LORE = addSwitch(
        "showInLore",
        true,
        "Shows the estimation in lore",
        "Show in Lore"
    )
    private val SETTING_ONLY_LORE = addSwitch(
        "onlyLore",
        false,
        "Hides the hud only showing the lore estimation",
        "Only Lore"
    )
    private val masterStars = listOf("➊", "➋", "➌", "➍", "➎")
    private val masterStarIds = listOf(
        "FIRST_MASTER_STAR",
        "SECOND_MASTER_STAR",
        "THIRD_MASTER_STAR",
        "FOURTH_MASTER_STAR",
        "FIFTH_MASTER_STAR"
    )
    // TODO: probably limit this cache
    private val itemCache = WeakHashMap<ItemStack, ItemStatData>()
    private var lastItem: ItemStack? = null

    data class ItemStatData(
        val name: String, // formatted custom name component
        val unlockedGemstones: List<String> = listOf(), // gems
        val enchantments: List<String> = listOf(), // enchantments
        val ability_scrolls: List<String> = listOf(), // ability_scroll
        // TODO: whenever lb api supports runes add this
        val runes: Map<String, Int> = mapOf(), // runes
        val skyblockId: String, // id
        var hpbs: Int = 0, // hot_potato_count
        var dyeItem: String? = null, // dye_item
        var artOfPeace: Boolean = false, // artOfPeaceApplied
        var soulbound: Boolean = false, // donated_museum
        var stars: Int = 0, // upgrade_level | dungeon_item_level
        var recomb: Boolean = false, // rarity_upgrades
        var artOfWar: Boolean = false, // art_of_war_count
        var skin: String? = null, // skin
        var ultimateEnchant: String? = null,
        var isDungeonItem: Boolean = false,
        var enrichment: String? = null,
    ) {
        fun fuming(): Int = if (hpbs > 10) hpbs - 10 else 0

        fun hpb(): Int = hpbs - fuming()

        fun masterStars(): Int = if (!isDungeonItem || stars <= 5) 0 else stars - 5

        fun basePrice(): SkyblockPrices.CustomPriceData = SkyblockPrices.priceData(skyblockId)

        fun enchantmentPrice(): Double = enchantments.sumOf { SkyblockPrices.buyPrice(it).toDouble() }

        fun hpbPrice(): Double = SkyblockPrices.buyPrice("HOT_POTATO_BOOK").toDouble() * hpb()

        fun fumingPrice(): Double = SkyblockPrices.buyPrice("FUMING_POTATO_BOOK").toDouble() * fuming()

        fun ultimatePrice(): Double {
            if (ultimateEnchant == null) return 0.0
            var ultPrice = SkyblockPrices.buyPrice(ultimateEnchant!!).toDouble()
            if (ultPrice == 0.0)
                ultPrice = SkyblockPrices.buyPrice(
                    ultimateEnchant!!.replace("_\\d+$".toRegex(), "_1")
                ).toDouble() * 16

            return ultPrice
        }

        fun artOfWarPrice(): Double = SkyblockPrices.buyPrice("THE_ART_OF_WAR").toDouble()

        fun artOfPeacePrice(): Double = SkyblockPrices.buyPrice("THE_ART_OF_PEACE").toDouble()

        fun recombPrice(): Double = SkyblockPrices.buyPrice("RECOMBOBULATOR_3000").toDouble()

        fun starPrice(): Double {
            val starCount = masterStars()
            if (starCount == 0) return 0.0

            return masterStarIds.subList(0, starCount).sumOf { SkyblockPrices.buyPrice(it).toDouble() }
        }

        fun abilityScrollsPrice(): Double = ability_scrolls.sumOf { SkyblockPrices.buyPrice(it).toDouble() }

        fun enrichmentPrice(): Double =
            if (enrichment == null)
                0.0
            else
                SkyblockPrices.buyPrice("TALISMAN_ENRICHMENT_${enrichment}").toDouble()

        fun totalPrice(): Double {
            val basePriceData = basePrice()
            if (!basePriceData.auction)
                return -1.0
            var price = basePriceData.price.toDouble()
            if (price == 0.0)
                return -1.0

            if (enchantments.isNotEmpty())
                price += enchantmentPrice()
            if (hpbs > 0)
                price += hpbPrice()
            if (fuming() > 0)
                price += fumingPrice()
            if (ultimateEnchant != null)
                price += ultimatePrice()
            if (artOfWar)
                price += artOfWarPrice()
            if (artOfPeace)
                price += artOfPeacePrice()
            if (recomb)
                price += recombPrice()
            if (stars > 5 && isDungeonItem)
                price += starPrice()
            if (ability_scrolls.isNotEmpty())
                price += abilityScrollsPrice()

            if (price == basePriceData.price.toDouble())
                return -1.0
            return price
        }

        fun format(): List<String> {
            return buildList {
                val total = totalPrice()
                if (total == -1.0) return emptyList()

                add(name)
                if (ultimateEnchant != null)
                    add("&dUltimate Enchant&f: &a${shortNum(ultimatePrice())}")
                if (enchantments.isNotEmpty())
                    add("&9Enchantments &7(${enchantments.size})&f: &a${shortNum(enchantmentPrice())}")
                if (artOfPeace)
                    add("&6The Art of Peace&f: &a${shortNum(artOfPeacePrice())}")
                if (artOfWar)
                    add("&6The Art of War&f: &a${shortNum(artOfWarPrice())}")
                if (hpbs > 0)
                    add("&5Hot Potato Book &7(${hpb()})&f: &a${shortNum(hpbPrice())}")
                if (hpbs > 10)
                    add("&5Fuming Potato Book &7(${fuming()})&f: &a${shortNum(fumingPrice())}")
                if (stars > 5 && isDungeonItem)
                    add("&c${masterStars.subList(0, masterStars()).joinToString(" ")}&f: &a${shortNum(starPrice())}")
                if (ability_scrolls.isNotEmpty()) {
                    ability_scrolls.forEach {
                        val name = when (it) {
                            "SHADOW_WARP_SCROLL" -> "&5Shadow Warp"
                            "IMPLOSION_SCROLL" -> "&5Implosion"
                            "WITHER_SHIELD_SCROLL" -> "&5Wither Shield"
                            else -> return@forEach
                        }
                        add("&7- $name&f: &a${shortNum(SkyblockPrices.buyPrice(it).toDouble())}")
                    }
                }
                if (enrichment != null)
                    add("&9Enrichment&f: &a${shortNum(enrichmentPrice())}")
                if (recomb)
                    add("&6Recombobulator 3000&f: &a${shortNum(recombPrice())}")
                add("&6Base Item&f: &a${shortNum(basePrice().price.toDouble())}")
                add("&bTotal&f: &a${shortNum(total)}")
            }
        }
    }

    override fun initialize() {
        // TODO: finish this impl
        //  add gemstone slots
        //  fix cumulative enchants not working
        //  add dyes
        //  add skins
        on<TooltipRenderEvent> { event ->
            val itemStack = event.item ?: return@on
            val extraAttributes = ItemUtils.extraAttributes(itemStack) ?: return@on
            val sbId = extraAttributes.getString("id").getOrNull() ?: return@on
            lastItem = itemStack
            val cacheData = itemCache[itemStack]
            if (cacheData != null) {
                val total = cacheData.totalPrice()
                if (total == -1.0) return@on

                if (SETTING_SHOW_LORE.get()) {
                    event.lore.add(ClientTooltipComponent.create(FormattedCharSequence.composite(
                        FormattedCharSequence.forward("Price Est", Style.EMPTY.withColor(ChatFormatting.AQUA)),
                        FormattedCharSequence.forward(": ", Style.EMPTY.withColor(ChatFormatting.WHITE)),
                        FormattedCharSequence.forward(shortNum(total), Style.EMPTY.withColor(ChatFormatting.GREEN)),
                    )))
                }

                clearLines()
                addLines(cacheData.format())
                return@on
            }
            val enchantments = extraAttributes.getCompound("enchantments").getOrNull()
            var ultimateEnchant: String? = null
            val enchantIds: List<String>? = if (enchantments == null) null else buildList {
                enchantments.forEach { name, tag ->
                    val level = tag.asInt().getOrNull() ?: return@forEach
                    if (name.contains("ultimate", ignoreCase = true)) {
                        ultimateEnchant = "ENCHANTMENT_${name.uppercase()}_${level}"
                        return@forEach
                    }
                    add("ENCHANTMENT_${name.uppercase()}_${level}")
                }
            }
            val artOfWar = extraAttributes.getInt("art_of_war_count").getOrNull()
            val artOfPeace = extraAttributes.getInt("artOfPeaceApplied").getOrNull()
            val hpbs = extraAttributes.getInt("hot_potato_count").getOrNull()
            val stars = extraAttributes.getInt("upgrade_level").getOrNull()
                ?: extraAttributes.getInt("dungeon_item_level").getOrNull()
            val recomb = extraAttributes.getInt("rarity_upgrades").getOrNull()
            val abilityScrolls = extraAttributes.getList("ability_scroll").getOrNull()
            val enrichment = extraAttributes.getString("talisman_enrichment").getOrNull()?.uppercase()
            val data = ItemStatData(
                itemStack.customName?.colorCodes() ?: itemStack.itemName.string,
                enchantments = enchantIds ?: emptyList(),
                skyblockId = sbId,
                hpbs = hpbs ?: 0,
                artOfPeace = artOfPeace != null,
                stars = stars ?: 0,
                recomb = recomb != null,
                artOfWar = artOfWar != null,
                ultimateEnchant = ultimateEnchant,
                // lore is more accurate than nbt ):
                isDungeonItem = (ItemUtils.lore(itemStack) ?: emptyList()).any { it.contains(" DUNGEON ") },
                ability_scrolls = abilityScrolls?.map { it.asString().getOrNull() ?: "" } ?: emptyList(),
                enrichment = enrichment,
            )

            itemCache[itemStack] = data
            if (data.totalPrice() == -1.0) return@on

            if (SETTING_SHOW_LORE.get())
                event.lore.add(ClientTooltipComponent.create(FormattedCharSequence.composite(
                    FormattedCharSequence.forward("Price Est", Style.EMPTY.withColor(ChatFormatting.AQUA)),
                    FormattedCharSequence.forward(": ", Style.EMPTY.withColor(ChatFormatting.WHITE)),
                    FormattedCharSequence.forward(shortNum(data.totalPrice()), Style.EMPTY.withColor(ChatFormatting.GREEN)),
                )))

            clearLines()
            addLines(data.format())
        }

        on<RenderOverlayEvent> {
            if (SETTING_ONLY_LORE.get() || lastItem == null) return@on
            val cursorItem = minecraft.screen?.let { ScreenUtils.cursorStack(it) } ?: return@on
            if (cursorItem != lastItem) {
                lastItem = null
                clearLines()
                return@on
            }
            draw(it.ctx)
        }
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        itemCache.clear()
    }

    override fun getEditText(): List<String> = listOf(
        "&r&dPrecise Terminator &r&6✪✪✪✪✪&r&c➎",
        "&9Enchantments&r &7(17)&f: &a100,000",
        "&6The Art of War&f: &a100,000",
        "&5Hot Potato Book &7(10)&f: &a100,000",
        "&5Fuming Potato Book &7(5)&f: &a100,000",
        "&c➊ ➋ ➌ ➍ ➎&f: &a100,000",
        "&6Recombobulator 3000&f: &a100,000",
        "&6Base Item&f: &a100,000",
        "&bTotal&f: &a100,000"
    )

    private fun shortNum(num: Double): String {
        if (num < 0f) return "0"
        if (num < 1000f) return "$num"
        var num = num / 1000f

        if (num < 10f) return "%.2fK".format(num)
        if (num < 100f) return "%.1fK".format(num)
        if (num < 1000f) return "%.0fK".format(num)

        num /= 1000f
        if (num < 10f) return "%.2fM".format(num)
        if (num < 100f) return "%.1fM".format(num)
        if (num < 1000f) return "%.0fM".format(num)

        num /= 1000f
        return "%.2fB".format(num)
    }
}