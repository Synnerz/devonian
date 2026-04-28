package com.github.synnerz.devonian.features.misc.inventory

import com.github.synnerz.devonian.Devonian
import com.github.synnerz.devonian.api.ChatUtils
import com.github.synnerz.devonian.api.Location
import com.github.synnerz.devonian.api.events.*
import com.github.synnerz.devonian.commands.DevonianCommand
import com.github.synnerz.devonian.config.Config
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.mixin.accessor.AbstractContainerScreenAccessor
import com.github.synnerz.devonian.utils.BasicState
import com.github.synnerz.devonian.utils.PersistentJsonClass
import com.github.synnerz.devonian.utils.render.Render2D
import com.google.gson.JsonArray
import com.google.gson.JsonPrimitive
import com.google.gson.reflect.TypeToken
import com.mojang.blaze3d.platform.InputConstants
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
    "Bind a slot to another slot (with keybind in controls) so you can shift + left click on it to swap each others' items around. /dv slotbinding <mode> <name?> <useCurrentArea?>",
    subcategory = "Inventory",
) {
    override fun createRequirements(): List<BasicState<Boolean>?> {
        return listOf(Location.stateInSkyblock)
    }

    private val SETTING_PROTECT = addSwitch(
        "protect",
        true,
        "Prevent items in bound slots from being moved normally.",
        "Protect Bound Slots",
    )
    private val SETTING_DYNAMIC_CHANGE = addSwitch(
        "dynamicChange",
        true,
        "Changes the selected profile depending on the area (if disabled, it will not change)",
        "Dynamic Change"
    )
    private val SETTING_BOUND_LINES = addSwitch(
        "boundLines",
        true,
        "Draws outline on the bound slots.",
        "Bound Outline",
    )
    private val SETTING_POINTING_LINE_MODE = addSelection(
        "pointingLineMode",
        0,
        listOf("None", "Always", "Shifting"),
        "The mode to use for displaying lines between two binds.",
        "Pointing Line Mode",
    )
    private val SETTING_SAME_COLOR = addSwitch(
        "useSameColor",
        false,
        "Use the same color (the one below) for all bound slots, rather than " +
        "selecting a color based on hotbar slot.",
        "Use Same Color",
    )
    private val SETTING_FIXED_COLOR = addColorPicker(
        "fixedColor",
        Color(0, 0, 0).rgb,
        "Only used when 'Use Same Color' is enabled.",
        "Bound Slot Color",
    )

    private val bindingProfiles = object : PersistentJsonClass<MutableList<SlotBindingProfile>>(
        "devonian/slotbindingprofiles.json",
        object : TypeToken<MutableList<SlotBindingProfile>>() {}
    ) {
        override fun onLoadDefault() {
            data = mutableListOf(SlotBindingProfile("default"))
        }
    }
    private const val LEGACY_KEY_NAME = "slotsBound1"
    private const val KEY_NAME = "default"
    private val keybind = KeyBindingHelper.registerKeyBinding(
        KeyMapping(
            "key.devonian.slotBinding",
            GLFW.GLFW_KEY_UNKNOWN,
            Devonian.keybindCategory
        )
    )
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
    private var selectedProfile = "default"
    private val currentProfile
        get() = bindingProfiles.data!!.find { it.name.equals(selectedProfile, ignoreCase = true) }
    private val slotLocCache = arrayListOf<Pair<Int, Int>?>()
    private var currentHeldSlot: Slot? = null
    private var once = false

    data class SlotBindingProfile(
        val name: String,
        val slots: Array<MutableList<Int>> = Array(40) { mutableListOf<Int>() },
        val area: String? = null, // if null -> global
        var toggle: Boolean = true, // TODO: impl me ?
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as SlotBindingProfile

            if (name != other.name) return false
            if (!slots.contentEquals(other.slots)) return false
            if (area != other.area) return false

            return true
        }

        override fun hashCode(): Int {
            var result = name.hashCode()
            result = 31 * result + slots.contentHashCode()
            result = 31 * result + (area?.hashCode() ?: 0)
            return result
        }
    }

    override fun initialize() {
        bindingProfiles.load()
        Config.set(LEGACY_KEY_NAME, JsonArray())
        Config.set(KEY_NAME, "default")

        Config.onAfterLoad {
            selectedProfile = Config.get<String>(KEY_NAME) ?: "default"

            var defaultProfile = bindingProfiles.data!!.find { it.name == "default" }
            if (defaultProfile != null && defaultProfile.slots.isNotEmpty()) return@onAfterLoad

            val add = defaultProfile == null
            if (add) defaultProfile = SlotBindingProfile("default")

            Config.get<List<JsonPrimitive>>(LEGACY_KEY_NAME)?.map { it.asInt }?.forEach {
                val src = it shr 6
                val dst = it and 63
                defaultProfile.slots[src].add(dst)
            }
            bindingProfiles.data!!.add(defaultProfile)
        }

        Config.onPreSave {
            Config.set(KEY_NAME, selectedProfile)
        }

        DevonianCommand.command.subcommand("slotbinding") { _, args ->
            val modeArg = args.getOrNull(0) as? String?
            val arg1 = args.getOrNull(1) as? String?
            val arg2 = args.getOrNull(2) as? Boolean?

            if (modeArg.isNullOrEmpty()) {
                ChatUtils.sendMessage("&cSlotBinding did not set a valid mode", true)
                return@subcommand 0
            }

            when (modeArg.lowercase()) {
                "create" -> {
                    if (arg1.isNullOrEmpty()) {
                        ChatUtils.sendMessage("&cSlotBinding no profile name specified", true)
                        return@subcommand 0
                    }
                    if (arg2 == null) {
                        ChatUtils.sendMessage("&cSlotBinding no area specified &7(true = use current area, false = use global)", true)
                        return@subcommand 0
                    }
                    if (bindingProfiles.data!!.any { it.name.equals(arg1, ignoreCase = true) }) {
                        ChatUtils.sendMessage("&cSlotBinding profile with name &b\"$arg1\"&c already exists", true)
                        return@subcommand 0
                    }

                    val area = if (arg2) Location.area else null
                    bindingProfiles.data!!.add(SlotBindingProfile(arg1, area = area))
                    selectedProfile = arg1
                    ChatUtils.sendMessage("&aSlotBinding successfully created profile &b\"$arg1\"&a with area &b\"${area ?: "Global"}\" &7(this profile has been selected)", true)
                }
                "delete" -> {
                    if (arg1.isNullOrEmpty()) {
                        ChatUtils.sendMessage("&cSlotBinding no profile name specified", true)
                        return@subcommand 0
                    }

                    val removed = bindingProfiles.data!!.removeIf { it.name.equals(arg1, ignoreCase = true) }
                    if (!removed) {
                        ChatUtils.sendMessage("&cSlotBinding no profile with name &b\"$arg1\"&c was found", true)
                        return@subcommand 0
                    }

                    // TODO: maybe re-select depending on current area ?
                    if (arg1.equals(selectedProfile, true))
                        selectedProfile = "default"
                    ChatUtils.sendMessage("&aSlotBinding successfully deleted profile with name &b\"$arg1\"", true)
                }
                "profiles" -> {
                    ChatUtils.sendMessage("&aSlotBinding profiles &b${bindingProfiles.data!!.joinToString(",") { it.name }}", true)
                }
                "setprofile" -> {
                    if (arg1.isNullOrEmpty()) {
                        ChatUtils.sendMessage("&cSlotBinding no profile name specified", true)
                        return@subcommand 0
                    }

                    val exists = bindingProfiles.data!!.find { it.name.equals(arg1, ignoreCase = true) }
                    if (exists == null) {
                        ChatUtils.sendMessage("&cSlotBinding could not find profile with name &b\"$arg1\"", true)
                        return@subcommand 0
                    }

                    selectedProfile = exists.name
                    ChatUtils.sendMessage("&aSlotBinding set profile with name &b\"$selectedProfile\"", true)
                }
            }
            1
        }
            .word("mode")
            .suggest("mode", *listOf(
                "CREATE",
                "DELETE",
                "PROFILES",
                "SETPROFILE",
            ).toTypedArray())
            .word("name")
            .suggest("name", *bindingProfiles.data!!.map { it.name }.toTypedArray())
            .bool("useCurrentArea")

        on<AreaEvent> { event ->
            if (!SETTING_DYNAMIC_CHANGE.get()) return@on
            selectedProfile = bindingProfiles.data!!.find {
                it.area.equals(event.area, ignoreCase = true)
            }?.name ?: "default"
        }

        on<GuiKeyDownEvent> { event ->
            if (!keybind.matches(event.event)) return@on
            if (once) return@on
            once = true

            val screen = event.screen as? AbstractContainerScreenAccessor ?: return@on
            val slot = screen.hoveredSlot ?: return@on
            val boundSlots = currentProfile?.slots ?: return@on

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
            val boundSlots = currentProfile?.slots ?: return@on

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
            if (SETTING_PROTECT.get() && event.losesItem) {
                val boundSlots = currentProfile?.slots ?: return@on
                val slots = boundSlots.getOrNull(event.idx)
                if (!slots.isNullOrEmpty()) {
                    event.cancel("SlotBinding")
                    return@on
                }
            }

            (event.underlying as? QuickMoveItemEvent)?.also {
                val boundSlots = currentProfile?.slots ?: return@on
                val slots = boundSlots.getOrNull(event.idx) ?: return@on
                val other = slots.getOrNull(0) ?: return@on

                val player = minecraft.player ?: return@also
                val menu = player.containerMenu
                val id = menu.containerId

                setToFront(event.idx, other)

                if (event.isCancelled()) return@on

                if (event.idx < 9 && other >= 9) {
                    val inv = player.inventory
                    val dst = menu.slots.find { it.container === inv && it.containerSlot == other }!!
                    minecraft.gameMode?.handleInventoryMouseClick(id, dst.index, event.idx, ClickType.SWAP, player)
                } else minecraft.gameMode?.handleInventoryMouseClick(id, event.slot!!.index, other, ClickType.SWAP, player)
                event.cancel()
                return@on
            }

            if (SETTING_PROTECT.get()) {
                if (event.swapped != null) {
                    val boundSlots = currentProfile?.slots ?: return@on
                    val slots = boundSlots.getOrNull(event.idx) ?: return@on
                    if (slots.isNotEmpty() && !slots.contains(event.swapped.containerSlot)) event.cancel("SlotBinding")
                    return@on
                }

                val boundSlots = currentProfile?.slots ?: return@on
                val slots = boundSlots.getOrNull(event.idx) ?: return@on
                if (slots.isEmpty()) return@on

                event.cancel("SlotBinding")
            }
        }.prio = 1

        on<RenderSlotEvent> { event ->
            val slot = event.slot
            if (slot.container !== minecraft.player?.inventory) return@on

            val idx = slot.containerSlot
            val boundSlots = currentProfile?.slots ?: return@on
            val bound = boundSlots.getOrNull(idx) ?: return@on

            val hovered = (event.screen as AbstractContainerScreenAccessor).hoveredSlot
            val isHoveringBound =
                hovered != null &&
                hovered.container === minecraft.player?.inventory &&
                (boundSlots.getOrNull(hovered.containerSlot)?.size ?: 0) > 0

            while (slotLocCache.size <= idx) slotLocCache.add(null)
            slotLocCache[idx] = Pair(slot.x, slot.y)

            bound.forEachIndexed { i, other ->
                val c = colorFor(idx, other)
                if (SETTING_BOUND_LINES.get())
                    Render2D.drawWireRect(event.ctx, slot.x, slot.y, 16, 16, c, lw = 2)

                val loc = slotLocCache.getOrNull(other) ?: return@forEachIndexed
                if (isHoveringBound) {
                    if (hovered != slot || i > 0) return@forEachIndexed
                } else if (idx < other) return@forEachIndexed
                if (
                    SETTING_POINTING_LINE_MODE.get() == 0 ||
                    SETTING_POINTING_LINE_MODE.get() == 2 &&
                    !InputConstants.isKeyDown(minecraft.window!!, GLFW.GLFW_KEY_LEFT_SHIFT)
                ) return@forEachIndexed

                Render2D.drawLine(
                    event.ctx,
                    slot.x + 8f, slot.y + 8f,
                    loc.first + 8f, loc.second + 8f,
                    c,
                )
            }
        }.prio = 20

        on<PostRenderSlotsEvent> { event ->
            val curr = currentHeldSlot ?: return@on
            val cont = event.container as AbstractContainerScreenAccessor
            val inv = minecraft.player?.inventory
            val boundSlots = currentProfile?.slots ?: return@on

            event.container.menu.slots.forEach { slot ->
                if (curr === slot) return@forEach
                if (
                    slot.container === inv &&
                    slot.containerSlot != 40 &&
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
        }.prio = 2
    }

    private fun colorFor(idx: Int, other: Int): Color {
        if (SETTING_SAME_COLOR.get()) return SETTING_FIXED_COLOR.getColor()
        return slotColors.getOrElse(idx) { slotColors[other] }
    }

    fun setToFront(idx1: Int, idx2: Int) {
        val boundSlots = currentProfile?.slots ?: return
        val i1 = boundSlots[idx1].indexOf(idx2)
        val i2 = boundSlots[idx2].indexOf(idx1)
        if (i1 > 0) boundSlots[idx1][0] = boundSlots[idx1][i1].also { boundSlots[idx1][i1] = boundSlots[idx1][0] }
        if (i2 > 0) boundSlots[idx2][0] = boundSlots[idx2][i2].also { boundSlots[idx2][i2] = boundSlots[idx2][0] }
    }

    fun compatIsRendering(slot: Slot): Boolean {
        if (!isEnabled()) return false
        if (slot.container != minecraft.player?.inventory) return false
        return currentProfile?.slots?.getOrNull(slot.containerSlot)?.isNotEmpty() ?: false
    }
}