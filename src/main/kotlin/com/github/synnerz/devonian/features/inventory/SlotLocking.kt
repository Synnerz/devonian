package com.github.synnerz.devonian.features.inventory

import com.github.synnerz.devonian.Devonian
import com.github.synnerz.devonian.api.ChatUtils
import com.github.synnerz.devonian.api.ScreenUtils
import com.github.synnerz.devonian.api.dungeon.Dungeons
import com.github.synnerz.devonian.api.events.DropItemEvent
import com.github.synnerz.devonian.api.events.GuiKeyEvent
import com.github.synnerz.devonian.api.events.GuiSlotClickEvent
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.utils.JsonUtils
import com.github.synnerz.devonian.utils.Location
import com.google.gson.JsonArray
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.minecraft.client.KeyMapping
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.client.gui.screens.inventory.InventoryScreen
import org.lwjgl.glfw.GLFW

object SlotLocking : Feature(
    "slotLocking",
    "Lock a slot in your inventory to not be able to throw or move the item in that specific slot"
) {
    private const val KEY_NAME = "slotsLocked"
    private var lockedSlots = mutableListOf<Int>()
    private val keybind = KeyBindingHelper.registerKeyBinding(
        KeyMapping(
            "key.devonian.slotLocking",
            GLFW.GLFW_KEY_UNKNOWN,
            Devonian.keybindCategory
        )
    )

    override fun initialize() {
        JsonUtils.set(KEY_NAME, JsonArray())

        JsonUtils.afterLoad {
            lockedSlots = JsonUtils.get<List<String>>(KEY_NAME)?.map { it.toInt() }?.toMutableList() ?: mutableListOf()
        }

        // TODO: impl listener for whenever the player attempts to switch the slot with keys
        //  so the player cannot hit <hotbar slot num key> and it switches out
        on<DropItemEvent> { event ->
            if (minecraft.screen != null) return@on
            if (Location.area == "catacombs" && Dungeons.timeElapsed.value != 0) return@on
            val player = minecraft.player ?: return@on
            val heldSlot = player.inventory?.selectedSlot ?: return@on
            if (!lockedSlots.contains(36 + heldSlot)) return@on

            event.cancel()
            ChatUtils.sendMessage("&cThat slot is locked", true)
        }

        on<GuiSlotClickEvent> { event ->
            val slot = event.slot ?: return@on
            if (slot.container != minecraft.player?.inventory) return@on
            val slotIdx = slot.index
            val screen = minecraft.screen ?: return@on
            val containerSize = (screen as AbstractContainerScreen<*>).menu.slots.size
            // sub the player's inv
            val fixedSize = if (screen is InventoryScreen) 0 else containerSize - 45
            val idx = slotIdx - fixedSize
            if (!lockedSlots.contains(idx)) return@on

            event.cancel()
            ChatUtils.sendMessage("&cThat slot is locked", true)
        }

        on<GuiKeyEvent> { event ->
            if (!keybind.matches(event.event)) return@on
            val slot = ScreenUtils.cursorSlot(event.screen) ?: return@on
            val slotIdx = slot.index
            val containsSlot = lockedSlots.contains(slotIdx)
            val status = if (containsSlot) "&cUnlocked" else "&aLocked"

            if (containsSlot) lockedSlots.remove(slotIdx)
            else lockedSlots.add(slotIdx)
            updateCache()

            ChatUtils.sendMessage("&bSlot &6$slotIdx &bwas $status", true)
        }
        // TODO: impl rendering lock icon in slots
    }

    private fun updateCache() {
        val array = JsonArray()

        lockedSlots.forEach { array.add(it) }

        JsonUtils.set(KEY_NAME, array)
    }
}