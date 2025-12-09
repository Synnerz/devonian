package com.github.synnerz.devonian.features.inventory

import com.github.synnerz.devonian.Devonian
import com.github.synnerz.devonian.api.ChatUtils
import com.github.synnerz.devonian.api.ScreenUtils
import com.github.synnerz.devonian.api.events.GuiKeyEvent
import com.github.synnerz.devonian.api.events.GuiSlotClickEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.config.Config
import com.github.synnerz.devonian.features.Feature
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.mojang.blaze3d.platform.InputConstants
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.minecraft.client.KeyMapping
import net.minecraft.client.gui.screens.inventory.InventoryScreen
import org.lwjgl.glfw.GLFW

object SlotBinding : Feature(
    "slotBinding",
    "Bind a slot to another slot (with keybind in controls) so you can shift + left click on it to swap each others' items around"
) {
    private const val KEY_NAME = "slotsBound"
    private val boundSlots = mutableListOf<SlotBindingData>()
    private val keybind = KeyBindingHelper.registerKeyBinding(
        KeyMapping(
            "key.devonian.slotBinding",
            GLFW.GLFW_KEY_UNKNOWN,
            Devonian.keybindCategory
        )
    )
    var bindingData: SlotBindingData? = null

    data class SlotBindingData(
        val from: Int,
        var to: Int? = null
    ) {
        fun containsOneOf(idx: Int): Boolean = (idx == from || idx == to)

        fun containsHotbarSlot(): Boolean = (isHotbar(from) || isHotbar(to!!))

        fun from(): Int  {
            if (isHotbar(from)) return to!!

            return from
        }

        fun to(): Int {
            if (isHotbar(to!!)) return to!! - 36

            return from - 36
        }
    }

    override fun initialize() {
        Config.set(KEY_NAME, JsonObject())

        Config.onAfterLoad {
            val localData = Config.get<Map<String, JsonElement>>(KEY_NAME) ?: return@onAfterLoad
            for (data in localData) {
                val k = data.key.toIntOrNull() ?: continue
                val v = data.value.asInt
                boundSlots.add(SlotBindingData(k, v))
            }
        }

        on<GuiKeyEvent> { event ->
            if (!keybind.matches(event.event)) return@on
            if (event.screen !is InventoryScreen) return@on
            val slot = ScreenUtils.cursorSlot(event.screen) ?: return@on
            val slotId = slot.index
            if (!isValid(slotId)) {
                bindingData = null
                return@on
            }

            // If the data is null, this mean that there is a new bound being created,
            // so we detect that and begin creating it
            if (bindingData == null) {
                // First we double check that our list doesn't contain the slotId
                boundSlots.removeIf {
                    val bl = it.containsOneOf(slotId)
                    if (bl)
                        ChatUtils.sendMessage("&cRemoving bound slot &b$slotId", true)
                    bl
                }
                bindingData = SlotBindingData(slotId)
                return@on
            }

            if (bindingData!!.to != null) return@on
            if (bindingData!!.to == slotId) {
                ChatUtils.sendMessage("&7That slot is already bound to $slotId", true)
                bindingData = null
                return@on
            }
            if (bindingData!!.from == slotId) {
                ChatUtils.sendMessage("&cYou cannot bind the slot to itself funny guy", true)
                bindingData = null
                return@on
            }

            bindingData!!.to = slotId
            if (!bindingData!!.containsHotbarSlot()) {
                ChatUtils.sendMessage("&cYou are required to bind at least one Hotbar slot", true)
                bindingData = null
                return@on
            }
            boundSlots.add(bindingData!!)
            ChatUtils.sendMessage("&bBound slot &6${bindingData!!.from} &b-> &6${bindingData!!.to}", true)
            bindingData = null
            updateCache()
        }

        on<GuiSlotClickEvent> { event ->
            if (event.mbtn != 0 || minecraft.screen !is InventoryScreen || !InputConstants.isKeyDown(minecraft.window, GLFW.GLFW_KEY_LEFT_SHIFT)) return@on
            val slotIdx = event.slotId
            val data = boundSlots.find { it.containsOneOf(slotIdx) } ?: return@on
            if (data.to == null) return@on

            event.cancel()
            ScreenUtils.swap(data.from(), data.to())
        }
        // TODO: impl rendering line when the player is shifting a bound slot
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        bindingData = null
    }

    private fun updateCache() {
        val obj = JsonObject()

        boundSlots.forEach {
            obj.addProperty("${it.from}", it.to)
        }

        Config.set(KEY_NAME, obj)
    }

    private fun isValid(idx: Int): Boolean
        = idx in 9..<45

    private fun isHotbar(idx: Int): Boolean
        = idx in 36..<45
}