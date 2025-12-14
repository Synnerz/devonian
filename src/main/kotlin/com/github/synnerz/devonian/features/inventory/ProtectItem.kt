package com.github.synnerz.devonian.features.inventory

import com.github.synnerz.devonian.Devonian
import com.github.synnerz.devonian.api.ChatUtils
import com.github.synnerz.devonian.api.ItemUtils
import com.github.synnerz.devonian.api.events.GuiCloseEvent
import com.github.synnerz.devonian.api.events.GuiKeyDownEvent
import com.github.synnerz.devonian.api.events.GuiKeyUpEvent
import com.github.synnerz.devonian.config.Config
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.mixin.accessor.AbstractContainerScreenAccessor
import com.github.synnerz.devonian.utils.StringUtils.colorCodes
import com.google.gson.JsonArray
import com.google.gson.JsonPrimitive
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.minecraft.client.KeyMapping
import net.minecraft.world.item.ItemStack
import org.lwjgl.glfw.GLFW

object ProtectItem : Feature(
    "protectItem",
    "Protects an item, so you can no longer accidentally throw it away or sell it.",
    subcategory = "Inventory",
) {
    private const val KEY_NAME = "protectedItems"
    private var lockedList = mutableSetOf<String>()
    private val keybind = KeyBindingHelper.registerKeyBinding(
        KeyMapping(
            "key.devonian.protectItem",
            GLFW.GLFW_KEY_UNKNOWN,
            Devonian.keybindCategory
        )
    )
    private var once = false

    override fun initialize() {
        Config.set(KEY_NAME, JsonArray())

        Config.onAfterLoad {
            lockedList =
                Config.get<List<JsonPrimitive>>(KEY_NAME)?.map { it.asString }?.toMutableSet() ?: mutableSetOf()
        }

        Config.onPreSave {
            val array = JsonArray()

            lockedList.forEach { array.add(it) }

            Config.set(KEY_NAME, array)
        }

        on<GuiKeyDownEvent> { event ->
            if (!keybind.matches(event.event)) return@on
            if (once) return@on
            once = true

            val screen = event.screen as? AbstractContainerScreenAccessor ?: return@on
            val stack = screen.hoveredSlot?.item ?: return@on

            val uuid = ItemUtils.uuid(stack) ?: return@on
            val msg = if (lockedList.contains(uuid)) "&cRemoved" else "&aAdded"

            if (lockedList.contains(uuid)) lockedList.remove(uuid)
            else lockedList.add(uuid)

            ChatUtils.sendMessage("&bProtect item $msg &b${stack.customName?.colorCodes() ?: stack.itemName.string}", true)
        }

        on<GuiKeyUpEvent> { event ->
            if (!keybind.matches(event.event)) return@on
            once = false
        }

        on<GuiCloseEvent> {
            once = false
        }

        on<PreventItem.SlotEvent> { event ->
            if (!event.losesItem) return@on

            if (!isLocked(event.item)) return@on

            event.cancel("ProtectItem")
        }
    }

    private fun isLocked(itemStack: ItemStack?): Boolean {
        if (itemStack == null) return false
        if (itemStack.isEmpty) return false
        val uuid = ItemUtils.uuid(itemStack) ?: return false
        return lockedList.contains(uuid)
    }
}