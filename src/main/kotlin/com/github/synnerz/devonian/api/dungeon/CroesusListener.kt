package com.github.synnerz.devonian.api.dungeon

import com.github.synnerz.devonian.api.*
import com.github.synnerz.devonian.api.events.*
import com.github.synnerz.devonian.api.kuudra.KuudraTier
import com.github.synnerz.devonian.commands.DevonianCommand
import com.github.synnerz.devonian.utils.BasicState
import com.github.synnerz.devonian.utils.PersistentJsonClass
import com.github.synnerz.devonian.utils.StringUtils
import com.google.gson.reflect.TypeToken
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Style
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import kotlin.math.min
import kotlin.math.roundToInt

object CroesusListener {
    private val specialIds = mapOf(
        // big thank Unclaimed NOOB Six
        "NECROMANCERS_BROOCH" to "NECROMANCER_BROOCH",
        "WITHER_SHIELD" to "WITHER_SHIELD_SCROLL",
        "IMPLOSION" to "IMPLOSION_SCROLL",
        "SHADOW_WARP" to "SHADOW_WARP_SCROLL",
        "WARPED_STONE" to "AOTE_STONE",
        "SPIRIT_STONE" to "SPIRIT_DECOY",
    )
    private val inDungeonChestRegex = "^(Master )?Catacombs - Floor ([IV]+)$".toRegex()
    private val inKuudraChestRegex = "^Kuudra - (\\w+)$".toRegex()
    private val enchantedBookRegex = "^Enchanted Book \\(([\\w ]+) ([IV]+)\\)$".toRegex()
    private val essenceRegex = "^(\\w+) Essence x(\\d+)$".toRegex()
    private val costRegex = "^(\\d[\\d,]+) Coins$".toRegex()
    private val croesusTitleRegex = "^(?:\\((\\d+)/\\d+\\) )?Croesus$".toRegex()

    var inCroesus = false
        private set
    var inRunChests = false
        private set
    var inChest = false
        private set

    val stateInCroesus = BasicState(false)
    val stateInRunChests = BasicState(false)
    val stateInChest = BasicState(false)

    var currentPage: Int = 0
        private set
    var currentDungeonRun: FloorType? = null
        private set
    var currentKuudraRun: KuudraTier? = null
        private set
    var currentChest: ChestType? = null
        private set

    private var cumRuns = mutableListOf<CroesusRun>()
    private var cumChests = mutableListOf<RunChest>()
    // private var cumItems = mutableListOf<ChestItem>()

    private var lastViewedChestContents: ChestContents? = null
    private var blacklist = emptySet<String>()

    fun inBlacklist(itemId: String): Boolean = blacklist.contains(itemId)

    private val blacklistedItems = object : PersistentJsonClass<MutableSet<String>>(
        "devonian/croesusblacklist.json",
        object : TypeToken<MutableSet<String>>() {}
    ) {
        override fun onLoadDefault() {
            data = mutableSetOf()
            blacklist = emptySet()
        }
    }

    private fun reset() {
        if (inCroesus) ClosedCroesusMenu().post()
        if (inRunChests) {
            if (currentDungeonRun != null) ClosedDungeonRunMenu().post()
            if (currentKuudraRun != null) ClosedKuudraRunMenu().post()
            ClosedRunMenu().post()
        }
        if (inChest) ClosedChestMenu().post()

        inCroesus = false
        inRunChests = false
        inChest = false
        stateInCroesus.value = false
        stateInRunChests.value = false
        stateInChest.value = false

        currentPage = 0
        currentDungeonRun = null
        currentKuudraRun = null
        currentChest = null
    }

    @Threaded class OpenedCroesusMenu(val page: Int) : Event
    @Threaded class OpenedDungeonRunMenu(val floor: FloorType) : Event
    @Threaded class OpenedKuudraRunMenu(val tier: KuudraTier) : Event
    @Threaded class OpenedRunMenu(val floor: FloorType?, val tier: KuudraTier?) : Event
    @Threaded class OpenedChestMenu(val type: ChestType) : Event

    @Threaded class ClosedCroesusMenu : Event
    @Threaded class ClosedDungeonRunMenu : Event
    @Threaded class ClosedKuudraRunMenu : Event
    @Threaded class ClosedRunMenu : Event
    @Threaded class ClosedChestMenu : Event

    @Threaded class ScannedCroesusSlot(val run: CroesusRun) : Event
    @Threaded class ScannedRunChest(val chest: RunChest) : Event
    // currently this scanner only cares about the "Open Reward Chest" item
    // as all the chest contents can be scanned from that
    // no other items in that menu are scanned
    // @Threaded class ScannedChestItem(val item: ChestItem) : Event

