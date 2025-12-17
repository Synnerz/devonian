package com.github.synnerz.devonian.features.inventory

import com.github.synnerz.devonian.Devonian
import com.github.synnerz.devonian.api.ChatUtils
import com.github.synnerz.devonian.api.ItemUtils
import com.github.synnerz.devonian.api.dungeon.Dungeons
import com.github.synnerz.devonian.api.events.*
import com.github.synnerz.devonian.api.Location
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack

object PreventItem {
    val PREVENTED_SOUND = SoundEvents.NOTE_BLOCK_BASS
    val mc = Devonian.minecraft

    fun onCancel(msg: String, actors: List<String>) {
        if (actors.isEmpty()) return
        ChatUtils.sendMessage("$msg (${actors.joinToString(", ")})", withPrefix = true)
        mc.level?.playPlayerSound(
            PREVENTED_SOUND.value(),
            SoundSource.MASTER,
            1f, 0.5f,
        )
    }

    fun initialize() {
        EventBus.on<DropItemEvent> { event ->
            if (!event.willDropInDungeons && Location.area == "catacombs" && Dungeons.started.value) return@on

            val slot = event.slot
            if (slot != null && slot.container !== mc.player?.inventory) return@on

            val evn = SlotEvent(slot, event.itemStack, slot?.containerSlot ?: 999, true, event)
            if (!evn.post()) return@on

            event.cancel()
            onCancel("&cPrevented dropping item", evn.actors)
        }

        EventBus.on<PickupItemInventoryEvent> { event ->
            val slot = event.slot
            if (slot.container !== mc.player?.inventory) return@on

            val selling = isSellScreen(event.screen)
            val salvage = isSalvageScreen(event.screen)
            val evn = SlotEvent(slot, slot.item, slot.containerSlot, selling || salvage, event)
            if (!evn.post()) return@on

            event.cancel()
            onCancel("&cPrevented ${if (selling) "selling" else if (salvage) "salvaging" else "moving"} item", evn.actors)
        }

        EventBus.on<QuickMoveItemEvent> { event ->
            val slot = event.slot
            if (slot.container !== mc.player?.inventory) return@on

            val selling = isSellScreen(event.screen)
            val salvage = isSalvageScreen(event.screen)
            val evn = SlotEvent(slot, slot.item, slot.containerSlot, selling || salvage, event)
            if (!evn.post()) return@on

            event.cancel()
            onCancel("&cPrevented ${if (selling) "selling" else if (salvage) "salvaging" else "moving"} item", evn.actors)
        }

        EventBus.on<QuickCraftMoveEvent> { event ->
            val slot = event.slot
            if (slot.container !== mc.player?.inventory) return@on

            val selling = isSellScreen(event.screen)
            val salvage = isSalvageScreen(event.screen)
            val evn = SlotEvent(slot, slot.item, slot.containerSlot, selling || salvage, event)
            if (!evn.post()) return@on

            event.cancel()
            onCancel("&cPrevented ${if (selling) "selling" else if (salvage) "salvaging" else "placing"} item", evn.actors)
        }

        EventBus.on<SwapItemEvent> { event ->
            val slot1 = event.slot1
            val slot2 = event.slot2
            if (slot1.container !== mc.player?.inventory) return@on
            if (slot2.container !== mc.player?.inventory) return@on

            val evn1 = SlotEvent(slot1, slot1.item, slot1.containerSlot, false, event, slot2, false)
            val evn2 = SlotEvent(slot2, slot2.item, slot2.containerSlot, false, event, slot1, true)
            if (!evn1.post() && !evn2.post()) return@on

            event.cancel()
            onCancel("&cPrevented moving item", (evn1.actors.toSet() + evn2.actors).toList())
        }
    }

    fun isSellScreen(screen: AbstractContainerScreen<*>): Boolean {
        val hopper = screen.menu.items.getOrNull(49) ?: return false
        val lore = ItemUtils.lore(hopper, false) ?: return false
        val first = lore.getOrNull(0) ?: return false
        if (first == "Click items in your inventory to sell") return true
        val last = lore.getOrNull(lore.size - 1) ?: return false
        return last == "Click to buyback!"
    }

    fun isSalvageScreen(screen: AbstractContainerScreen<*>): Boolean {
        return screen.title.string == "Salvage Items"
    }

    class SlotEvent(
        val slot: Slot?,
        val item: ItemStack,
        val idx: Int,
        val losesItem: Boolean,
        val underlying: Event,
        val swapped: Slot? = null,
        val dupe: Boolean = false,
    ) : CancellableEvent() {
        val actors = mutableListOf<String>()

        fun cancel(name: String) {
            cancel()
            actors.add(name)
        }
    }
}
