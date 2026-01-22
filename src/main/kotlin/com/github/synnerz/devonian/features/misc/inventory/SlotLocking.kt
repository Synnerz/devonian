package com.github.synnerz.devonian.features.misc.inventory

import com.github.synnerz.devonian.Devonian
import com.github.synnerz.devonian.api.events.GuiCloseEvent
import com.github.synnerz.devonian.api.events.GuiKeyDownEvent
import com.github.synnerz.devonian.api.events.GuiKeyUpEvent
import com.github.synnerz.devonian.api.events.RenderSlotEvent
import com.github.synnerz.devonian.config.Config
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.mixin.accessor.AbstractContainerScreenAccessor
import com.github.synnerz.devonian.utils.render.Render2D
import com.google.gson.JsonArray
import com.google.gson.JsonPrimitive
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.minecraft.client.KeyMapping
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import org.lwjgl.glfw.GLFW
import java.awt.Color

object SlotLocking : Feature(
    "slotLocking",
    "Lock a slot in your inventory to not be able to throw or move the item in that specific slot.",
    subcategory = "Inventory",
    searchTags = setOf("protect", "prevent", "item"),
) {
    private val SETTING_LOCKED_SLOT_COLOR = addColorPicker(
        "slotColor",
        Color.RED.rgb,
        "",
        "Locked Slot Outline Color",
    )

    private const val KEY_NAME = "slotsLocked"
    private val lockedSlots = Array(40) { false }
    private val keybind = KeyBindingHelper.registerKeyBinding(
        KeyMapping(
            "key.devonian.slotLocking",
            GLFW.GLFW_KEY_UNKNOWN,
            Devonian.keybindCategory
        )
    )
    private var once = false

    private val TOGGLE_SOUND = SoundEvents.EXPERIENCE_ORB_PICKUP

    override fun initialize() {
        Config.set(KEY_NAME, JsonArray())

        Config.onAfterLoad {
            Config.get<List<JsonPrimitive>>(KEY_NAME)?.map { it.asBoolean }?.forEachIndexed { i, v ->
                lockedSlots[i] = v
            }
        }

        Config.onPreSave {
            val array = JsonArray()

            lockedSlots.forEach { array.add(it) }

            Config.set(KEY_NAME, array)
        }

        on<PreventItem.SlotEvent> { event ->
            val locked = lockedSlots.getOrNull(event.idx) ?: return@on
            if (locked) event.cancel("SlotLocking")
        }

        on<GuiKeyDownEvent> { event ->
            if (!keybind.matches(event.event)) return@on
            if (once) return@on
            once = true

            val screen = event.screen as? AbstractContainerScreenAccessor ?: return@on
            val slot = screen.hoveredSlot ?: return@on
            if (slot.container !== minecraft.player?.inventory) return@on

            val idx = slot.containerSlot

            val containsSlot = lockedSlots.getOrNull(idx) ?: return@on
            lockedSlots[idx] = !containsSlot
            minecraft.level?.playPlayerSound(
                TOGGLE_SOUND,
                SoundSource.MASTER,
                1f, if (containsSlot) 0.1f else 1f
            )
        }

        on<GuiKeyUpEvent> { event ->
            if (!keybind.matches(event.event)) return@on
            once = false
        }

        on<GuiCloseEvent> {
            once = false
        }

        on<RenderSlotEvent> { event ->
            val slot = event.slot
            if (slot.container !== minecraft.player?.inventory) return@on

            val idx = slot.containerSlot

            val locked = lockedSlots.getOrNull(idx) ?: return@on
            if (!locked) return@on

            if (SlotBinding.compatIsRendering(slot)) {
                event.ctx.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, SETTING_LOCKED_SLOT_COLOR.get())
            } else {
                Render2D.drawWireRect(event.ctx, slot.x, slot.y, 16, 16, SETTING_LOCKED_SLOT_COLOR.getColor(), lw = 2)
            }
        }.prio = 10
    }
}