    @Threaded class LoadedCroesusMenu(val runs: List<CroesusRun>) : Event
    @Threaded class LoadedRunChests(val chests: List<RunChest>) : Event
    // @Threaded class LoadedChestItems(val items: List<ChestItem>) : Event
    @Threaded class LoadedChestItems(val chest: ChestContents) : Event

    fun initialize() {
        blacklistedItems.onAfterLoad {
            blacklist = blacklistedItems.data!!.toSet()
        }
        blacklistedItems.load()

        DevonianCommand.command.subcommand("croesusblacklist", true) { _, args ->
            if (args.isEmpty()) {
                val chest = lastViewedChestContents
                if (chest == null) ChatUtils.sendMessage("&cOpen a croesus chest to first!")
                else {
                    ChatUtils.sendMessage("&bLast viewed croesus chest", true)
                    chest.items.forEach {
                        val id = it.itemId
                        val inList = blacklist.contains(id)
                        ChatUtils.sendMessage(
                            ChatUtils.literal(
                                "&8- ${if (inList) "&c" else "&e"}$id"
                            ).setStyle(
                                Style.EMPTY.withClickEvent(ClickEvent.RunCommand("devonian croesusblacklist $id"))
                            )
                        )
                    }
                    ChatUtils.sendMessage("&7Click an item name to blacklist add/remove it")
                }
                return@subcommand 1
            }
            val itemId = args.firstOrNull() as? String? ?: return@subcommand 0
            val added = blacklist.contains(itemId)
            val msg = if (!added) "&aAdded" else "&cRemove"

            if (!added)
                blacklistedItems.data!!.add(itemId)
            else
                blacklistedItems.data!!.remove(itemId)
            blacklist = blacklistedItems.data!!.toSet()
            ChatUtils.sendMessage("&bCroesusBlacklist $msg &6$itemId", true)
            1
        }
            .word("ItemId")

        val stateEnabled = Location.stateInArea("dungeon hub", "catacombs")

        EventBus.on<ServerContainerOpenEvent> { event ->
            reset()
            cumRuns = mutableListOf()
            cumChests = mutableListOf()
            // cumItems = mutableListOf()

            croesusTitleRegex.matchEntire(event.titleStr)?.let {
                inCroesus = true
                stateInCroesus.value = true

                val page = it.groupValues[1].let { if (it.isEmpty()) 1 else it.toInt() }
                currentPage = page

                OpenedCroesusMenu(page).post()
                return@on
            }

            inDungeonChestRegex.matchEntire(event.titleStr)?.let {
                inRunChests = true
                stateInRunChests.value = true

                val isMM = it.groupValues[1].isNotEmpty()
                val floorNum = StringUtils.parseRoman(it.groupValues[2])

                val floor = FloorType.from(floorNum, isMM)
                currentDungeonRun = floor

                OpenedDungeonRunMenu(floor).post()
                OpenedRunMenu(floor, null).post()
                return@on
            }

            inKuudraChestRegex.matchEntire(event.titleStr)?.let {
                inRunChests = true
                stateInRunChests.value = true

                val tierName = it.groupValues[1]

                val tier = KuudraTier.from(tierName)
                currentKuudraRun = tier

                OpenedKuudraRunMenu(tier).post()
                OpenedRunMenu(null, tier).post()
            }

            ChestType.from(event.titleStr)?.let {
                inChest = true
                stateInChest.value = true

                currentChest = it
                lastViewedChestContents = null

                OpenedChestMenu(it).post()
                return@on
            }
        }.setEnabled(stateEnabled)

        EventBus.on<ServerContainerCloseEvent> {
            reset()
        }.setEnabled(stateEnabled)

        EventBus.on<ClientContainerCloseEvent> {
            reset()
        }.setEnabled(stateEnabled)

        EventBus.on<WorldChangeEvent> {
            reset()
        }.setEnabled(stateEnabled)

        EventBus.on<ServerContainerSetSlotEvent> { event ->
            when {
                inCroesus -> {
                    if (event.slot == 44) LoadedCroesusMenu(cumRuns).post()

                    val y = event.slot / 9
                    val x = event.slot % 9
                    if (y !in 1 .. 4) return@on
                    if (x !in 1 .. 7) return@on

                    val itemStack = event.itemStack
                    if (itemStack.item != Items.PLAYER_HEAD) return@on

                    val lore = ItemUtils.lore(itemStack) ?: return@on
                    val formattedLore = ItemUtils.lore(itemStack, true) ?: return@on

                    var hasOpened = false
                    var canOpen = true
                    var canKismet = true
                    // var canDKey = true
                    // var canWOF = true

                    for ((idx, line) in lore.withIndex()) {
                        when {
                            line == "No chests opened yet!" -> canOpen = true
                            line == "No more chests to open!" -> canOpen = false
                            line.startsWith("Opened Chest: ") -> hasOpened = true
                            line == " Kismet Feather" -> canKismet = !formattedLore[idx].contains("§m")
                            // line == " Dungeon Chest Key" -> canDKey = !formattedLore[idx].contains("§m")
                            // line == " Wheel of Fate" -> canWOF = !formattedLore[idx].contains("§m")
                        }
                    }

                    val state = ChestActionable(canOpen, hasOpened, canKismet)
                    val run = CroesusRun(
                        currentPage, event.slot,
                        currentDungeonRun, currentKuudraRun,
                        state,
                    )

                    ScannedCroesusSlot(run).post()

                    cumRuns.add(run)
                }

                inRunChests -> {
                    if (event.slot == 18) LoadedRunChests(cumChests).post()
                    if (event.slot !in 9..17) return@on

                    val itemStack = event.itemStack
                    if (itemStack.item != Items.PLAYER_HEAD) return@on

                    val chestName = itemStack.customName?.string ?: return@on
                    val chestType = ChestType.from(chestName) ?: return@on

                    val contents = parseContents(itemStack, chestType) ?: return@on
                    val chest = RunChest(contents, event.slot)

                    ScannedRunChest(chest).post()

                    cumChests.add(chest)
                }

                inChest -> {
                    val itemStack = event.itemStack
                    if (itemStack.item != Items.CHEST) return@on

                    val type = currentChest ?: return@on
                    val contents = parseContents(itemStack, type) ?: return@on

                    LoadedChestItems(contents).post()
                }
            }
        }.setEnabled(stateEnabled)
    }

