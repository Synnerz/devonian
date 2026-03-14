package com.github.synnerz.devonian.api.dungeon

import com.github.synnerz.devonian.api.ItemUtils
import com.github.synnerz.devonian.api.Location
import com.github.synnerz.devonian.api.Scheduler
import com.github.synnerz.devonian.api.SkyblockPrices
import com.github.synnerz.devonian.api.events.ClientContainerCloseEvent
import com.github.synnerz.devonian.api.events.Event
import com.github.synnerz.devonian.api.events.EventBus
import com.github.synnerz.devonian.api.events.ServerContainerCloseEvent
import com.github.synnerz.devonian.api.events.ServerContainerOpenEvent
import com.github.synnerz.devonian.api.events.ServerContainerSetSlotEvent
import com.github.synnerz.devonian.utils.StringUtils
import net.minecraft.world.item.Items
import kotlin.math.roundToInt

object CroesusListener {
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
    private val inChestRegex = "^(?:Master )?Catacombs - Floor [IV]+$".toRegex()
    private val enchantedBookRegex = "^Enchanted Book \\(([\\w ]+) ([IV]+)\\)$".toRegex()
    private val essenceRegex = "^(Wither|Undead) Essence x(\\d+)$".toRegex()
    private val costRegex = "^(\\d[\\d,]+) Coins$".toRegex()
    private var inChest = false
    private var currentChest: String? = null
    private var inCroesus = false
    private var croesusPage: Int = 0

