package com.github.synnerz.devonian.features.inventory

import com.github.synnerz.devonian.Devonian
import com.github.synnerz.devonian.api.ChatUtils
import com.github.synnerz.devonian.api.dungeon.Dungeons
import com.github.synnerz.devonian.api.events.CancellableEvent
import com.github.synnerz.devonian.api.events.DropItemEvent
import com.github.synnerz.devonian.api.events.EventBus
import com.github.synnerz.devonian.api.events.PickupItemInventoryEvent
import com.github.synnerz.devonian.api.events.QuickCraftMoveEvent
import com.github.synnerz.devonian.api.events.QuickMoveItemEvent
import com.github.synnerz.devonian.api.events.SwapItemEvent
import com.github.synnerz.devonian.utils.Location
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack

object PreventItem {
    val PREVENTED_SOUND = SoundEvents.NOTE_BLOCK_BASS
    val mc = Devonian.minecraft

    fun onCancel(msg: String) {
        ChatUtils.sendMessage(msg, withPrefix = true)
        mc.level?.playPlayerSound(
            PREVENTED_SOUND.value(),
            SoundSource.MASTER,
            1f, 0.5f,
        )
    }

    fun initialize() {
        EventBus.on<DropItemEvent> { event ->
            if (Location.area == "catacombs" && Dungeons.timeElapsed.value != 0) return@on

            val slot = event.slot ?: return@on

            val evn = SlotEvent(slot, slot.item, slot.containerSlot, true)
            if (!evn.post()) return@on

            event.cancel()
            onCancel("&cPrevented dropping item (${evn.actors.joinToString(", ")})")
        }

        EventBus.on<PickupItemInventoryEvent> { event ->
            val slot = event.slot

            val evn = SlotEvent(slot, slot.item, slot.containerSlot, false)
            if (!evn.post()) return@on

            event.cancel()
            onCancel("&cPrevented moving item (${evn.actors.joinToString(", ")})")
        }

        EventBus.on<QuickMoveItemEvent> { event ->
            val slot = event.slot

            val evn = SlotEvent(slot, slot.item, slot.containerSlot, false)
            if (!evn.post()) return@on

            event.cancel()
            onCancel("&cPrevented moving item (${evn.actors.joinToString(", ")})")
        }

        EventBus.on<QuickCraftMoveEvent> { event ->
            val slot = event.slot

            val evn = SlotEvent(slot, slot.item, slot.containerSlot, false)
            if (!evn.post()) return@on

            event.cancel()
            onCancel("&cPrevented placing item (${evn.actors.joinToString(", ")})")
        }

        EventBus.on<SwapItemEvent> { event ->
            val slot1 = event.slot1
            val slot2 = event.slot2

            val evn1 = SlotEvent(slot1, slot1.item, slot1.containerSlot, false, slot2, false)
            val evn2 = SlotEvent(slot2, slot2.item, slot2.containerSlot, false, slot1, true)
            if (!evn1.post() && !evn2.post()) return@on

            event.cancel()
            onCancel("&cPrevented moving item (${(evn1.actors.toSet() + evn2.actors).joinToString(", ")})")
        }
    }

    class SlotEvent(
        val slot: Slot,
        val item: ItemStack,
        val idx: Int,
        val losesItem: Boolean,
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