    private fun parseContents(item: ItemStack, type: ChestType): ChestContents? {
        val lore = ItemUtils.lore(item) ?: return null
        val formatLore = ItemUtils.lore(item, true) ?: return null

        var bought = false
        var requiresKey = false
        var chestPrice = 0
        val items = mutableListOf<ChestItem>()

        for (idx in lore.indices) {
            when (val line = lore.getOrNull(idx) ?: continue) {
                "Contents",
                "",
                    -> continue

                "Already opened!" -> {
                    bought = true
                    // chest has already been opened do something
                    break
                }

                "Cost" -> {
                    val chestPriceLore = lore[idx + 1]
                    val possibleDKey = lore[idx + 2]

                    var price = 0
                    if (chestPriceLore != "FREE") {
                        val kuudraKey = KuudraKey.from(chestPriceLore)
                        price = kuudraKey?.calcPrice()
                            ?: costRegex.matchEntire(chestPriceLore)
                                ?.groupValues
                                ?.getOrNull(1)
                                ?.replace(",", "")
                                ?.toIntOrNull()
                            ?: 0

                        if (possibleDKey == "Dungeon Chest Key" || chestPriceLore == "Dungeon Chest Key") {
                            price += SkyblockPrices.buyPrice("DUNGEON_CHEST_KEY").roundToInt()
                            requiresKey = true
                        }
                    }

                    chestPrice = price

                    break
                }

                else -> {
                    var itemId: String? = null
                    var book = false
                    var essence = false
                    var count = 1

                    val enchantMatch = enchantedBookRegex.matchEntire(line)?.groupValues?.drop(1)
                    if (enchantMatch != null) {
                        val name = enchantMatch[0]
                        val numeral = enchantMatch[1]
                        val tier = StringUtils.parseRoman(numeral)
                        val cleanName = name.replace(" ", "_").uppercase()
                        itemId = "ENCHANTMENT_${cleanName}_$tier"
                        if (SkyblockPrices.buyPrice(itemId).roundToInt() == 0)
                            itemId = "ENCHANTMENT_ULTIMATE_${cleanName}_$tier"
                        book = true
                    }

                    val essenceMatch = essenceRegex.matchEntire(line)?.groupValues?.drop(1)
                    if (essenceMatch != null) {
                        val type = essenceMatch[0].uppercase()
                        count = essenceMatch[1].toIntOrNull() ?: continue
                        itemId = "ESSENCE_$type"
                        essence = true
                    }

                    if (itemId == null) {
                        itemId = line
                            .uppercase()
                            .replace("- ", "")
                            .replace("'", "")
                            .replace(" ", "_")

                        if (itemId.endsWith("SHARD")) itemId = "SHARD_${itemId.dropLast("_SHARD".length)}"
                        itemId = specialIds[itemId] ?: itemId
                    }

                    val price = SkyblockPrices.buyPrice(itemId).roundToInt()

                    // TODO: probably make this a toggle
                    if (price == 0) {
                        // FIXME: Devonian$CroesusProfit[status=Item Not Found, name="NECROMANCERS_BROOCH", line="Necromancer's Brooch"]
                        //  fix whenever not feeling lazy since this requires to have
                        //  a failsafe check because most items with ' in them is just remove but this one removes
                        //  the "s" as well
                        println("Devonian\$CroesusListener[status=Item Not Found, name=\"$itemId\", line=\"$line\"]")
                        continue
                    }

                    val item = ChestItem(
                        itemId,
                        formatLore[idx],
                        price,
                        count,
                        essence = essence,
                        book = book,
                        blacklisted = inBlacklist(itemId),
                    )
                    items.add(item)
                }
            }
        }

        val state = ChestOpenState(!bought, requiresKey)
        return ChestContents(type, items, chestPrice, state)
    }

