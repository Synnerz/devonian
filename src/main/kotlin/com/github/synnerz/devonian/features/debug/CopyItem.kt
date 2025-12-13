package com.github.synnerz.devonian.features.debug

import com.github.synnerz.devonian.api.ChatUtils
import com.github.synnerz.devonian.api.events.GuiKeyEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.config.json.JsonDataObject
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.mixin.accessor.AbstractContainerScreenAccessor
import com.github.synnerz.devonian.utils.Serializer
import com.github.synnerz.devonian.utils.StringUtils.colorCodes
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.nbt.*
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import org.lwjgl.glfw.GLFW.*

object CopyItem : Feature(
    "copyItem",
    "Ctrl-C",
    Categories.DEBUG,
    subcategory = "Utils",
) {
    override fun initialize() {
        on<GuiKeyEvent> { event ->
            if (event.key != GLFW_KEY_C) return@on

            val isCtrlDown = glfwGetKey(minecraft.window.handle(), GLFW_KEY_LEFT_CONTROL) == GLFW_PRESS
            if (!isCtrlDown) return@on

            val screen = minecraft.screen as? AbstractContainerScreenAccessor ?: return@on
            val slot = screen.hoveredSlot ?: return@on

            event.cancel()

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