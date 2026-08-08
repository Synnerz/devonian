package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.devonian.api.ChatUtils
import com.github.synnerz.devonian.api.ItemUtils
import com.github.synnerz.devonian.api.Location
import com.github.synnerz.devonian.api.SkyblockPrices
import com.github.synnerz.devonian.api.dungeon.Dungeons
import com.github.synnerz.devonian.api.dungeon.FloorType
import com.github.synnerz.devonian.api.events.ChatEvent
import com.github.synnerz.devonian.api.events.ClientContainerCloseEvent
import com.github.synnerz.devonian.api.events.ServerContainerCloseEvent
import com.github.synnerz.devonian.api.events.ServerContainerOpenEvent
import com.github.synnerz.devonian.api.events.ServerContainerSetSlotEvent
import com.github.synnerz.devonian.commands.DevonianCommand
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.utils.BasicState
import com.github.synnerz.devonian.utils.PersistentJsonClass
import com.github.synnerz.devonian.utils.StringUtils
import com.github.synnerz.devonian.utils.StringUtils.clearCodes
import com.github.synnerz.devonian.utils.StringUtils.colorCodes
import com.google.gson.reflect.TypeToken
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import java.time.LocalDateTime
import kotlin.math.roundToInt

object LootLogger : Feature(
    "lootLogger",
    "Logs the loot you purchase from dungeon chests (/dv lootlogger <mode> <floor> <date>)",
    Categories.DUNGEONS,
    subcategory = "QOL"
) {
    override fun createRequirements(): List<BasicState<Boolean>?> {
        return super.createRequirements() + listOf(Location.stateInSkyblock)
    }

    private var lootData = object : PersistentJsonClass<MutableMap</* date */String, MutableMap</* floor */String, MutableList<ChestData>>>>(
        "devonian/lootlogger.json",
        object : TypeToken<MutableMap</* date */String, MutableMap</* floor */String, MutableList<ChestData>>>>() {}
    ) {
        override fun onLoadDefault() {
            data = mutableMapOf()
        }
    }
    private val chestNames = mapOf(
        "Wood" to "&fWood Chest&r",
        "Gold" to "&6Gold Chest&r",
        "Diamond" to "&bDiamond Chest&r",
        "Emerald" to "&2Emerald Chest&r",
        "Obsidian" to "&5Obsidian Chest&r",
        "Bedrock" to "&8Bedrock Chest&r",
    )
    private val essenceRegex = "^(Wither|Undead) Essence x(\\d+)$".toRegex()
    private val croesusChestRegex = "^(Master )?Catacombs - Floor ([IV]+)$".toRegex()
    private val chestOpenRegex = "^ *(BEDROCK|OBSIDIAN|EMERALD|DIAMOND|GOLD|WOOD) CHEST REWARDS$".toRegex()
    private val localTime = LocalDateTime.now()
    private val currentDate = "${localTime.monthValue}/${localTime.dayOfMonth}/${localTime.year}"
    private var currentFloor: String? = null
    private var currentChest: ChestData? = null
    private var scan = false

    data class ChestItem(
        val itemId: String,
        val name: String, // formatted lore
        val amount: Int = 1,
        val essence: Boolean = false,
        val book: Boolean = false,
    ) {
        fun price(ignoreEssence: Boolean = false): Int {
            if (ignoreEssence && essence) return 0
            return SkyblockPrices.buyPrice(itemId).roundToInt() * amount
        }
    }

    data class ChestData(
        val name: String,
        val items: MutableList<ChestItem> = mutableListOf(),
        var chestPrice: Int = 0,
        var requiresKey: Boolean = false,
        @Transient
        var purchased: Boolean = false,
        var hasRerolled: Boolean = false,
    ) {
        fun totalProfit(ignoreEssence: Boolean = false): Int =
            items.sumOf { it.price(ignoreEssence) } - chestPrice
    }

    override fun initialize() {
        lootData.load()

        DevonianCommand.command.subcommand("lootlogger") { _, args ->
            val mode = args.getOrNull(0) as? String?
            val floor = args.getOrNull(1) as? String?
            val date = args.getOrNull(2) as? String?
            if (mode.isNullOrEmpty()) {
                ChatUtils.sendMessage("&cLootLogger not a valid mode was set", true)
                return@subcommand 0
            }
            if (floor.isNullOrEmpty()) {
                ChatUtils.sendMessage("&cLootLogger no valid floor set", true)
                return@subcommand 0
            }
            if (date.isNullOrEmpty()) {
                ChatUtils.sendMessage("&cLootLogger You did not set a valid date. here are the current ones&7: &7${lootData.data!!.keys.joinToString(", ")}", true)
                return@subcommand 0
            }
            val list = lootData.data!![date]?.get(floor)
            if (list.isNullOrEmpty()) {
                ChatUtils.sendMessage("&cLootLogger list for date \"$date\" and floor \"${floor}\" is empty", true)
                return@subcommand 0
            }
            if (mode == "COMPACTED") {
                ChatUtils.sendMessage("&bLootLogger &a$floor&b stats" +
                        " &b| &eSpent &6${StringUtils.shortenNumber(list.sumOf { it.chestPrice })}" +
                        " &b| &eProfit &6${StringUtils.shortenNumber(list.sumOf { it.totalProfit(true) })} &7(${StringUtils.shortenNumber(list.sumOf { it.totalProfit() })})",
                true)
                ChatUtils.sendMessage("&8NOTE: the profit section subtracts the chest price so it _should_ be pure profit")
                return@subcommand 1
            }
            if (mode != "DETAILED") {
                ChatUtils.sendMessage("&cLootLogger not a valid mode was set", true)
                return@subcommand 0
            }
            val chestCount = mutableMapOf<String, Int>()
            val itemCount = mutableMapOf<String, Int>()
            list.forEach {
                val count = chestCount.getOrPut(it.name) { 0 }
                chestCount[it.name] = count + 1
                it.items.forEach { item ->
                    val name =
                        if (item.essence) item.name.replace(" §r§8x(\\d+)".toRegex(), "")
                        else item.name
                    val saveCount = itemCount.getOrPut(name) { 0 }
                    itemCount[name] = saveCount + item.amount
                }
            }
            ChatUtils.sendMessage("&bLootLogger &a$floor&b stats", true)
            itemCount.entries.sortedBy { it.value }.forEach { (name, amount) ->
                ChatUtils.sendMessage("&8- ${name}&f: &6${StringUtils.addCommas(amount)}")
            }
            ChatUtils.sendMessage("&bChests Opened&f:")
            chestCount.entries.sortedBy { it.value }.forEach { (name, amount) ->
                ChatUtils.sendMessage("&8- ${chestNames[name]}&f: &6${StringUtils.addCommas(amount)}")
            }
            ChatUtils.sendMessage("&eTotal Spent&f: &6${StringUtils.shortenNumber(list.sumOf { it.chestPrice })}")
            ChatUtils.sendMessage("&eProfit&f: &6${StringUtils.shortenNumber(list.sumOf { it.totalProfit(true) })}")
            ChatUtils.sendMessage("&eTotal Profit&f: &6${StringUtils.shortenNumber(list.sumOf { it.totalProfit() })} &8(including essence)")
            ChatUtils.sendMessage("&8NOTE: the profit section subtracts the chest price so it _should_ be pure profit")
            1
        }
            .word("mode")
            .suggest("mode", *listOf(
                "COMPACTED",
                "DETAILED"
            ).toTypedArray())
            .word("floor")
            .suggest("floor", *listOf(
                "E", "F1", "F2", "F3", "F4", "F5", "F6", "F7",
                "M1", "M2", "M3", "M4", "M5", "M6", "M7"
            ).toTypedArray())
            .greedyString("date")
            .suggest("date", *listOf(
                "${localTime.monthValue}/${localTime.dayOfMonth}/${localTime.year}"
            ).toTypedArray())

        on<ServerContainerOpenEvent> { event ->
            croesusChestRegex.matchEntire(event.titleStr)?.let {
                val ( _, mm, floor ) = it.groupValues
                val num = StringUtils.parseRoman(floor)
                currentFloor = if (mm.isEmpty()) "F${num}" else "M${num}"
                return@on
            }
            if (!chestNames.containsKey(event.titleStr)) return@on
            currentChest = ChestData(event.titleStr)
            scan = true
        }

        on<ServerContainerCloseEvent> {
            scan = false
        }

        on<ClientContainerCloseEvent> {
            scan = false
        }

        on<ServerContainerSetSlotEvent> { event ->
            val data = currentChest ?: return@on
            if (!scan) return@on
            val slot = event.slot
            val itemStack = event.itemStack

            parseItem(itemStack, slot, data)
        }

        on<ChatEvent> { event ->
            // technically should never cause conc because this runs way too late
            val ( chestName ) = event.matches(chestOpenRegex) ?: return@on
            if (currentFloor == null && Dungeons.floor != FloorType.None)
                currentFloor = Dungeons.floor.shortName
            if (currentChest == null ||
                currentFloor == null ||
                currentChest!!.purchased ||
                currentChest!!.name.lowercase() != chestName.lowercase()
            ) {
                currentChest = null
                currentFloor = null
                scan = false
                return@on
            }

            lootData.data!!
                .getOrPut(currentDate) { mutableMapOf() }
                .getOrPut(currentFloor!!) { mutableListOf() }
                .add(currentChest!!)

            currentChest = null
            currentFloor = null
            scan = false
        }
    }

    private fun parseItem(itemStack: ItemStack, slot: Int, chestData: ChestData) {
        if (slot == 31) {
            if (itemStack.item != Items.CHEST) return
            val lore = ItemUtils.lore(itemStack) ?: return
            val costIdx = lore.indexOf("Cost")
            if (costIdx == -1) return
            var price = 0
            var requiresKey = false

            lore.getOrNull(costIdx + 1)?.replace("(,+| Coins)".toRegex(), "")?.let {
                price +=
                    if (it.isEmpty() || !"\\d+".toRegex().matches(it)) 0
                    else it.toInt()
            }
            lore.getOrNull(costIdx + 2)?.let {
                if (it != "Dungeon Chest Key") return@let
                price += SkyblockPrices.buyPrice("DUNGEON_CHEST_KEY").roundToInt()
                requiresKey = true
            }

            chestData.requiresKey = requiresKey
            chestData.chestPrice = price
            return
        }

        // kismet feather
        if (slot == 50) {
            val lore = ItemUtils.lore(itemStack)
            val hasKismet = lore?.last()?.contains("You already rerolled a chest!") ?: false
            chestData.hasRerolled = hasKismet
            scan = false
            return
        }
        // what could go wrong
        if (slot !in 9..18) return
        if (
            itemStack.item == Items.STAINED_GLASS_PANE.black ||
            itemStack.item == Items.STAINED_GLASS_PANE.gray ||
            itemStack.isEmpty
        ) return
        val customName = itemStack.customName ?: return
        val isEnchantedBook = customName.string == "Enchanted Book"
        val itemName =
            if (isEnchantedBook)
                ItemUtils.lore(itemStack, true)?.getOrNull(2) ?: return
            else
                customName.colorCodes()
        var sbId = ItemUtils.skyblockId(itemStack)
        var amount = 1
        if (sbId == null && itemName.contains(" Essence ")) {
            val match = essenceRegex.matchEntire(itemName.clearCodes())?.groupValues?.drop(1) ?: return
            sbId = "ESSENCE_${match[0].uppercase()}"
            amount = match[1].toInt()
        }
        if (sbId == null) {
            println("Devonian\$LootLogger could not find sbId for $itemName")
            return
        }

        chestData.items.add(ChestItem(
            sbId,
            itemName,
            amount,
            itemName.contains(" Essence "),
            isEnchantedBook,
        ))
    }
}