package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.devonian.api.ItemUtils
import com.github.synnerz.devonian.api.events.PacketReceivedEvent
import com.github.synnerz.devonian.api.events.PacketSentEvent
import com.github.synnerz.devonian.api.events.RenderSlotEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import net.minecraft.network.protocol.game.ClientboundContainerClosePacket
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import java.awt.Color
import java.util.concurrent.CopyOnWriteArraySet

object CroesusHighlightUnopened : Feature(
    "croesusHighlightUnopened",
    "Highlights the chests in croesus that are not yet opened",
    Categories.DUNGEONS,
    "Dungeon Hub",
    subcategory = "QOL"
) {
    private val SETTING_HIDE_BLACKLISTED = addSwitch(
        "hideBlacklisted",
        false,
        "Hides the croesus chests that have been opened",
        "Croesus Highlight Blacklist"
    )
    private var inCroesus = false
    private val whitelist = CopyOnWriteArraySet<Int>()
    private val blacklist = CopyOnWriteArraySet<Int>()

    override fun initialize() {
        on<PacketReceivedEvent> { event ->
            val packet = event.packet

            when (packet) {
                is ClientboundOpenScreenPacket -> {
                    val title = packet.title.string
                    inCroesus = title == "Croesus"
                    whitelist.clear()
                    blacklist.clear()
                    return@on
                }

                is ClientboundContainerClosePacket -> {
                    inCroesus = false
                    blacklist.clear()
                    whitelist.clear()
                    return@on
                }

                is ClientboundContainerSetContentPacket -> {
                    if (!inCroesus) return@on
                    val items = packet.items
                    for (idx in 0..<45) {
                        updateSlotHighlight(idx, items.getOrNull(idx))
                    }
                }

                is ClientboundContainerSetSlotPacket -> {
                    if (!inCroesus) return@on
                    val slot = packet.slot
                    if (slot !in 0..<45) return@on
                    updateSlotHighlight(slot, packet.item)
                }
            }
        }

        on<PacketSentEvent> { event ->
            val packet = event.packet
            if (packet !is ServerboundContainerClosePacket) return@on

            inCroesus = false
            blacklist.clear()
            whitelist.clear()
        }

        on<RenderSlotEvent> { event ->
            val slot = event.slot
            if (slot.container == minecraft.player?.inventory) return@on
            val title = event.screen.title.string
            if (title != "Croesus") return@on
            if (SETTING_HIDE_BLACKLISTED.get() && blacklist.contains(slot.containerSlot)) {
                event.cancel()
                return@on
            }
            if (!whitelist.contains(slot.containerSlot)) return@on

            event.ctx.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, Color.GREEN.rgb)
        }
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        inCroesus = false
        blacklist.clear()
        whitelist.clear()
    }

    private fun updateSlotHighlight(slotIndex: Int, itemStack: ItemStack?) {
        whitelist.remove(slotIndex)
        blacklist.remove(slotIndex)

        if (itemStack == null || itemStack == ItemStack.EMPTY) return
        if (itemStack.item != Items.PLAYER_HEAD) return
        val lore = ItemUtils.lore(itemStack) ?: return

        if (lore.any { it.contains("Opened Chest: ") }) {
            blacklist.add(slotIndex)
            return
        }
        if (lore.any { it == "No chests opened yet!" }) {
            whitelist.add(slotIndex)
        }
    }
}
