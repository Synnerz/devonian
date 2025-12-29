package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.devonian.api.ItemUtils
import com.github.synnerz.devonian.api.Scheduler
import com.github.synnerz.devonian.api.ScreenUtils
import com.github.synnerz.devonian.api.events.*
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket
import net.minecraft.world.item.Items
import java.awt.Color
import java.util.concurrent.CopyOnWriteArraySet

object PartyFinderHighlight : Feature(
    "partyFinderHighlight",
    "Highlights a party finder party if you meet the requirements to join it",
    Categories.DUNGEONS,
    subcategory = "QOL"
) {
    private val classLevelRegex = "^ (Healer|Tank|Mage|Berserk|Archer) (\\d+)(?:: [\\d.,]+%)?$".toRegex()
    private val chatClassSwapRegex = "^You have selected the (Healer|Tank|Mage|Berserk|Archer) Dungeon Class!$".toRegex()
    private val loreClassRegex = "^ (\\w{1,16}): (Healer|Tank|Mage|Berserk|Archer) \\((\\d+)\\)$".toRegex()
    private val requirementLowCataRegex = "^Requires Catacombs Level \\d+!$".toRegex()
    private val requirementLowClassRegex = "^Requires a Class at Level \\d+!$".toRegex()
    private val requirementTooBadRegex = "^Complete previous floor first!$".toRegex()
    private val refreshCooldownRegex = "^Please wait a few seconds between refreshing!$".toRegex()
    private var isOnCd = false
    private val whitelist = CopyOnWriteArraySet<Int>()
    private val blacklist = CopyOnWriteArraySet<Int>()
    private var currentRole: String? = null
    private var inPF = false
    private var shouldScan = false

    override fun initialize() {
        on<TabUpdateEvent> { event ->
            event.matches(classLevelRegex)?.let {
                currentRole = it[0]
                return@on
            }
        }

        on<ChatEvent> { event ->
            event.matches(chatClassSwapRegex)?.let {
                currentRole = it[0]
                return@on
            }
            event.matches(refreshCooldownRegex)?.let {
                isOnCd = true
                return@on
            }
        }

        on<ServerContainerOpen> { event ->
            inPF = event.string() == "Party Finder"
            shouldScan = inPF
            if (!shouldScan && (whitelist.isNotEmpty() || blacklist.isNotEmpty())) {
                whitelist.clear()
                blacklist.clear()
            }
        }

        on<ServerContainerClose> {
            if (whitelist.isNotEmpty() || blacklist.isNotEmpty()) {
                whitelist.clear()
                blacklist.clear()
            }
        }

        on<ClientContainerClose> {
            if (whitelist.isNotEmpty() || blacklist.isNotEmpty()) {
                whitelist.clear()
                blacklist.clear()
            }
        }

        on<ServerContainerSetContent> { event ->
            if (!shouldScan) return@on

            event.forEach { idx, itemStack ->
                if (itemStack == null || itemStack.isEmpty || itemStack.item != Items.PLAYER_HEAD) return@forEach
                if (idx > 36) return@forEach

                val lore = ItemUtils.lore(itemStack) ?: return@forEach
                scanLore(lore, idx)
            }

            shouldScan = false
        }

        on<PacketReceivedEvent> { event ->
            val packet = event.packet
            if (packet !is ClientboundContainerSetSlotPacket) return@on
            if (!inPF || shouldScan) return@on
            val slot = packet.slot
            val itemStack = packet.item

            if (blacklist.contains(slot) || whitelist.contains(slot)) {
                blacklist.remove(slot)
                whitelist.remove(slot)
            }
            if (itemStack.isEmpty) return@on
            if (itemStack.item != Items.PLAYER_HEAD) return@on

            val lore = ItemUtils.lore(itemStack) ?: return@on
            scanLore(lore, slot)
        }

        on<GuiClickEvent> { event ->
            if (!inPF) return@on
            val slot = ScreenUtils.cursorSlot(event.screen) ?: return@on
            if (slot.containerSlot != 46 || slot.container == minecraft.player?.inventory) return@on

            Scheduler.scheduleServerTask(5) {
                if (isOnCd) return@scheduleServerTask
                blacklist.clear()
                whitelist.clear()
                isOnCd = false
            }
        }

        on<RenderSlotEvent> { event ->
            if (event.isInventory()) return@on
            val slot = event.slot

            if (blacklist.contains(slot.containerSlot)) {
                event.ctx.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, Color.RED.rgb)
                return@on
            }
            if (!whitelist.contains(slot.containerSlot)) return@on

            event.ctx.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, Color.GREEN.rgb)
        }.prio = 30
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        blacklist.clear()
        whitelist.clear()
        isOnCd = false
    }

    private fun scanLore(lore: List<String>, idx: Int) {
        val rolesIn = mutableListOf<String>()
        var canJoin = true

        for (line in lore) {
            if (
                requirementLowCataRegex.matches(line) ||
                requirementLowClassRegex.matches(line) ||
                requirementTooBadRegex.matches(line)
            ) {
                canJoin = false
                break
            }

            val match = loreClassRegex.matchEntire(line)?.groupValues?.drop(1) ?: continue
            rolesIn.add(match[1])
        }

        if (rolesIn.contains(currentRole)) canJoin = false

        if (canJoin)
            whitelist.add(idx)
        else
            blacklist.add(idx)
    }
}
