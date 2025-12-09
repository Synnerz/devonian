package com.github.synnerz.devonian.features.inventory

import com.github.synnerz.devonian.Devonian
import com.github.synnerz.devonian.api.ChatUtils
import com.github.synnerz.devonian.api.ItemUtils
import com.github.synnerz.devonian.api.Scheduler
import com.github.synnerz.devonian.api.ScreenUtils
import com.github.synnerz.devonian.api.dungeon.Dungeons
import com.github.synnerz.devonian.api.events.CancellableEvent
import com.github.synnerz.devonian.api.events.DropItemEvent
import com.github.synnerz.devonian.api.events.GuiKeyEvent
import com.github.synnerz.devonian.api.events.GuiSlotClickEvent
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.config.JsonUtils
import com.github.synnerz.devonian.utils.Location
import com.google.gson.JsonArray
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.minecraft.client.KeyMapping
import net.minecraft.client.gui.screens.inventory.ContainerScreen
import net.minecraft.resources.ResourceLocation
import net.minecraft.sounds.SoundEvent
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.Blocks
import org.lwjgl.glfw.GLFW

object ProtectItem : Feature(
    "protectItem",
    "Protects an item, so you can no longer accidentally throw it away or sell it."
) {
    private var lockedList = mutableListOf<String>()
    private val hopperItem = Blocks.HOPPER.asItem()
    private val keybind = KeyBindingHelper.registerKeyBinding(
        KeyMapping(
            "key.devonian.protectItem",
            GLFW.GLFW_KEY_UNKNOWN,
            Devonian.keybindCategory
        )
    )

    override fun initialize() {
        JsonUtils.set("protectedItems", JsonArray())

        JsonUtils.afterLoad {
            lockedList = JsonUtils.get<List<String>>("protectedItems")?.toMutableList() ?: mutableListOf()
        }

        on<DropItemEvent> { event ->
            if (Location.area == "catacombs" && Dungeons.timeElapsed.value != 0 && minecraft.screen == null) return@on
            val stack = event.itemStack
            val uuid = ItemUtils.uuid(stack) ?: return@on
            if (!lockedList.contains(uuid)) return@on
            cancelEvent(event, stack)
        }

        on<GuiSlotClickEvent> { event ->
            if (event.slot == null) return@on
            if (event.slot.container !== minecraft.player?.inventory) return@on
            val screen = minecraft.screen ?: return@on
            if (screen !is ContainerScreen) return@on
            val possibleHopper = event.handler.getSlot(49)?.item
            if (possibleHopper != null && event.slotId != 49 && isLocked(event.slot.item)) {
                val lore = ItemUtils.lore(possibleHopper)
                if (possibleHopper.item == hopperItem && possibleHopper.customName?.string == "Sell Item")
                    cancelEvent(event, event.slot.item)
                else if (lore != null && lore.any { it.contains("buyback") })
                    cancelEvent(event, event.slot.item)
            }
            if (!screen.title.string.contains("Auction") || !isLocked(event.slot.item)) return@on

            cancelEvent(event, event.slot.item)
        }

        on<GuiKeyEvent> { event ->
            if (!keybind.matches(event.event)) return@on
            val stack = ScreenUtils.cursorStack(event.screen) ?: return@on
            val uuid = ItemUtils.uuid(stack) ?: return@on
            val msg = if (lockedList.contains(uuid)) "&cRemoved" else "&aAdded"

            if (lockedList.contains(uuid)) lockedList.remove(uuid)
            else lockedList.add(uuid)
            updateCache()

            ChatUtils.sendMessage("&bProtect item $msg &b${stack.customName?.string ?: stack.itemName.string}", true)
        }
    }

    private fun isLocked(itemStack: ItemStack?): Boolean {
        if (itemStack == null) return false
        val uuid = ItemUtils.uuid(itemStack) ?: return false
        return lockedList.contains(uuid)
    }

    private fun updateCache() {
        val array = JsonArray()

        lockedList.forEach { array.add(it) }

        JsonUtils.set("protectedItems", array)
    }

    private fun cancelEvent(event: CancellableEvent, stack: ItemStack) {
        Scheduler.scheduleTask(1) {
            minecraft.player?.playSound(
                SoundEvent.createVariableRangeEvent(ResourceLocation.withDefaultNamespace("block.note_block.bass")),
                1f,
                1f
            )
        }
        event.cancel()

        val customName = stack.customName ?: ChatUtils.literal("&cName not found")
        val text = ChatUtils.literal("${ChatUtils.prefix} &cCancelled attempt at dropping ")
            .append(customName)
        ChatUtils.sendMessage(text)
    }
}