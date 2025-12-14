package com.github.synnerz.devonian.features.inventory

import com.github.synnerz.devonian.Devonian
import com.github.synnerz.devonian.api.events.GuiCloseEvent
import com.github.synnerz.devonian.api.events.GuiKeyDownEvent
import com.github.synnerz.devonian.api.events.GuiKeyUpEvent
import com.github.synnerz.devonian.api.events.PostRenderSlotsEvent
import com.github.synnerz.devonian.api.events.QuickMoveItemEvent
import com.github.synnerz.devonian.api.events.RenderSlotEvent
import com.github.synnerz.devonian.config.Config
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.mixin.accessor.AbstractContainerScreenAccessor
import com.github.synnerz.devonian.utils.Render2D
import com.google.gson.JsonArray
import com.google.gson.JsonPrimitive
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.minecraft.client.KeyMapping
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.inventory.ClickType
import net.minecraft.world.inventory.Slot
import org.lwjgl.glfw.GLFW
import java.awt.Color

object SlotBinding : Feature(
    "slotBinding",
    "Bind a slot to another slot (with keybind in controls) so you can shift + left click on it to swap each others' items around",
    subcategory = "Inventory",
) {
    private val SETTING_BOUND_LINES = addSwitch(
        "boundLines",
        true,
        "draw lines connecting bound slots",
        "Connect Bound Slots",
    )
    private val SETTING_PROTECT = addSwitch(
        "protect",
        true,
        "",
        "Protect Bound Slots",
    )

    private const val KEY_NAME = "slotsBound1"
    private val keybind = KeyBindingHelper.registerKeyBinding(
        KeyMapping(
            "key.devonian.slotBinding",
            GLFW.GLFW_KEY_UNKNOWN,
            Devonian.keybindCategory
        )
    )
    private var once = false

    private val boundSlots = Array(40) { mutableListOf<Int>() }
    private var currentHeldSlot: Slot? = null
    private val slotLocCache = arrayListOf<Pair<Int, Int>?>()

    private val TOGGLE_SOUND = SoundEvents.EXPERIENCE_ORB_PICKUP

    // https://colorbrewer2.org/#type=qualitative&scheme=Set1&n=9
    private val slotColors = arrayOf(
        Color(228,26,28),
        Color(55,126,184),
        Color(77,175,74),
        Color(152,78,163),
        Color(255,127,0),
        Color(255,255,51),
        Color(166,86,40),
        Color(247,129,191),
        Color(153,153,153),
    )

    private fun colorFor(idx: Int, other: Int): Color {
        return slotColors.getOrElse(idx) { slotColors[other] }
    }

    override fun initialize() {
        Config.set(KEY_NAME, JsonArray())

        Config.onAfterLoad {
            Config.get<List<JsonPrimitive>>(KEY_NAME)?.map { it.asInt }?.forEach {
                val src = it shr 6
                val dst = it and 63
                boundSlots[src].add(dst)
            }
        }

        Config.onPreSave {
            val array = JsonArray()

            boundSlots.forEachIndexed { src, arr ->
                arr.forEach { dst ->
                    val packed = (src shl 6) or dst
                    array.add(packed)
                }
            }

            Config.set(KEY_NAME, array)
        }

        on<GuiKeyDownEvent> { event ->
            if (!keybind.matches(event.event)) return@on
            if (once) return@on
            once = true

            val screen = event.screen as? AbstractContainerScreenAccessor ?: return@on
            val slot = screen.hoveredSlot ?: return@on

            if (slot.container !== minecraft.player?.inventory) return@on
            if (slot.containerSlot !in boundSlots.indices) return@on

            currentHeldSlot = slot
            minecraft.level?.playPlayerSound(
                TOGGLE_SOUND,
                SoundSource.MASTER,
                1f, 1f
            )
        }

        on<GuiKeyUpEvent> { event ->
            if (!keybind.matches(event.event)) return@on
            once = false

            val curr = currentHeldSlot ?: return@on
            currentHeldSlot = null

            val screen = event.screen as? AbstractContainerScreenAccessor ?: return@on
            val slot = screen.hoveredSlot ?: return@on

            if (slot.container !== minecraft.player?.inventory) return@on
            val idx = slot.containerSlot
            if (idx !in boundSlots.indices) return@on

            if (slot === curr) {
                boundSlots[idx].forEach {
                    boundSlots[it].remove(idx)
                }
                boundSlots[idx].clear()
                // minecraft.level?.playPlayerSound(
                //     TOGGLE_SOUND,
                //     SoundSource.MASTER,
                //     1f, 0.1f
                // )
                return@on
            }

            val other = curr.containerSlot
            if (idx >= 9 && other >= 9) return@on

            if (boundSlots[idx].contains(other)) return@on
            boundSlots[idx].add(other)
            boundSlots[other].add(idx)
        }

        on<GuiCloseEvent> {
            currentHeldSlot = null
            once = false
        }

        on<PreventItem.SlotEvent> { event ->
            (event.underlying as? QuickMoveItemEvent)?.also {
                val slots = boundSlots.getOrNull(event.idx) ?: return@on
                val other = slots.getOrNull(0) ?: return@on

                val player = minecraft.player ?: return@also
                val menu = player.containerMenu ?: return@also
                val id = menu.containerId

                if (event.idx < 9 && other >= 9) {
                    val inv = player.inventory
                    val dst = menu.slots.find { it.container === inv && it.containerSlot == other }!!
                    minecraft.gameMode?.handleInventoryMouseClick(id, dst.index, event.idx, ClickType.SWAP, player)
                } else minecraft.gameMode?.handleInventoryMouseClick(id, event.slot.index, other, ClickType.SWAP, player)
                event.cancel()
                return@on
            }

            if (SETTING_PROTECT.get()) {
                if (event.swapped != null) {
                    val slots = boundSlots.getOrNull(event.idx) ?: return@on
                    if (!slots.contains(event.swapped.containerSlot)) event.cancel("SlotBinding")
                    return@on
                }

                val slots = boundSlots.getOrNull(event.idx) ?: return@on
                if (slots.isEmpty()) return@on

                event.cancel("SlotBinding")
            }
        }

        on<RenderSlotEvent> { event ->
            val slot = event.slot
            if (slot.container !== minecraft.player?.inventory) return@on

            val idx = slot.containerSlot
            val bound = boundSlots.getOrNull(idx) ?: return@on

            while (slotLocCache.size <= idx) slotLocCache.add(null)
            slotLocCache[idx] = Pair(slot.x, slot.y)

            bound.forEach { other ->
                val c = colorFor(idx, other)
                Render2D.drawWireRect(event.ctx, slot.x, slot.y, 16, 16, c, lw = 2)

                if (!SETTING_BOUND_LINES.get()) return@forEach
                if (idx > other) {
                    val loc = slotLocCache[other] ?: return@forEach
                    Render2D.drawLine(
                        event.ctx,
                        slot.x + 8f, slot.y + 8f,
                        loc.first + 8f, loc.second + 8f,
                        c,
                    )
                }
            }
        }

        on<PostRenderSlotsEvent> { event ->
            val curr = currentHeldSlot ?: return@on
            val cont = event.container as AbstractContainerScreenAccessor

            event.container.menu.slots.forEach { slot ->
                if (curr === slot) return@forEach
                if (
                    (curr.containerSlot < 9 || slot.containerSlot < 9) &&
                    !boundSlots[curr.containerSlot].contains(slot.containerSlot)
                ) return@forEach

                Render2D.drawLine(
                    event.ctx,
                    slot.x + 4f, slot.y + 4f,
                    slot.x + 12f, slot.y + 12f,
                    Color.RED,
                )
                Render2D.drawLine(
                    event.ctx,
                    slot.x + 12f, slot.y + 4f,
                    slot.x + 4f, slot.y + 12f,
                    Color.RED,
                )
            }

            Render2D.drawLine(
                event.ctx,
                curr.x + 8f, curr.y + 8f,
                (event.mouseX - cont.leftPos).toFloat(), (event.mouseY - cont.topPos).toFloat(),
                Color.GREEN,
            )
        }
    }

    fun compatIsRendering(slot: Slot): Boolean {
        if (!isEnabled()) return false
        if (slot.container != minecraft.player?.inventory) return false
        return boundSlots.getOrNull(slot.containerSlot)?.isNotEmpty() ?: false
    }
}