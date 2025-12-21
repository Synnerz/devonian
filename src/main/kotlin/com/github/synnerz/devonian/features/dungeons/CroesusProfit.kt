package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.devonian.api.ItemUtils
import com.github.synnerz.devonian.api.Scheduler
import com.github.synnerz.devonian.api.SkyblockPrices
import com.github.synnerz.devonian.api.events.*
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.hud.texthud.TextHudFeature
import com.github.synnerz.devonian.utils.StringUtils
import net.minecraft.network.protocol.game.ClientboundContainerClosePacket
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket
import net.minecraft.world.item.Items
import java.awt.Color
import kotlin.math.roundToInt

object CroesusProfit : TextHudFeature(
    "croesusProfit",
    "Shows the profit of the current chest(s) opened in croesus",
    Categories.DUNGEONS,
    "Dungeon Hub",
    subcategory = "HUD"
) {
    private val SETTING_USE_ESSENCE_PROFIT = addSwitch(
        "useEssenceProfit",
        true,
        "Whether to use Wither/Undead essence for profit",
        "Croesus Profit Essence",
    )
    private val SETTING_COMPACT_MODE = addSwitch(
        "compactMode",
        false,
        "Compact mode stops showing the entire item list and only shows the chest name with its profit",
        "Croesus Profit Compact Mode",
    )
    private val chestRegex = "^(?:Master )?Catacombs - Floor [IV]+$".toRegex()
    private val enchantedBookRegex = "^Enchanted Book \\(([\\w ]+) ([IV]+)\\)$".toRegex()
    private val essenceRegex = "^(Wither|Undead) Essence x(\\d+)$".toRegex()
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
    )
    private val chestsData = mapOf(
        "Wood" to ChestData("&fWood"),
        "Gold" to ChestData("&6Gold"),
        "Diamond" to ChestData("&bDiamond"),
        "Emerald" to ChestData("&2Emerald"),
        "Obsidian" to ChestData("&5Obsidian"),
        "Bedrock" to ChestData("&8Bedrock"),
    )
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
    ) {
        fun totalProfit(): Int = items.sumOf { it.price() } - chestPrice
    }

    override fun initialize() {
        on<PacketReceivedEvent> { event ->
            val packet = event.packet
            if (packet is ClientboundOpenScreenPacket) {
                inChest = packet.title.string.matches(chestRegex)
                if (!inChest) Scheduler.scheduleTask {
                    clearLines()
                    reset()
                }
                return@on
            }

            if (packet is ClientboundContainerClosePacket) {
                Scheduler.scheduleTask {
                    clearLines()
                    reset()
                }
                return@on
            }

            if (packet !is ClientboundContainerSetContentPacket) return@on
            if (!inChest) return@on

            val items = packet.items

            for (idx in 9..18) {
                val itemStack = items.getOrNull(idx) ?: continue
                if (itemStack.item != Items.PLAYER_HEAD) continue

                val chestName = itemStack.customName?.string ?: continue
                val lore = ItemUtils.lore(itemStack) ?: continue
                val formatLore = ItemUtils.lore(itemStack, true) ?: continue
                val data = chestsData[chestName] ?: continue
                data.slotIdx = idx

                for (jdx in 0..lore.lastIndex) {
                    val line = lore.getOrNull(jdx) ?: continue
                    if (line == "Contents") continue
                    if (line.isBlank()) continue

                    if (line == "Already opened!") {
                        // chest has already been opened do something
                        break
                    }

                    if (line == "Cost") {
                        val chestPriceLore = lore[jdx + 1]
                        val possibleKey = lore[jdx + 2]
                        var price = "^(\\d[\\d,]+) Coins\$".toRegex()
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

            inChest = false
            Scheduler.scheduleTask {
                val sorted = chestsData.values.sortedByDescending { it.totalProfit() }
                mostProfitable = sorted.firstOrNull()?.slotIdx ?: -1
                secondMostProfitable = sorted.getOrNull(1)?.slotIdx ?: -1
                updateDisplay()
            }
        }

        on<PacketSentEvent> { event ->
            val packet = event.packet
            if (packet !is ServerboundContainerClosePacket) return@on

            Scheduler.scheduleTask {
                clearLines()
                reset()
            }
        }

        on<RenderOverlayEvent> {
            if (chestsData["Wood"]!!.items.isEmpty()) return@on
            draw(it.ctx)
        }

        on<RenderSlotEvent> { event ->
            val slot = event.slot
            if (secondMostProfitable != -1 && slot.containerSlot == secondMostProfitable) {
                if (slot.container == minecraft.player?.inventory) return@on
                event.ctx.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, SECOND_PROFITABLE_COLOR)
                return@on
            }

            if (mostProfitable == -1 || slot.containerSlot != mostProfitable) return@on
            if (slot.container == minecraft.player?.inventory) return@on

            event.ctx.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, PROFITABLE_COLOR)
        }.prio = -1
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
        }
        mostProfitable = -1
        secondMostProfitable = -1
    }

    private fun updateDisplay() {
        clearLines()
        for (data in chestsData) {
            val v = data.value
            val items = v.items
            if (items.isEmpty()) continue
            val profit = v.totalProfit()

            addLine(v.chestName)
            if (!SETTING_COMPACT_MODE.get())
                addLines(items.map { "  ${it.name}  " })
            if (v.requiresKey)
                addLine("&9+ Dungeon Chest Key ")
            addLine("&bProfit&f: ${if (profit < 0) "&c" else "&a"}${StringUtils.addCommas(profit)}")
        }
    }
}