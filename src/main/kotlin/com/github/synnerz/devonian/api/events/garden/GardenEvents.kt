package com.github.synnerz.devonian.api.events.garden

import com.github.synnerz.devonian.api.ItemUtils
import com.github.synnerz.devonian.api.Location
import com.github.synnerz.devonian.api.Scheduler
import com.github.synnerz.devonian.api.SkyblockPrices
import com.github.synnerz.devonian.api.events.ChatEvent
import com.github.synnerz.devonian.api.events.ClientContainerCloseEvent
import com.github.synnerz.devonian.api.events.Event
import com.github.synnerz.devonian.api.events.EventBus
import com.github.synnerz.devonian.api.events.ServerContainerCloseEvent
import com.github.synnerz.devonian.api.events.ServerContainerOpenEvent
import com.github.synnerz.devonian.api.events.ServerContainerSetSlotEvent
import com.github.synnerz.devonian.utils.StringUtils
import com.github.synnerz.devonian.utils.StringUtils.colorCodes
import net.minecraft.world.item.ItemStack
import kotlin.math.roundToInt

object GardenEvents {
    private const val OVERCLOCKER_3000 = "Overclocker 3000"
    private val pestDropRegex = "^You received (\\d+)x ([\\w ]+) for killing an? ([\\w ]+)!$".toRegex()
    private val pestRareDropRegex = "^RARE DROP! (?:(\\d+)x )?([\\w ]+) \\(\\+[\\d,]+☘\\)\$".toRegex()
    private val timesVisitedRegex = "^Times Visited: (\\d+)$".toRegex()
    private val offersAcceptedRegex = "^Offers Accepted: (\\d+)$".toRegex()
    private val farmingXPRegex = "^ \\+([\\d,.kMB]+) Farming XP$".toRegex()
    private val gardenXPRegex = "^ \\+([\\d,]+) Garden Experience$".toRegex()
    private val copperRegex = "^ \\+([\\d,]+) Copper$".toRegex()
    private val powderRegex = "^ \\+([\\d,]+) (?:Gemstone|Mithril) Powder$".toRegex()
    private val rareItemRegex = "^ ◆?([\\w' ]+)$".toRegex()
    // will not match if it's single or not enchanted form
    private val requiredItemRegex = "^ ([\\w ]+) x([\\d,.]+)$".toRegex()
    private val normalToInternals = mapOf(
        "MUTANT_NETHER_WART" to "MUTANT_NETHER_STALK",
        "ENCHANTED_NETHER_WART" to "ENCHANTED_NETHER_STALK",
        "ENCHANTED_RED_MUSHROOM_BLOCK" to "ENCHANTED_HUGE_MUSHROOM_2",
        "ENCHANTED_BROWN_MUSHROOM_BLOCK" to "ENCHANTED_HUGE_MUSHROOM_1",
        "ENCHANTED_MELON" to "ENCHANTED_MELON_BLOCK",
        "ENCHANTED_COCOA_BEANS" to "ENCHANTED_COCOA",
        "SPACE_HELMET" to "DCTR_SPACE_HELM",
        "DEDICATION_4" to "ENCHANTMENT_DEDICATION_4",
        "DEDICATION_IV" to "ENCHANTMENT_DEDICATION_4",
        "QUICKDRAW_CHIP" to "QUICKDRAW_GARDEN_CHIP",
        "HYPERCHARGE_CHIP" to "HYPERCHARGE_GARDEN_CHIP",
        "DEDICATION_4" to "ENCHANTMENT_DEDICATION_4",
        "DEDICATION_IV" to "ENCHANTMENT_DEDICATION_4",
    )
    private var lastGui: String? = null
    private var visitorData: VisitorData? = null

    data class VisitorComponent(val string: String, val format: String)
    data class VisitorCrop(val component: VisitorComponent, val amount: Int) {
        fun price(): Int
            = SkyblockPrices.buyPrice(component.string).roundToInt() * amount
    }
    data class VisitorData(
        val name: VisitorComponent,
        val timesVisited: Int = 0,
        val offersAccepted: Int = 0,
    )
    data class VisitorRareItem(
        val sbId: String,
        val lore: String,
    ) {
        fun price(): Int = SkyblockPrices.buyPrice(sbId).roundToInt()
    }
    data class VisitorItemData(
        val name: VisitorComponent,
        val rareItems: MutableList<VisitorRareItem> = mutableListOf(),
        val farmingXP: Int,
        val gardenXP: Int,
        val copper: Int,
        val requiredCrops: MutableList<VisitorCrop> = mutableListOf(),
    ) {
        fun profit(): Int {
            val totalPrice = copperPrice() + rareItemsPrice()

            return totalPrice - requiredPrice()
        }

        fun rareItemsPrice(): Int = rareItems.sumOf { it.price() }

        fun requiredPrice(): Int = requiredCrops.sumOf { it.price() }

        fun copperPrice(): Int
            = (SkyblockPrices.buyPrice("ENCHANTMENT_GREEN_THUMB_1") / 1500).roundToInt() * copper
    }

    class PestKill(val name: String) : Event
    class PestDrop(
        val name: String,
        val amount: Int,
        val isRare: Boolean = false,
    ) : Event
    class VisitorOpen(val data: VisitorData) : Event
    class VisitorClose(val data: VisitorData) : Event
    class VisitorItems(val data: VisitorItemData) : Event

