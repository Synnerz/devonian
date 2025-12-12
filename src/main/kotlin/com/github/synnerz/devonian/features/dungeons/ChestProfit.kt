package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.devonian.api.ItemUtils
import com.github.synnerz.devonian.api.Scheduler
import com.github.synnerz.devonian.api.SkyblockPrices
import com.github.synnerz.devonian.api.events.PacketReceivedEvent
import com.github.synnerz.devonian.api.events.RenderOverlayEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.hud.texthud.TextHudFeature
import com.github.synnerz.devonian.utils.StringUtils
import com.github.synnerz.devonian.utils.StringUtils.clearCodes
import com.github.synnerz.devonian.utils.StringUtils.colorCodes
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket
import net.minecraft.world.item.Items
import kotlin.math.roundToInt

object ChestProfit : TextHudFeature(
    "chestProfit",
    "Displays the amount of profit that is in the dungeon chests you currently have opened (only works inside dungeons)",
    Categories.DUNGEONS,
    "catacombs",
    subcategory = "HUD",
) {
    private val SETTING_USE_ESSENCE_PROFIT = addSwitch(
        "useEssenceProfit",
        true,
        "Whether to use Wither/Undead essence for profit",
        "Chest Profit Essence",
    )
    private val SETTING_COMPACT_MODE = addSwitch(
        "compactMode",
        false,
        "Compact mode stops showing the entire item list and only shows the chest name with its profit",
        "Chest Profit Compact Mode",
    )
    private val chestNames = listOf(
        "Wood Chest",
        "Gold Chest",
        "Diamond Chest",
        "Emerald Chest",
        "Obsidian Chest",
        "Bedrock Chest"
    )
    private val essenceRegex = "^(Wither|Undead) Essence x(\\d+)\$".toRegex()
    private val enchantedBookRegex = "^([\\w ]+) ([IVXLCDM]+)$".toRegex()
    private val ultimateEnchants = listOf(
        "Wisdom",
        "Swarm",
        "Soul Eater",
        "Rend",
        "One For All",
        "No Pain No Gain",
        "Legion",
        "Last Stand",
        "Combo",
        "Bank"
    )
    private val formattedNames = mapOf(
        "Wood Chest" to "&fWood Chest",
        "Gold Chest" to "&6Gold Chest",
        "Diamond Chest" to "&bDiamond Chest",
        "Emerald Chest" to "&2Emerald Chest",
        "Obsidian Chest" to "&5Obsidian Chest",
        "Bedrock Chest" to "&8Bedrock Chest"
    )
    val currentChestData = mapOf(
        "Wood Chest" to ChestData("&fWood Chest&r"),
        "Gold Chest" to ChestData("&6Gold Chest&r"),
        "Diamond Chest" to ChestData("&bDiamond Chest&r"),
        "Emerald Chest" to ChestData("&2Emerald Chest&r"),
        "Obsidian Chest" to ChestData("&5Obsidian Chest&r"),
        "Bedrock Chest" to ChestData("&8Bedrock Chest&r"),
    )
    var inChest = false
    var currentChest: String? = null

    data class ItemData(
        val itemName: String,
        val sbId: String,
        val amount: Int,
        val isEssence: Boolean
    ) {
        fun price(): Int {
            if (isEssence && !SETTING_USE_ESSENCE_PROFIT.get()) return 0
            return SkyblockPrices.buyPrice(sbId).roundToInt() * amount
        }
    }

    data class ChestData(
        val name: String, // formatted name
        val itemData: MutableList<ItemData> = mutableListOf(),
        var chestPrice: Int = 0
    ) {
        fun profit(): Int {
            val totalPrices = itemData.sumOf { it.price() }
            return totalPrices - chestPrice
        }
    }

    override fun initialize() {
        on<PacketReceivedEvent> { event ->
            val packet = event.packet
            if (packet is ClientboundContainerSetContentPacket) {
                if (!inChest) return@on

                val chestItem = packet.items.getOrNull(31) ?: return@on
                if (chestItem.item != Items.CHEST) return@on
                val chestLore = ItemUtils.lore(chestItem) ?: return@on
                var chestPrice = 0
                val costIdx = chestLore.indexOf("Cost")
                if (costIdx == -1) return@on

                chestLore[costIdx + 1].replace("(,+| Coins)".toRegex(), "").also {
                    chestPrice +=
                        if (it.isEmpty() || !"\\d+".toRegex().matches(it)) 0
                        else it.toInt()
                }
                chestLore[costIdx + 2].also {
                    if (it != "Dungeon Chest Key") return@also
                    chestPrice += SkyblockPrices.buyPrice("DUNGEON_CHEST_KEY").roundToInt()
                }

                val currentData = currentChestData[currentChest!!] ?: return@on
                currentData.chestPrice = chestPrice

                for (idx in 9..17) {
                    val itemStack = packet.items[idx] ?: continue
                    if (
                        itemStack.item == Items.BLACK_STAINED_GLASS_PANE ||
                        itemStack.item == Items.GRAY_STAINED_GLASS_PANE ||
                        itemStack.isEmpty
                    ) continue

                    val customName = itemStack.customName ?: continue
                    val customNameStr = customName.string ?: continue
                    val isEnchantedBook = customNameStr == "Enchanted Book"
                    val itemName =
                        if (isEnchantedBook)
                            ItemUtils.lore(itemStack, true)?.firstOrNull() ?: continue
                        else
                            customName.colorCodes()

                    var sbId = ItemUtils.skyblockId(itemStack)
                    var amount = 1

                    if (isEnchantedBook) {
                        val match = enchantedBookRegex.matchEntire(itemName.clearCodes())?.groupValues?.drop(1) ?: continue
                        val enchantName = match[0]
                        val enchantLevel = StringUtils.parseRoman(match[1])
                        val additionalName = if (ultimateEnchants.contains(enchantName)) "_ULTIMATE_" else ""

                        sbId = "ENCHANTMENT_"

                        if (additionalName.isNotEmpty()) sbId += additionalName
                        sbId += enchantName.uppercase().replace(" ", "_")
                        sbId += "_${enchantLevel}"
                    }
                    if (sbId == null && itemName.contains(" Essence ")) {
                        val match = essenceRegex.matchEntire(itemName.clearCodes())?.groupValues?.drop(1) ?: continue
                        sbId = "ESSENCE_${match[0].uppercase()}"
                        amount = match[1].toInt()
                    }

                    if (sbId == null) continue

                    currentData.itemData.add(ItemData(
                        itemName,
                        sbId,
                        amount,
                        itemName.contains(" Essence ")
                    ))
                }

                Scheduler.scheduleTask { updateDisplay() }
                inChest = false
                return@on
            }
            if (packet !is ClientboundOpenScreenPacket) return@on

            inChest = chestNames.contains(packet.title.string)
            if (inChest) {
                currentChest = packet.title.string

                val data = currentChestData[currentChest] ?: return@on
                data.itemData.clear()
                data.chestPrice = 0
                Scheduler.scheduleTask { updateDisplay() }
            }
        }

        on<RenderOverlayEvent> {
            if (currentChest == null) return@on
            draw(it.ctx)
        }
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        inChest = false
        currentChest = null
        clearLines()
        reset()
    }

    // i was too lazy to make actual example
    override fun getEditText(): List<String> = listOf("&5Obsidian Chest", "&aA", "&aLife", "&bProfit&f: &a100")

    private fun reset() {
        for (data in currentChestData) {
            data.value.itemData.clear()
            data.value.chestPrice = 0
        }
    }

    private fun updateDisplay() {
        // TODO: add sort by most profitable
        clearLines()
        for (data in currentChestData) {
            val v = data.value
            val items = v.itemData
            if (items.isEmpty()) continue
            addLine(v.name)
            if (!SETTING_COMPACT_MODE.get())
                addLines(items.map { "  ${it.itemName}  " })
            val profit = v.profit()
            addLine("&bProfit&f: ${if (profit < 0) "&c" else "&a"}${StringUtils.addCommas(profit)}")
        }
    }
}