    data class ChestItem(
        val itemId: String,
        val name: String, // formatted lore
        val pricePer: Int = 0,
        val amount: Int = 1,
        val essence: Boolean = false,
        val book: Boolean = false,
    ) {
        fun price(ignoreEssence: Boolean = false): Int {
            if (ignoreEssence && essence) return 0
            return pricePer * amount
        }
    }

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
            else items.sumOf { it.price(ignoreEssence) } - price
    }

    data class CroesusChest(
        val hasOpened: Boolean = false,
        val hasNoChest: Boolean = false,
        val canOpen: Boolean = false,
        val slot: Int,
        // page would go here, but it does not exist in the same tick
    )

    class OpenedCroesus : Event
    class CroesusChestSet(val data: CroesusChest) : Event
    // This one is definitely changing soon
    class CroesusPageSwitch(val page: Int) : Event
    class ClosedCroesus : Event
    class OpenedChest(val chestName: String) : Event
    class ItemChestSet(val data: ChestData) : Event
    class ClosedChest : Event

    fun initialize() {
        EventBus.on<ServerContainerOpenEvent> { event ->
            val croesus = event.titleStr == "Croesus"
            /* possibly reset croesus data? */
            if (!croesus && inCroesus) Scheduler.scheduleTask { ClosedCroesus().post() }
            else if (croesus && !inCroesus) Scheduler.scheduleTask { OpenedCroesus().post() }
            inCroesus = croesus

            val matches = event.titleStr.matches(inChestRegex)
            if (!matches && inChest) {
                Scheduler.scheduleTask {
                    ClosedChest().post()
                    reset()
                }
                return@on
            }
            if (!inChest && matches) Scheduler.scheduleTask { OpenedChest(event.titleStr) }
            inChest = matches
            currentChest = event.titleStr
        }.setEnabled(Location.stateInArea("dungeon hub"))

        EventBus.on<ServerContainerCloseEvent> {
            if (inCroesus || inChest) {
                Scheduler.scheduleTask {
                    if (inCroesus) {
                        ClosedCroesus().post()
                        inCroesus = false
                        croesusPage = 0
                        return@scheduleTask
                    }
                    ClosedChest().post()
                    reset()
                }
            }
        }.setEnabled(Location.stateInArea("dungeon hub"))

        EventBus.on<ClientContainerCloseEvent> {
            if (inCroesus || inChest) {
                if (inCroesus) {
                    ClosedCroesus().post()
                    inCroesus = false
                    croesusPage = 0
                    return@on
                }
                ClosedChest().post()
                reset()
            }
        }.setEnabled(Location.stateInArea("dungeon hub"))

        EventBus.on<ServerContainerSetSlotEvent> { event ->
            if (inCroesus) onCroesus(event)
            else if (inChest && currentChest != null) onCroesusChest(event)
        }.setEnabled(Location.stateInArea("dungeon hub"))
    }

    fun reset() {
        for (data in chestsData) {
            val v = data.value
            v.items.clear()
            v.price = 0
            v.requiresKey = false
            v.slot = -1
            v.purchased = false
        }
        inChest = false
        currentChest = null
    }

    fun onCroesus(event: ServerContainerSetSlotEvent) {
        // TODO: find the correct page
        val slot = event.slot
        if (slot == 0) {
            // Should mean page was changed
            CroesusPageSwitch(croesusPage + 1).post()
            return
        }
        if (event.slot > 45) return
        val itemStack = event.itemStack
        if (itemStack.item != Items.PLAYER_HEAD) return
        val lore = ItemUtils.lore(itemStack) ?: return

        var hasOpened = false
        var hasNoChest = false
        var canOpen = false

        for (line in lore) {
            if (line.contains("Opened Chest: "))
                hasOpened = true
            else if (line.contains("No more chests to open!"))
                hasNoChest = true
            else if (line == "No chests opened yet!")
                canOpen = true
        }

        Scheduler.scheduleTask { CroesusChestSet(CroesusChest(hasOpened, hasNoChest, canOpen, slot)).post() }
    }

    fun onCroesusChest(event: ServerContainerSetSlotEvent) {
        val slot = event.slot
        if (slot !in 9..18) return
        val itemStack = event.itemStack
        if (itemStack.item != Items.PLAYER_HEAD) return

        val chestName = itemStack.customName?.string ?: return
        val lore = ItemUtils.lore(itemStack) ?: return
        val formattedLore = ItemUtils.lore(itemStack, true) ?: return

        val data = chestsData[chestName] ?: return
        data.slot = slot
        val items = mutableListOf<ChestItem>()

        for (idx in 0..lore.lastIndex) {
            val line = lore.getOrNull(idx) ?: continue
            if (line == "Contents" || line.isBlank()) continue
            if (line == "Already opened!") {
                data.purchased = true
                break
            }
            // TODO: find reroll here? maybe inside chest?

            if (line == "Cost") {
                val chestPriceLore = lore[idx + 1]
                val possibleKey = lore[idx + 2]
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

                data.price = price
                break
            }

            val loreName = formattedLore[idx]
            val enchantMatch = enchantedBookRegex.matchEntire(line)?.groupValues?.drop(1)
            if (enchantMatch != null) {
                val name = enchantMatch[0]
                val numeral = enchantMatch[1]
                val tier = StringUtils.parseRoman(numeral)
                val cleanName = name.replace(" ", "_").uppercase()
                var price = SkyblockPrices.buyPrice("ENCHANTMENT_${cleanName}_$tier").roundToInt()
                val itemId = if (price == 0) "ENCHANTMENT_ULTIMATE_${cleanName}_$tier" else "ENCHANTMENT_${cleanName}_$tier"
                if (price == 0)
                    price = SkyblockPrices.buyPrice("ENCHANTMENT_ULTIMATE_${cleanName}_$tier").roundToInt()

                items.add(ChestItem(itemId, loreName, price, book = true))
                continue
            }

            val essenceMatch = essenceRegex.matchEntire(line)?.groupValues?.drop(1)
            if (essenceMatch != null) {
                val type = essenceMatch[0].uppercase()
                val amount = essenceMatch[1].toIntOrNull() ?: continue
                val price = SkyblockPrices.buyPrice("ESSENCE_$type")

                items.add(ChestItem("ESSENCE_$type", loreName, price.roundToInt(), amount, true))
                continue
            }

            var itemId = line
                .uppercase()
                .replace("- ", "")
                .replace("'", "")
                .replace(" ", "_")
            if (itemId in specialIds) itemId = specialIds[itemId]!!

            val price = SkyblockPrices.buyPrice(itemId).roundToInt()

            if (price == 0) {
                println("Devonian\$CroesusListener[status=Item Not Found, name=\"$itemId\", line=\"$line\"]")
                continue
            }

            items.add(ChestItem(itemId, loreName, price))
        }

        Scheduler.scheduleTask {
            data.items.addAll(items)
            ItemChestSet(data).post()
        }
    }
}