    fun initialize() {
        EventBus.on<ChatEvent> { event ->
            event.matches(pestDropRegex)?.let {
                val ( num, cropType, pestType ) = it
                val amount = num.toIntOrNull() ?: 0
                if (cropType != OVERCLOCKER_3000) PestKill(pestType).post()

                PestDrop(cropType, amount).post()
            }

            val rareDropMatch = event.matches(pestRareDropRegex) ?: return@on
            val ( num, cropType ) = rareDropMatch
            val amount = num.toIntOrNull() ?: 1

            PestDrop(cropType, amount, true).post()
        }.setEnabled(Location.stateInArea("garden"))

        EventBus.on<ServerContainerOpenEvent> { event ->
            if (visitorData != null && lastGui != visitorData!!.name.string) {
                val _data = visitorData
                Scheduler.scheduleTask { VisitorClose(_data!!).post() }
                visitorData = null
            }
            lastGui = event.titleStr
        }.setEnabled(Location.stateInArea("garden"))

        EventBus.on<ServerContainerCloseEvent> {
            lastGui = null
            if (visitorData == null) return@on
            val _data = visitorData
            Scheduler.scheduleTask { VisitorClose(_data!!).post() }
            visitorData = null
        }.setEnabled(Location.stateInArea("garden"))
        EventBus.on<ClientContainerCloseEvent> {
            lastGui = null
            if (visitorData == null) return@on
            val _data = visitorData
            Scheduler.scheduleTask { VisitorClose(_data!!).post() }
            visitorData = null
        }.setEnabled(Location.stateInArea("garden"))

        EventBus.on<ServerContainerSetSlotEvent> { event ->
            if (lastGui == null) return@on
            val slot = event.slot
            val itemStack = event.itemStack
            if (slot == 29 && visitorData != null) {
                findVisitorItems(itemStack)
                return@on
            }
            if (slot != 13) return@on
            findVisitorData(itemStack)
        }.setEnabled(Location.stateInArea("garden"))
    }

    private fun findVisitorData(itemStack: ItemStack) {
        val customName = itemStack.customName ?: return
        val name = customName.string
        if (!name.equals(lastGui, ignoreCase = true)) return

        val lore = ItemUtils.lore(itemStack) ?: return
        var visits = 0
        var accepts = 0

        for (line in lore) {
            val timesVisitedMatch = timesVisitedRegex.matchEntire(line)?.groupValues
            if (timesVisitedMatch != null) {
                visits = timesVisitedMatch[1].toIntOrNull() ?: 0
                continue
            }
            val match = offersAcceptedRegex.matchEntire(line)?.groupValues ?: continue
            accepts = match[1].toIntOrNull() ?: 0
        }
        // not valid visitor, it should always have at least 1 visit
        if (visits <= 0 || accepts < 0) return

        val _visitorData = VisitorData(
            VisitorComponent(name, customName.colorCodes()),
            visits,
            accepts
        )
        visitorData = _visitorData
        Scheduler.scheduleTask {
            VisitorOpen(_visitorData).post()
        }
    }

    private fun findVisitorItems(itemStack: ItemStack) {
        if (visitorData == null) return
        val name = itemStack.customName?.string ?: return
        if (!name.equals("accept offer", ignoreCase = true)) return
        val lore = ItemUtils.lore(itemStack) ?: return
        val formattedLore = ItemUtils.lore(itemStack, true) ?: return
        val rareItems = mutableListOf<VisitorRareItem>()
        val required = mutableListOf<VisitorCrop>()
        var copper = 0
        var farmingXP = 0
        var gardenXP = 0
        var seenPowder = false

        for (idx in 0..lore.lastIndex) {
            val line = lore[idx]
            val requiredItemMatch = requiredItemRegex.matchEntire(line)?.groupValues
            if (requiredItemMatch != null && farmingXP == 0) {
                val itemId = requiredItemMatch[1].uppercase().trim().replace(" ", "_")
                val amount = requiredItemMatch[2].trim().replace("([,.]+)".toRegex(), "").toIntOrNull() ?: 1
                val comp = VisitorComponent(
                    if (itemId in normalToInternals) normalToInternals[itemId]!! else itemId,
                    formattedLore[idx].replace("(§\\wx[\\d,.]+)".toRegex(), "").trim(),
                )
                required.add(VisitorCrop(comp, amount))
                continue
            }

            val farmingXPMatch = farmingXPRegex.matchEntire(line)?.groupValues
            if (farmingXPMatch != null) {
                farmingXP = StringUtils.parseShortenedNumber(farmingXPMatch[1].uppercase())
                continue
            }

            val gardenXPMatch = gardenXPRegex.matchEntire(line)?.groupValues
            if (gardenXPMatch != null) {
                gardenXP = gardenXPMatch[1].toIntOrNull() ?: 0
                continue
            }

            val copperMatch = copperRegex.matchEntire(line)?.groupValues
            if (copperMatch != null) {
                copper = copperMatch[1].trim().replace(",", "").toIntOrNull() ?: 1
                continue
            }

            val powderMatch = powderRegex.matchEntire(line)?.groupValues
            if (powderMatch != null) {
                seenPowder = true
                continue
            }

            if (copper == 0 && !seenPowder) continue
            val rareItemMatch = rareItemRegex.matchEntire(line)?.groupValues ?: continue
            val sbId = rareItemMatch[1].uppercase().trim().replace(" ", "_")

            rareItems.add(VisitorRareItem(
                if (sbId in normalToInternals) normalToInternals[sbId]!! else sbId,
                formattedLore[idx]
            ))
        }

        Scheduler.scheduleTask {
            VisitorItems(VisitorItemData(
                visitorData!!.name,
                rareItems,
                farmingXP,
                gardenXP,
                copper,
                required,
            )).post()
        }
    }
}