    enum class ChestType(val displayName: String, val itemName: String) {
        Wood("&fWood", "Wood"),
        Gold("&6Gold", "Gold"),
        Diamond("&bDiamond", "Diamond"),
        Emerald("&2Emerald", "Emerald"),
        Obsidian("&5Obsidian", "Obsidian"),
        Bedrock("&8Bedrock", "Bedrock"),

        Free("&fFree", "Free Chest"),
        Paid("&6Paid", "Paid Chest");

        companion object {
            fun from(name: String) = entries.find { name == it.itemName }
        }
    }

    open class ChestOpenState(val canOpen: Boolean, val requiresKey: Boolean)
    class ChestActionable(canOpen: Boolean, requiresKey: Boolean, val canKismet: Boolean) : ChestOpenState(canOpen, requiresKey)

    data class ChestItem(
        val itemId: String,
        val displayName: String,
        val pricePer: Int,
        val amount: Int = 1,
        val essence: Boolean = false,
        val book: Boolean = false,
        val blacklisted: Boolean = false,
    ) {
        fun valuation(ignoreEssence: Boolean = false): Int {
            if (blacklisted) return 0
            if (ignoreEssence && essence) return 0
            return pricePer * amount
        }
    }

    data class ChestContents(
        val type: ChestType,
        val items: List<ChestItem>,
        val openCost: Int,
        val state: ChestOpenState,
    ) {
        fun totalProfit(ignoreEssence: Boolean = false): Int =
            if (state.canOpen) items.sumOf { it.valuation(ignoreEssence) } - openCost
            else Int.MIN_VALUE
    }

    data class RunChest(
        val chest: ChestContents,
        val slot: Int,
    )

    data class CroesusRun(
        val page: Int,
        val slot: Int,
        val type: FloorType?,
        val tier: KuudraTier?,
        val state: ChestActionable,
    )

    private enum class KuudraKey(
        val itemName: String,
        val enchSandMyc: Int,
        val coins: Int,
        val stars: Int = 2,
    ) {
        Basic("Kuudra Key", 2, 200_000),
        Hot("Hot Kuudra Key", 6, 400_000),
        Burning("Burning Kuudra Key", 20, 750_000),
        Fiery("Fiery Kuudra Key", 60, 1_500_000),
        Infernal("Infernal Kuudra Key", 120, 3_000_000);

        fun calcPrice(): Int {
            // TODO: setting or way to detect faction
            val enchPrice = min(
                SkyblockPrices.buyPrice("ENCHANTED_MYCELIUM"),
                SkyblockPrices.buyPrice("ENCHANTED_RED_SAND"),
            )
            val starPrice = SkyblockPrices.buyPrice("CORRUPTED_NETHER_STAR")
            return (enchSandMyc * enchPrice + stars * starPrice + coins).roundToInt()
        }

        companion object {
            fun from(name: String) = entries.find { name == it.itemName }
        }
    }

    @Deprecated("unused")
    data class ChestData(
        val name: String,
        val items: MutableList<ChestItem> = mutableListOf(),
        var price: Int = 0,
        var slot: Int = -1,
        var requiresKey: Boolean = false,
        var purchased: Boolean = false,
        var hasRerolled: Boolean = false,
    ) {
        fun totalProfit(ignoreEssence: Boolean = false): Int =
            if (purchased) Int.MIN_VALUE
            else items.sumOf { it.valuation(ignoreEssence) } - price
    }
}