package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.devonian.api.ItemUtils
import com.github.synnerz.devonian.api.Scheduler
import com.github.synnerz.devonian.api.SkyblockPrices
import com.github.synnerz.devonian.api.events.*
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.hud.texthud.TextHudFeature
import com.github.synnerz.devonian.utils.StringUtils
import com.github.synnerz.devonian.utils.render.Render2D
import com.github.synnerz.devonian.utils.render.Render2D.width
import net.minecraft.world.item.Items
import java.awt.Color
import kotlin.math.roundToInt

object CroesusProfit : TextHudFeature(
    "croesusProfit",
    "Shows the profit of the current chest(s) opened in croesus.",
    Categories.DUNGEONS,
    "Dungeon Hub",
    subcategory = "HUD",
) {
    private val SETTING_USE_ESSENCE_PROFIT = addSwitch(
        "useEssenceProfit",
        true,
        "Whether to use Wither/Undead essence for profit.",
        "Croesus Profit Essence",
    )
    private val SETTING_COMPACT_MODE = addSwitch(
        "compactMode",
        false,
        "Compact mode stops showing the entire item list and only shows the chest name with its profit.",
        "Croesus Profit Compact Mode",
    )
    private val chestRegex = "^(?:Master )?Catacombs - Floor [IV]+$".toRegex()
    private val enchantedBookRegex = "^Enchanted Book \\(([\\w ]+) ([IV]+)\\)$".toRegex()
    private val essenceRegex = "^(Wither|Undead) Essence x(\\d+)$".toRegex()
    private val costRegex = "^(\\d[\\d,]+) Coins$".toRegex()
    private val specialIds = mapOf(
        // big thank Unclaimed NOOB Six
        "WITHER_SHARD" to "SHARD_WITHER",
        "THORN_SHARD" to "SHARD_THORN",
        "APEX_DRAGON_SHARD" to "SHARD_APEX_DRAGON",
        "POWER_DRAGON_SHARD" to "SHARD_POWER_DRAGON",
        "SCARF_SHARD" to "SHARD_SCARF",
        "NECROMANCERS_BROOCH" to "NECROMANCER_BROOCH",
        "WITHER_SHIELD" to "WITHER_SHIELD_SCROLL",
        "IMPLOSION" to "IMPLOSION_SCROLL",
        "SHADOW_WARP" to "SHADOW_WARP_SCROLL",
        "WARPED_STONE" to "AOTE_STONE",
        "SPIRIT_STONE" to "SPIRIT_DECOY",
    )
    private val chestsData = mapOf(
        "Wood" to ChestData("&fWood"),
        "Gold" to ChestData("&6Gold"),
        "Diamond" to ChestData("&bDiamond"),
        "Emerald" to ChestData("&2Emerald"),
        "Obsidian" to ChestData("&5Obsidian"),
        "Bedrock" to ChestData("&8Bedrock"),
    )
    private val chestSlots = mutableMapOf<Int, String>()
    private val PROFITABLE_COLOR = Color.GREEN.rgb
    private val SECOND_PROFITABLE_COLOR = Color.YELLOW.rgb
    private var inChest = false
    private var mostProfitable = -1
    private var secondMostProfitable = -1

    data class ChestItemData(
        val name: String, // formatted lore line
        val price: Int = 0,
        val isEssence: Boolean = false,
    ) {
        fun price(): Int {
            if (isEssence && !SETTING_USE_ESSENCE_PROFIT.get()) return 0
            return price
        }
    }

    data class ChestData(
        val chestName: String,
        val items: MutableList<ChestItemData> = mutableListOf(),
        var chestPrice: Int = 0,
        var requiresKey: Boolean = false,
        var slotIdx: Int = -1,
        var bought: Boolean = false,
    ) {
        fun totalProfit(): Int = if (bought) Int.MIN_VALUE else  items.sumOf { it.price() } - chestPrice
    }

    override fun initialize() {
        on<ServerContainerOpenEvent> { event ->
            inChest = event.titleStr.matches(chestRegex)
            if (!inChest) Scheduler.scheduleTask {
                clearLines()
                reset()
            }
        }

        on<ServerContainerCloseEvent> {
            Scheduler.scheduleTask {
                clearLines()
                reset()
            }
        }

        on<ClientContainerCloseEvent> {
            clearLines()
            reset()
        }

        on<ServerContainerSetSlotEvent> { event ->
            if (!inChest) return@on
            val slot = event.slot
            if (slot !in 9..18) return@on
            val itemStack = event.itemStack
            if (itemStack.item != Items.PLAYER_HEAD) return@on

            val chestName = itemStack.customName?.string ?: return@on
            val lore = ItemUtils.lore(itemStack) ?: return@on
            val formatLore = ItemUtils.lore(itemStack, true) ?: return@on
            val data = chestsData[chestName] ?: return@on
            chestSlots[slot] = chestName
            data.slotIdx = slot

            for (jdx in 0..lore.lastIndex) {
                val line = lore.getOrNull(jdx) ?: continue
                if (line == "Contents") continue
                if (line.isBlank()) continue

                if (line == "Already opened!") {
                    data.bought = true
                    // chest has already been opened do something
                    break
                }

                if (line == "Cost") {
                    val chestPriceLore = lore[jdx + 1]
                    val possibleKey = lore[jdx + 2]
                    var price = costRegex
                        .matchEntire(chestPriceLore)
                        ?.groupValues
                        ?.drop(1)
                        ?.getOrNull(0)
                        ?.replace(",", "")
                        ?.toIntOrNull() ?: 0
                    if (possibleKey == "Dungeon Chest Key") {
                        price += SkyblockPrices.buyPrice("DUNGEON_CHEST_KEY").roundToInt()
                        data.requiresKey = true
                    }

                    data.chestPrice = price
                    Scheduler.scheduleTask {
                        val sorted = chestsData.values.map { it to it.totalProfit() }.sortedByDescending { it.second }
                        mostProfitable = sorted.firstOrNull()?.let {
                            if (it.second <= 0) -1 else it.first.slotIdx
                        } ?: -1
                        secondMostProfitable = sorted.getOrNull(1)?.let {
                            if (it.second <= 0) -1 else it.first.slotIdx
                        } ?: -1
                        updateDisplay()
                    }
                    break
                }

                val enchantMatch = enchantedBookRegex.matchEntire(line)?.groupValues?.drop(1)
                if (enchantMatch != null) {
                    val name = enchantMatch[0]
                    val numeral = enchantMatch[1]
                    val tier = StringUtils.parseRoman(numeral)
                    val cleanName = name.replace(" ", "_").uppercase()
                    var price = SkyblockPrices.buyPrice("ENCHANTMENT_${cleanName}_$tier").roundToInt()
                    if (price == 0)
                        price = SkyblockPrices.buyPrice("ENCHANTMENT_ULTIMATE_${cleanName}_$tier").roundToInt()

                    data.items.add(ChestItemData(formatLore[jdx], price))
                    continue
                }

                val essenceMatch = essenceRegex.matchEntire(line)?.groupValues?.drop(1)
                if (essenceMatch != null) {
                    val type = essenceMatch[0].uppercase()
                    val amount = essenceMatch[1].toIntOrNull() ?: continue
                    val price = (SkyblockPrices.buyPrice("ESSENCE_$type") * amount).roundToInt()

                    data.items.add(ChestItemData(formatLore[jdx], price, true))
                    continue
                }

                var itemId = line
                    .uppercase()
                    .replace("- ", "")
                    .replace("'", "")
                    .replace(" ", "_")
                if (itemId in specialIds) itemId = specialIds[itemId]!!

                val price = SkyblockPrices.buyPrice(itemId).roundToInt()

                // TODO: probably make this a toggle
                if (price == 0) {
                    // FIXME: Devonian$CroesusProfit[status=Item Not Found, name="NECROMANCERS_BROOCH", line="Necromancer's Brooch"]
                    //  fix whenever not feeling lazy since this requires to have
                    //  a failsafe check because most items with ' in them is just remove but this one removes
                    //  the "s" as well
                    println("Devonian\$CroesusProfit[status=Item Not Found, name=\"$itemId\", line=\"$line\"]")
                    continue
                }

                data.items.add(ChestItemData(formatLore[jdx], price))
            }
        }

        on<PostRenderGuiEvent> { event ->
            if (!inChest) return@on
            draw(event.ctx)
        }

        on<RenderSlotEvent> { event ->
            val slot = event.slot
            if (event.isInventory()) return@on

            val color = when (slot.containerSlot) {
                mostProfitable -> PROFITABLE_COLOR
                secondMostProfitable -> SECOND_PROFITABLE_COLOR
                else -> return@on
            }

            event.ctx.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, color)
        }.prio = -1

        on<PostRenderSlotsEvent> { event ->
            val inv = minecraft.player?.inventory ?: return@on
            event.container.menu.slots.forEach { slot ->
                if (slot.container === inv) return@forEach

                val name = chestSlots[slot.containerSlot] ?: return@forEach
                val data = chestsData[name] ?: return@forEach
                if (data.bought) return@forEach
                val profit = data.totalProfit()

                val str = StringUtils.shortenNumber(profit)
                val color = when {
                    profit <= 0 -> "§c"
                    profit <= 500_000 -> "§e"
                    profit <= 10_000_000 -> "§a"
                    else -> "§z"
                }
                val w = str.width()
                Render2D.drawString(
                    event.ctx,
                    "$color$str",
                    slot.x + 8 - w / 4,
                    slot.y + 20,
                    0.5f,
                )
            }
        }
    }

    // lazy part 2
    override fun getEditText(): List<String> = listOf("&fCroesus Profit", "&5Obsidian Chest", "&aA", "&aLife", "&bProfit&f: &a100")

    override fun onWorldChange(event: WorldChangeEvent) {
        inChest = false
        clearLines()
        reset()
    }

    private fun reset() {
        for (data in chestsData) {
            val v = data.value
            v.items.clear()
            v.chestPrice = 0
            v.requiresKey = false
            v.slotIdx = -1
            v.bought = false
        }
        chestSlots.clear()
        mostProfitable = -1
        secondMostProfitable = -1
    }

    private fun updateDisplay() {
        clearLines()
        for (data in chestsData.entries.toList().sortedByDescending { it.value.totalProfit() }) {
            val v = data.value
            if (v.bought) continue
            val items = v.items
            if (items.isEmpty()) continue
            val profit = v.totalProfit()

            if (SETTING_COMPACT_MODE.get()) {
                val chestName =
                    if (v.requiresKey) "${v.chestName} &7(&9*&7)&f:"
                    else v.chestName
                addLine("${chestName}&f: ${if (profit < 0) "&c" else "&a"}${StringUtils.addCommas(profit)}")
                continue
            }
            addLine(v.chestName)
            addLines(items.map { "  ${it.name}  " })
            if (v.requiresKey)
                addLine("&9+ Dungeon Chest Key ")
            addLine("&bProfit&f: ${if (profit < 0) "&c" else "&a"}${StringUtils.addCommas(profit)}")
        }
    }
}