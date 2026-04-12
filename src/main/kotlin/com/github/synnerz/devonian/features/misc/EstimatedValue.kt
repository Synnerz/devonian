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
import java.util.Optional
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
    // TODO: add caching
//    private val itemData = hashMapOf<String, ItemStatData>()
    private var lastItem: ItemStack? = null

    data class ItemStatData(
        val unlockedGemstones: List<String> = listOf(), // gems
        val enchantments: List<String> = listOf(), // enchantments
        val ability_scrolls: List<String> = listOf(), // ability_scroll
        // TODO: whenever lb api supports runes add this
        val runes: Map<String, Int> = mapOf(), // runes
        val skyblockId: String, // id
        var hpbs: Int = 0, // hot_potato_count
        var dye_item: String? = null, // dye_item
        var artOfPeace: Boolean = false, // artOfPeaceApplied
        var soulbound: Boolean = false, // donated_museum
        var stars: Int = 0, // upgrade_level | dungeon_item_level
        var recomb: Boolean = false, // rarity_upgrades
        var artOfWar: Boolean = false, // art_of_war_count
        var skin: String? = null, // skin
        var ultimateEnchant: String? = null,
    )

    override fun initialize() {
        // TODO: finish this impl
        //  add gemstone slots
        //  fix cumulative enchants not working
        //  wither blades scrolls
        //  fix equipments with master stars not being detected because they're "not" dungeon items
        //  optimize
        //  optimize
        //  optimize
        on<TooltipRenderEvent> { event ->
            val itemStack = event.item ?: return@on
            val extraAttributes = ItemUtils.extraAttributes(itemStack) ?: return@on
            val sbId = extraAttributes.getString("id").safeGet() ?: return@on
            // return for now, maybe change this later ? items like tact don't have uuid
            val uuid = extraAttributes.getString("uuid").safeGet() ?: return@on
            lastItem = itemStack
            val enchantments = extraAttributes.getCompound("enchantments").safeGet()
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
            val artOfWar = extraAttributes.getInt("art_of_war_count").safeGet()
            val hpbs = extraAttributes.getInt("hot_potato_count").safeGet()
            val stars = extraAttributes.getInt("upgrade_level").safeGet()
                ?: extraAttributes.getInt("dungeon_item_level").safeGet()
            val recomb = extraAttributes.getInt("rarity_upgrades").safeGet()
            val dungeonItem = extraAttributes.getInt("dungeon_item").safeGet()

            clearLines()
            addLines(buildList {
                var totalPrice = 0.0
                val enchPrice = enchantIds?.sumOf { SkyblockPrices.buyPrice(it).toDouble() } ?: 0.0
                totalPrice += enchPrice

                add(itemStack.customName?.colorCodes() ?: itemStack.itemName.string)

                if (ultimateEnchant != null) {
                    var ultPrice = SkyblockPrices.buyPrice(ultimateEnchant!!).toDouble()
                    if (ultPrice == 0.0)
                        ultPrice = SkyblockPrices.buyPrice(
                            ultimateEnchant!!.replace("_\\d+$".toRegex(), "_1")
                        ).toDouble() * 16
                    totalPrice += ultPrice
                    add("&dUltimate Enchant&f: &a${shortNum(ultPrice)}")
                }
                if (!enchantIds.isNullOrEmpty())
                    add("&9Enchantments &7(${enchantIds.size})&f: &a${shortNum(enchPrice)}")
                if (artOfWar != null) {
                    val aow = SkyblockPrices.buyPrice("THE_ART_OF_WAR").toDouble()
                    totalPrice += aow
                    add("&6The Art of War&f: &a${shortNum(aow)}")
                }
                if (hpbs != null) {
                    val hasFumings = hpbs > 10
                    val fumings = if (hasFumings) hpbs - 10 else 0
                    val hotPotatos = hpbs - fumings
                    val hprice = SkyblockPrices.buyPrice("HOT_POTATO_BOOK").toDouble() * hotPotatos
                    val fprice = if (hasFumings) SkyblockPrices.buyPrice("FUMING_POTATO_BOOK").toDouble() * fumings else 0.0
                    totalPrice += hprice
                    totalPrice += fprice

                    add("&5Hot Potato Book &7(${hotPotatos})&f: &a${shortNum(hprice)}")
                    if (fumings != 0)
                        add("&5Fuming Potato Book &7($fumings)&f: &a${shortNum(fprice)}")
                }
                if (stars != null && dungeonItem != null && stars > 5) {
                    val starCount = stars - 5
                    val totalPrice = masterStarIds.subList(0, starCount).sumOf { SkyblockPrices.buyPrice(it).toDouble() }
                    val str = masterStars.subList(0, starCount)
                    add("&c${str.joinToString(" ")}&f: &a${shortNum(totalPrice)}")
                }
                if (recomb != null) {
                    val recombPrice = SkyblockPrices.buyPrice("RECOMBOBULATOR_3000").toDouble()
                    totalPrice += recombPrice
                    add("&6Recombobulator 3000&f: &a${shortNum(recombPrice)}")
                }
                val basePrice = SkyblockPrices.buyPrice(sbId).toDouble()
                totalPrice += basePrice
                add("&6Base Item&f: &a${shortNum(basePrice)}")
                add("&bTotal&f: &a${shortNum(totalPrice)}")
                if (SETTING_SHOW_LORE.get())
                    event.lore.add(ClientTooltipComponent.create(FormattedCharSequence.composite(
                        FormattedCharSequence.forward("Price Est", Style.EMPTY.withColor(ChatFormatting.AQUA)),
                        FormattedCharSequence.forward(": ", Style.EMPTY.withColor(ChatFormatting.WHITE)),
                        FormattedCharSequence.forward(shortNum(totalPrice), Style.EMPTY.withColor(ChatFormatting.GREEN)),
                    )))
            })
        }

        on<RenderOverlayEvent> {
            if (SETTING_ONLY_LORE.get()) return@on
            val cursorItem = minecraft.screen?.let { ScreenUtils.cursorStack(it) } ?: return@on
            if (cursorItem != lastItem) return@on
            draw(it.ctx)
        }
    }

    override fun onWorldChange(event: WorldChangeEvent) {
//        itemData.clear()
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

    private fun <T> Optional<T>.safeGet(): T? = if (this.isEmpty) null else this.get()

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