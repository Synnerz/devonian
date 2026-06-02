package com.github.synnerz.devonian.features.misc.inventory

import com.github.synnerz.devonian.Devonian
import com.github.synnerz.devonian.api.Location
import com.github.synnerz.devonian.api.bufimgrenderer.BufferedImageRenderer
import com.github.synnerz.devonian.api.bufimgrenderer.BufferedImageUploader
import com.github.synnerz.devonian.api.events.*
import com.github.synnerz.devonian.config.Config
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.mixin.accessor.AbstractContainerScreenAccessor
import com.github.synnerz.devonian.utils.BasicState
import com.github.synnerz.devonian.utils.render.Render2D
import com.github.synnerz.devonian.utils.render.states.TexturedQuadRenderState
import com.google.gson.JsonArray
import com.google.gson.JsonPrimitive
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper
import net.minecraft.client.KeyMapping
import net.minecraft.client.gui.render.TextureSetup
import net.minecraft.resources.Identifier
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import org.joml.Matrix3x2f
import org.lwjgl.glfw.GLFW
import java.awt.Color

object SlotLocking : Feature(
    "slotLocking",
    "Lock a slot in your inventory to not be able to throw or move the item in that specific slot.",
    subcategory = "Inventory",
    searchTags = setOf("protect", "prevent", "item"),
) {
    override fun createRequirements(): List<BasicState<Boolean>?> {
        return listOf(Location.stateInSkyblock)
    }

    private val SETTING_STYLE = addSelection(
        "style",
        0,
        listOf("Lock Icon", "Box"),
        "",
        "Locked Slot Style",
    )
    private val SETTING_LOCKED_SLOT_COLOR = addColorPicker(
        "slotColor",
        Color.RED.rgb,
        "",
        "Locked Slot Outline Color",
    )

    // TODO: make profile based as well
    private const val KEY_NAME = "slotsLocked"
    private const val KEY_NAME_RIFT = "slotsLockedRift"
    private val lockedSlots = Array(40) { false }
    private val lockedSlotsRift = Array(40) { false }
    private val keybind = KeyMappingHelper.registerKeyMapping(
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
            Config.get<List<JsonPrimitive>>(KEY_NAME_RIFT)?.map { it.asBoolean }?.forEachIndexed { idx, bool ->
                lockedSlotsRift[idx] = bool
            }
        }

        Config.onPreSave {
            val array = JsonArray()

            lockedSlots.forEach { array.add(it) }

            Config.set(KEY_NAME, array)
            Config.set(KEY_NAME_RIFT, JsonArray().also { lockedSlotsRift.forEach { v -> it.add(v) } })
        }

        on<PreventItem.SlotEvent> { event ->
            val locked = lockSlots().getOrNull(event.idx) ?: return@on
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

            val containsSlot = lockSlots().getOrNull(idx) ?: return@on
            lockSlots()[idx] = !containsSlot
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

            val locked = lockSlots().getOrNull(idx) ?: return@on
            if (!locked) return@on

            if (SlotBinding.compatIsRendering(slot)) {
                event.ctx.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, SETTING_LOCKED_SLOT_COLOR.get())
            } else {
                Render2D.drawWireRect(event.ctx, slot.x, slot.y, 16, 16, SETTING_LOCKED_SLOT_COLOR.getColor(), lw = 2)
            }
        }.apply {
            prio = 10
            setEnabled(SETTING_STYLE.state.map { it == 1 })
        }

        on<PostRenderSlotsEvent> { event ->
            val lock = TextureSetup(
                lockUploader.textureView, null, null,
                lockUploader.sampler, null, null,
            )
            val inv = minecraft.player?.inventory
            val mat = Matrix3x2f(event.ctx.pose())

            event.container.menu.slots.forEach { slot ->
                if (slot.container !== inv) return@forEach
                if (lockSlots().getOrNull(slot.containerSlot) != true) return@forEach

                event.ctx.guiRenderState.addGuiElement(
                    TexturedQuadRenderState(
                        BufferedImageRenderer.pipeline,
                        lock,
                        mat,
                        slot.x + 0f, slot.y + 0f,
                        slot.x + 16f, slot.y + 16f,
                        0f, 0f,
                        1f, 1f,
                        0x66FFFFFF,
                        event.ctx.scissorStack.peek(),
                    )
                )
            }
        }.apply {
            prio = 1
            setEnabled(SETTING_STYLE.state.map { it == 0 })
        }
    }

    fun lockSlots(): Array<Boolean> =
        if (Location.area == "the rift") lockedSlotsRift
        else lockedSlots

    // from sba
    private val mcidLock = Identifier.fromNamespaceAndPath("devonian", "lock")
    private val lockUploader = BufferedImageUploader.fromResource("/assets/devonian/lock.png")!!
        .register(mcidLock)
}