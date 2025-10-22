package com.github.synnerz.devonian.features.misc

import com.github.synnerz.devonian.api.ChatUtils
import com.github.synnerz.devonian.api.ItemUtils
import com.github.synnerz.devonian.api.Scheduler
import com.github.synnerz.devonian.api.events.CancellableEvent
import com.github.synnerz.devonian.api.events.DropItemEvent
import com.github.synnerz.devonian.api.events.GuiKeyEvent
import com.github.synnerz.devonian.api.events.GuiSlotClickEvent
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.mixin.accessor.HandledScreenAccessor
import com.github.synnerz.devonian.utils.*
import com.google.gson.JsonArray
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.minecraft.block.Blocks
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.client.gui.screen.ingame.InventoryScreen
import net.minecraft.client.option.KeyBinding
import net.minecraft.item.ItemStack
import net.minecraft.sound.SoundEvent
import net.minecraft.util.Identifier
import org.lwjgl.glfw.GLFW

object ProtectItem : Feature("protectItem") {
    private var lockedList = mutableListOf<String>()
    private val hopperItem = Blocks.HOPPER.asItem()
    private val keybind = KeyBindingHelper.registerKeyBinding(
        KeyBinding(
            "key.devonian.protectItem",
            GLFW.GLFW_KEY_UNKNOWN,
            "devonian"
        )
    )

    override fun initialize() {
        JsonUtils.set("protectedItems", JsonArray())

        JsonUtils.afterLoad {
            lockedList = JsonUtils.get<List<String>>("protectedItems")?.toMutableList() ?: mutableListOf()
        }

        on<DropItemEvent> { event ->
            if (Location.area == "catacombs" && minecraft.currentScreen == null) return@on
            val stack = event.itemStack
            val uuid = ItemUtils.uuid(stack) ?: return@on
            if (!lockedList.contains(uuid)) return@on
            cancelEvent(event, stack)
        }

        on<GuiSlotClickEvent> { event ->
            if (event.slot == null) return@on
            if (event.slot.inventory !== minecraft.player?.inventory) return@on
            val screen = minecraft.currentScreen ?: return@on
            if (screen !is GenericContainerScreen) return@on
            val possibleHopper = event.handler.getSlot(49)?.stack
            if (possibleHopper != null && event.slotId != 49 && isLocked(event.slot.stack)) {
                val lore = ItemUtils.lore(possibleHopper)
                if (possibleHopper.item == hopperItem && possibleHopper.customName?.string == "Sell Item")
                    cancelEvent(event, event.slot.stack)
                else if (lore != null && lore.any { it.contains("buyback") })
                    cancelEvent(event, event.slot.stack)
            }
            if (!screen.title.string.contains("Auction") || !isLocked(event.slot.stack)) return@on

            cancelEvent(event, event.slot.stack)
        }

        on<GuiKeyEvent> { event ->
            val key = event.key
            val scancode = event.scanCode
            if (!keybind.matchesKey(key, scancode)) return@on
            val screen = event.screen
            val stack = cursorStack(screen) ?: return@on
            val uuid = ItemUtils.uuid(stack) ?: return@on
            val msg = if (lockedList.contains(uuid)) "&cRemoved" else "&aAdded"

            if (lockedList.contains(uuid)) lockedList.remove(uuid)
            else lockedList.add(uuid)
            updateCache()

            ChatUtils.sendMessage("&bProtect item $msg &b${stack.name.string}", true)
        }
    }

    // TODO: make this its own utils file (Looking at you ScreenUtils)
    private fun cursorStack(screen: Screen): ItemStack? {
        if (!(screen is InventoryScreen || screen is GenericContainerScreen)) return null
        val handled = screen as HandledScreen<*>
        val accessor = handled as HandledScreenAccessor
        val window = minecraft.window ?: return null

        return accessor.getSlotAtPos(
            minecraft.mouse.getScaledX(window),
            minecraft.mouse.getScaledY(window)
        )?.stack
    }

    private fun isLocked(itemStack: ItemStack?): Boolean {
        if (itemStack == null) return false
        val uuid = ItemUtils.uuid(itemStack) ?: return false
        return lockedList.contains(uuid)
    }

    private fun updateCache() {
        val array = JsonArray()

        lockedList.forEach  {  array.add(it)}

        JsonUtils.set("protectedItems", array)
    }

    private fun cancelEvent(event: CancellableEvent, stack: ItemStack) {
        Scheduler.scheduleTask(1) {
            minecraft.player?.playSound(
                SoundEvent.of(Identifier.ofVanilla("block.note_block.bass")),
                1f,
                1f
            )
        }
        event.cancel()

        val customName = if (stack.customName == null) ChatUtils.literal("&cName not found") else stack.customName
        val text = ChatUtils.literal("${ChatUtils.prefix} &cCancelled attempt at dropping ")
            .append(customName)
        ChatUtils.sendMessage(text)
    }
}