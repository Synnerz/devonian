package com.github.synnerz.devonian.features.debug

import com.github.synnerz.devonian.api.ChatUtils
import com.github.synnerz.devonian.api.events.GuiKeyDownEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.config.json.JsonDataObject
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.mixin.accessor.AbstractContainerScreenAccessor
import com.github.synnerz.devonian.utils.Serializer
import com.github.synnerz.devonian.utils.StringUtils.colorCodes
import com.google.gson.JsonArray
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import org.lwjgl.glfw.GLFW.*

object CopyItem : Feature(
    "copyItem",
    "Ctrl-C when hovering over an item to copy it to the clipboard.",
    Categories.DEBUG,
    subcategory = "Utils",
    searchTags = setOf("nbt", "debug"),
) {
    override fun initialize() {
        on<GuiKeyDownEvent> { event ->
            if (event.key != GLFW_KEY_C) return@on

            val isCtrlDown = glfwGetKey(minecraft.window.handle(), GLFW_KEY_LEFT_CONTROL) == GLFW_PRESS
            if (!isCtrlDown) return@on

            val screen = minecraft.gui.screen() as? AbstractContainerScreen<*> ?: return@on
            val screenAcc = minecraft.gui.screen() as? AbstractContainerScreenAccessor ?: return@on
            event.cancel()

            val slot = screenAcc.hoveredSlot
            if (slot == null) {
                val obj = JsonDataObject()
                obj.set("name", screen.title.colorCodes())
                obj.set("name_", screen.title.string)
                obj.set(
                    "type",
                    try {
                        BuiltInRegistries.MENU.getKey(screen.menu.type).toString()
                    } catch (_: Exception) {
                        "UNKNOWN"
                    }
                )
                obj.set("id", screen.menu.containerId)
                val items = JsonArray()
                screen.menu.slots.forEachIndexed { idx, slot ->
                    val o = JsonDataObject()
                    o.set("index", slot.index)
                    o.set("containerSlot", slot.containerSlot)
                    o.set("container", slot.container::class.java.name)
                    val stack = slot.item
                    if (!stack.isEmpty) o.set("item", Serializer.serializeItem(stack))
                    items.add(o.json)
                }
                obj.set("items", items)
                minecraft.keyboardHandler.clipboard = obj.toString()
                ChatUtils.sendMessage(
                    Component.literal("§aCopied ")
                        .append(screen.title.colorCodes())
                        .append(Component.literal("§r§a to clipboard."))
                )
                return@on
            }

            if (!slot.hasItem()) {
                ChatUtils.sendMessage("No item to copy.")
                return@on
            }

            val stack = slot.item

            minecraft.keyboardHandler.clipboard = Serializer.serializeItem(stack).toString()
            ChatUtils.sendMessage(
                Component.literal("§aCopied ")
                    .append(stack.displayName)
                    .append(Component.literal("§r§a to clipboard."))
            )
        }
    }
}