package com.github.synnerz.devonian.features.debug

import com.github.synnerz.devonian.api.ChatUtils
import com.github.synnerz.devonian.api.events.GuiKeyEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.config.json.JsonDataObject
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.mixin.accessor.AbstractContainerScreenAccessor
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

            minecraft.keyboardHandler.clipboard = serializeItem(stack)
            ChatUtils.sendMessage(
                Component.literal("§aCopied ")
                    .append(stack.displayName)
                    .append(Component.literal("§r§a to clipboard."))
            )
        }
    }

    fun serializeItem(stack: ItemStack): String {
        val item = stack.item
        val obj = JsonDataObject(JsonObject())

        stack.get(DataComponents.CUSTOM_NAME)?.let {
            obj.set("name", it.colorCodes())
            obj.set("name_", it.string)
        }
        obj.set("id", BuiltInRegistries.ITEM.getKey(item).toString())
        obj.set("count", stack.count)
        obj.set("damage", stack.damageValue)

        stack.get(DataComponents.PROFILE)?.let {
            val data = it.partialProfile() ?: return@let
            val profile = obj.getObject("Profile")
            profile.set("id", data.id.toString())
            profile.set("name", data.name)
            data.properties.forEach { k, v ->
                val e = profile.getObject(k)
                e.set("name", v.name)
                e.set("value", v.value)
                e.set("signature", v.signature)
            }
        }

        stack.get(DataComponents.LORE)?.let {
            val lines = it.lines
            val arr = JsonArray(lines.size)
            val arrUnf = JsonArray(lines.size)
            lines.forEach {
                arr.add(it.colorCodes())
                arrUnf.add(it.string)
            }
            obj.set("Lore", arr)
            obj.set("Lore_", arrUnf)
        }

        stack.get(DataComponents.CUSTOM_DATA)?.let {
            val nbt = it.copyTag()
            obj.set("ExtraAttributes", serializeNBT(nbt))
        }

        return obj.toString()
    }

    private fun serializeNBT(nbt: Tag): JsonElement? = when (nbt) {
        is EndTag -> null

        is StringTag -> JsonPrimitive(nbt.value)

        is ByteTag -> JsonPrimitive(nbt.value)
        is ShortTag -> JsonPrimitive(nbt.value)
        is IntTag -> JsonPrimitive(nbt.value)
        is LongTag -> JsonPrimitive(nbt.value)
        is FloatTag -> JsonPrimitive(nbt.value)
        is DoubleTag -> JsonPrimitive(nbt.value)

        is ByteArrayTag -> {
            val arr = nbt.asByteArray
            val json = JsonArray(nbt.size())
            arr.forEach { json.add(it) }
            json
        }

        is IntArrayTag -> {
            val arr = nbt.asIntArray
            val json = JsonArray(nbt.size())
            arr.forEach { json.add(it) }
            json
        }

        is LongArrayTag -> {
            val arr = nbt.asLongArray
            val json = JsonArray(nbt.size())
            arr.forEach { json.add(it) }
            json
        }

        is ListTag -> {
            val json = JsonArray(nbt.size)
            for (i in 0 until nbt.size) {
                json.add(serializeNBT(nbt[i]))
            }
            json
        }

        is CompoundTag -> {
            val json = JsonObject()
            nbt.forEach { k, v -> json.add(k, serializeNBT(v)) }
            json
        }
    }
}