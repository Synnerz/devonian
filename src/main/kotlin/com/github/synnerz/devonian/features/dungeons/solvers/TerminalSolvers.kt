package com.github.synnerz.devonian.features.dungeons.solvers

import com.github.synnerz.devonian.api.ScreenUtils
import com.github.synnerz.devonian.api.dungeon.Stages
import com.github.synnerz.devonian.api.events.*
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.features.dungeons.solvers.TerminalSolvers.SETTING_BACKGROUND_SLOT
import com.github.synnerz.devonian.features.dungeons.solvers.TerminalSolvers.SETTING_BACKGROUND_TERMINAL_COLOR
import com.github.synnerz.devonian.features.dungeons.solvers.TerminalSolvers.SETTING_CUSTOM_GUI
import com.github.synnerz.devonian.features.dungeons.solvers.TerminalSolvers.SETTING_CUSTOM_GUI_SCALE
import com.github.synnerz.devonian.features.dungeons.solvers.TerminalSolvers.SETTING_HIDE_DONE
import com.github.synnerz.devonian.features.dungeons.solvers.TerminalSolvers.SETTING_HIDE_ITEMS
import com.github.synnerz.devonian.features.dungeons.solvers.TerminalSolvers.SETTING_RED_GREEN_DISABLE_RENDER
import com.github.synnerz.devonian.features.dungeons.solvers.TerminalSolvers.SETTING_RED_GREEN_PREVENT_RECLICK
import com.github.synnerz.devonian.features.dungeons.solvers.TerminalSolvers.SETTING_RENDER_NUMBERS
import com.github.synnerz.devonian.features.dungeons.solvers.TerminalSolvers.SETTING_RUBIX_BLOCK_SUBOPTIMAL
import com.github.synnerz.devonian.features.dungeons.solvers.TerminalSolvers.SETTING_RUBIX_FORCE_POSITIVE
import com.github.synnerz.devonian.features.dungeons.solvers.TerminalSolvers.color
import com.github.synnerz.devonian.features.dungeons.solvers.TerminalSolvers.minecraft
import com.github.synnerz.devonian.utils.BasicState
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.DyeColor
import net.minecraft.world.item.Items
import java.awt.Color
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.min

// Credits to <https://github.com/UnclaimedBloom6/BloomModule/blob/main/features/TerminalSolvers.js>
// this fk noob
object TerminalSolvers : Feature(
    "terminalSolvers",
    "Shows the correct slots to click to solve the current terminal.",
    Categories.F7,
    "catacombs",
    subcategory = "Terminals",
) {
    override fun createRequirements(): List<BasicState<Boolean>?> {
        return super.createRequirements() + listOf(Stages.Terminals.isActiveState)
    }

    private val SETTING_MIDDLE_CLICK = addSwitch(
        "middleClick",
        true,
        "change left clicks into middle clicks",
        "Terminal Middle Click",
    )
    private val SETTING_DISABLE_TOOLTIP = addSwitch(
        "disableTooltip",
        true,
        "Disables the tooltip whenever inside a terminal.",
        "Disable Terminal Tooltip",
    )
    private val SETTING_CORRECT_COLOR = addColorPicker(
        "correctColor",
        0xFF4F8F2F.toInt(),
        "The correct color for terminal solver.",
        "Terminal Solver Correct Color",
    )
    private val SETTING_SECOND_CORRECT_COLOR = addColorPicker(
        "secondCorrectColor",
        0xFFB5A12E.toInt(),
        "The second correct color for terminal solver.",
        "Terminal Solver Second Correct Color",
    )
    private val SETTING_THIRD_CORRECT_COLOR = addColorPicker(
        "thirdCorrectColor",
        0xFFB86A2E.toInt(),
        "The third correct color for terminal solver.",
        "Terminal Solver Third Correct Color",
    )
    private val SETTING_OTHER_CORRECT_COLOR = addColorPicker(
        "otherColor",
        0xFF8F2E2E.toInt(),
        "The other color for terminal solver.",
        "Terminal Solver Other Color",
    )
    val SETTING_BACKGROUND_SLOT = addSwitch(
        "bgTerminal",
        false,
        "Makes it so every slot rendered on the Terminal gui will have a custom color.",
        "Terminal Slot Background"
    )
    val SETTING_BACKGROUND_TERMINAL_COLOR = addColorPicker(
        "bgTerminalColor",
        Color(25, 25, 25, 255).rgb,
        "The color which will be used to draw on all the slots",
        "Terminal Slot Background Color"
    )
    private val SETTING_CANCEL_WRONG_CLICKS = addSwitch(
        "cancelWrongClicks",
        true,
        "Cancels the wrong clicks inside of terminals.",
        "Terminal Solver Cancel Clicks",
    )
    private val SETTING_CANCEL_NONCLICKS = addSwitch(
        "cancelNonClicks",
        false,
        "Cancels hotkey swapping and pickup all behavior.",
        "Terminal Solver Cancel Non-Click Actions",
    )
    val SETTING_HIDE_DONE = addSwitch(
        "hideDone",
        true,
        "Hides the already clicked slot items or the ones that are not a solution.",
        "Terminal Solver Hide Complete",
    )
    val SETTING_HIDE_ITEMS = addSwitch(
        "hideItems",
        true,
        "Hides all items in the 'Starts With'/'Select All' terminals.",
        "Terminal Solver Hide Items",
    )
    val SETTING_RED_GREEN_DISABLE_RENDER = addSwitch(
        "redGreen",
        false,
        "Toggle to specifically disable custom renderer for red/green solver.",
        "Correct All Terminal Vanilla Renderer",
    )
    val SETTING_RED_GREEN_PREVENT_RECLICK = addSlider(
        "redGreenPreventReclick",
        0.0,
        0.0, 1000.0,
        "After clicking a pane in the red/green terminal, prevents you from reclicking that " +
        "pane for this amount of time. It does not account for whether your initial click went through or not, " +
        "so please do not turn this on if you are laggy.",
        "Red Green Prevent Reclick",
    )

    // make render name for numbers
    val SETTING_RENDER_NUMBERS = addSwitch(
        "renderNumbers",
        true,
        "Whether to render the numbers from Numbers terminal in the slots or not.",
        "Terminal Solver Render Numbers",
    )
    val SETTING_RUBIX_FORCE_POSITIVE = addSwitch(
        "rubixPositive",
        false,
        "Effectively always shows the clicks required as positive, doesn't affect selecting fastest solution.",
        "Rubix Show Left Click Count",
    )
    val SETTING_RUBIX_BLOCK_SUBOPTIMAL = addSwitch(
        "rubixBlockBad",
        false,
        "Prevents the wrong type of mouse click, " +
        "i.e. right clicking on an item that needs 2 or less left clicks, and vv.",
        "Rubix Block Bad Clicks",
    )

    val SETTING_CUSTOM_GUI = addSwitch(
        "customGui",
        false,
        "Replaces the vanilla chest gui with a custom one.",
        "Custom Terminal Gui",
    )
    val SETTING_CUSTOM_GUI_SCALE = addDecimalSlider(
        "customGuiScale",
        1.0,
        0.01, 5.0,
        "",
        "Custom Terminal Gui Scale",
    )

    private var currentSolver: TerminalData? = null

    private val PREVENTED_SOUND = SoundEvents.NOTE_BLOCK_BASS

    data class InterimRubixSlot(val idx: Int, val color: Int, val clicks: Int = 0)
    data class RedGreenSlot(val correct: Boolean, var clickCd: Long = 0L)
    data class Cell(val x: Int, val y: Int, val w: Int, val h: Int)

    private fun onInteractSlot(slot: Slot, event: CancellableEvent, lc: Boolean): Boolean {
        return if (SETTING_CANCEL_WRONG_CLICKS.get() && currentSolver?.cancelClick(slot, lc) == true) {
            event.cancel()
            minecraft.level?.playPlayerSound(
                PREVENTED_SOUND.value(),
                SoundSource.MASTER,
                1f, 0.5f,
            )
            true
        } else false
    }

    override fun initialize() {
        on<GuiOpenEvent> { event ->
            val title = event.screen.title.string
            currentSolver = TerminalData.byMatch(title)
        }

        on<GuiCloseEvent> {
            currentSolver?.reset()
            currentSolver = null
        }

        on<TooltipRenderEvent> { event ->
            if (currentSolver != null) event.cancel()
        }.setEnabled(SETTING_DISABLE_TOOLTIP.state)

        on<TickEvent> {
            currentSolver?.onTick()
        }

        on<RenderGuiEvent> { event ->
            val solver = currentSolver ?: return@on
            if (!solver.useCustomGui()) return@on

            event.cancel()

            if (SETTING_BACKGROUND_SLOT.get()) solver.onRenderBackground(event.ctx)

            val gui = (event.screen as? AbstractContainerScreen<*>) ?: return@on
            gui.menu.slots.forEach { slot ->
                val cell = solver.getLocation(slot) ?: return@forEach
                solver.onRenderSlot(event.ctx, slot, cell) {}
                if (!solver.doOnAfterRender()) return@forEach
                solver.onAfterRender(event.ctx, slot, cell)
            }
        }.setEnabled(SETTING_CUSTOM_GUI.state)

        on<RenderSlotEvent> { event ->
            val solver = currentSolver ?: return@on
            val cell = solver.getLocation(event.slot) ?: return@on
            solver.onRenderSlot(event.ctx, event.slot, cell) { event.cancel() }
        }

        on<PostRenderSlotsEvent> { event ->
            val solver = currentSolver ?: return@on
            if (!solver.doOnAfterRender()) return@on

            event.container.menu.slots.forEach { slot ->
                val cell = solver.getLocation(slot) ?: return@forEach
                solver.onAfterRender(event.ctx, slot, cell)
            }
        }

        on<DropItemEvent> { event ->
            if (currentSolver == null) return@on
            val slot = event.slot ?: return@on
            onInteractSlot(slot, event, true)
        }

        on<PickupItemInventoryEvent> { event ->
            if (currentSolver == null) return@on

            if (event.isAll && SETTING_CANCEL_NONCLICKS.get()) {
                event.cancel()
                return@on
            }

            if (onInteractSlot(event.slot, event, !event.isSplitItem)) return@on
            if (SETTING_MIDDLE_CLICK.get() && currentSolver != TerminalData.RUBIX) {
                event.cancel()
                ScreenUtils.click(event.slot.index, false, "MIDDLE")
            }
        }

        on<SwapItemEvent> { event ->
            if (currentSolver == null) return@on
            event.cancel()
        }.setEnabled(SETTING_CANCEL_NONCLICKS.state)

        on<GuiClickEvent> { event ->
            val solver = currentSolver ?: return@on
            if (!solver.useCustomGui()) return@on
            val gui = event.screen as? AbstractContainerScreen<*> ?: return@on

            event.cancel()
            if (!event.state) return@on
            val idx = solver.getSlotCustom(event.mx.toInt(), event.my.toInt()) ?: return@on
            if (idx !in gui.menu.slots.indices) return@on
            val click = when (event.mbtn) {
                1 -> "RIGHT"
                2 -> "MIDDLE"
                else -> "LEFT"
            }

            val slot = gui.menu.getSlot(idx)
            if (solver.cancelClick(slot, click == "LEFT")) return@on

            ScreenUtils.click(idx, false, click)
        }.setEnabled(SETTING_CUSTOM_GUI.state)
    }

    fun color(idx: Int): Int = when (idx) {
        0 -> SETTING_CORRECT_COLOR.get()
        1 -> SETTING_SECOND_CORRECT_COLOR.get()
        2 -> SETTING_THIRD_CORRECT_COLOR.get()
        else -> SETTING_OTHER_CORRECT_COLOR.get()
    }
}

interface ITerminalSolver {
    val changesWindow: Boolean

    fun getSlotsBox(): TerminalSolvers.Cell

    fun reset()

    fun onTick()

    fun onRenderSlot(ctx: GuiGraphicsExtractor, slot: Slot, cell: TerminalSolvers.Cell, cancel: () -> Unit) {}

    fun doOnAfterRender() = false
    fun onAfterRender(ctx: GuiGraphicsExtractor, slot: Slot, cell: TerminalSolvers.Cell) {}


    fun cancelClick(slot: Slot, lc: Boolean): Boolean = cancelClick(slot)
    fun cancelClick(slot: Slot): Boolean = false

    fun useCustomGui() = SETTING_CUSTOM_GUI.get()

    fun onRenderBackground(ctx: GuiGraphicsExtractor) {
        val bounds = getLocation(getSlotsBox())
        val color = SETTING_BACKGROUND_TERMINAL_COLOR.getColor()
        if (color.alpha == 0) return

        val s = getSlotSize()
        ctx.fill(
            bounds.x - (s * 0.4).toInt(),
            bounds.y - (s * 0.6).toInt(),
            bounds.x + bounds.w + (s * 0.8).toInt(),
            bounds.y + bounds.h + (s * 1.2).toInt(),
            color.rgb,
        )
    }

    fun renderSlotBackground(ctx: GuiGraphicsExtractor, cell: TerminalSolvers.Cell) {
        if (!SETTING_BACKGROUND_SLOT.get()) return
        if (useCustomGui()) return
        if (SETTING_BACKGROUND_TERMINAL_COLOR.getColor().alpha == 0) return
        ctx.fill(cell.x - 2, cell.y - 2, cell.x + cell.w + 2, cell.y + cell.h + 2, SETTING_BACKGROUND_TERMINAL_COLOR.get())
    }

    fun renderSlot(ctx: GuiGraphicsExtractor, cell: TerminalSolvers.Cell, idx: Int) {
        ctx.fill(cell.x, cell.y, cell.x + cell.w, cell.y + cell.h, color(idx))
    }

    private fun getSlotSize() = ceil(24.0 * SETTING_CUSTOM_GUI_SCALE.get()).toInt()
    private fun getSlotPadding() = ceil(1.5 * SETTING_CUSTOM_GUI_SCALE.get()).toInt()

    fun getLocation(slot: Slot): TerminalSolvers.Cell? {
        if (slot.container == minecraft.player?.inventory) return null
        if (!useCustomGui()) return TerminalSolvers.Cell(slot.x, slot.y, 16, 16)

        val x = slot.containerSlot % 9
        val y = slot.containerSlot / 9

        val cell = TerminalSolvers.Cell(x, y, 1, 1)
        return getLocation(cell)
    }

    fun getLocation(cell: TerminalSolvers.Cell): TerminalSolvers.Cell {
        val window = minecraft.window
        val box = getSlotsBox()

        val cx = box.x + box.w / 2.0
        val cy = box.y + box.h / 2.0 + 1.0

        val wx = window.guiScaledWidth / 2
        val wy = window.guiScaledHeight / 2

        val size = getSlotSize()
        val padding = getSlotPadding()
        val slotBounds = size + padding * 2

        return TerminalSolvers.Cell(
            ((cell.x - cx) * slotBounds).toInt() + wx + padding,
            ((cell.y - cy) * slotBounds).toInt() + wy + padding,
            slotBounds * (cell.w - 1) + size,
            slotBounds * (cell.h - 1) + size,
        )
    }

    fun getSlotCustom(mouseX: Int, mouseY: Int): Int? {
        val window = minecraft.window
        val box = getSlotsBox()

        val cx = box.x + box.w / 2.0
        val cy = box.y + box.h / 2.0 + 1.0

        val wx = window.guiScaledWidth / 2
        val wy = window.guiScaledHeight / 2

        val size = getSlotSize()
        val padding = getSlotPadding()
        val slotBounds = size + padding * 2

        val x = ((mouseX - wx) / slotBounds.toDouble() + cx).toInt()
        val y = ((mouseY - wy) / slotBounds.toDouble() + cy).toInt()

        if (x !in 0 .. 8) return null
        if (y !in 0 .. 5) return null

        return y * 9 + x
    }
}

enum class TerminalData(val title: Regex) : ITerminalSolver {
    NUMBERS("Click in order!".toRegex()) {
        override val changesWindow: Boolean = true
        override fun getSlotsBox(): TerminalSolvers.Cell = TerminalSolvers.Cell(1, 1, 7, 2)

        private var slots = emptyArray<Int>()
        private var initSlots: Array<Int>? = null
        private var minCount = 14

        override fun reset() {
            slots = emptyArray()
            initSlots = null
        }

        override fun onTick() {
            val screen = minecraft.gui.screen() ?: return
            val items = (screen as AbstractContainerScreen<*>).menu.items
            if (initSlots == null && items.size > 45 && !(items.getOrNull(items.size - 45)?.isEmpty ?: true)) {
                initSlots = Array(items.size) { idx ->
                    val stack = items[idx]
                    val count =
                        if (stack.item == Items.STAINED_GLASS_PANE.red)
                            stack.count
                        else
                            0
                    return@Array count
                }
            }

            minCount = 14
            slots = Array(items.size) { idx ->
                val _cached = initSlots?.getOrNull(idx)
                val stack = items[idx]
                val count =
                    if (stack.item == Items.STAINED_GLASS_PANE.red)
                        stack.count
                    else
                        0
                val fixedCount =
                    if (
                        _cached != null &&
                        (stack.item == Items.AIR || count != 0 && count != _cached)
                    )
                        _cached
                    else
                        count
                if (fixedCount > 0) minCount = min(minCount, fixedCount)
                return@Array fixedCount
            }
        }

        override fun onRenderSlot(
            ctx: GuiGraphicsExtractor,
            slot: Slot,
            cell: TerminalSolvers.Cell,
            cancel: () -> Unit
        ) {
            val count = slots.getOrElse(slot.containerSlot) { 0 }
            if (count == 0) {
                if (SETTING_HIDE_DONE.get()) {
                    renderSlotBackground(ctx, cell)
                    cancel()
                }
                return
            }

            renderSlotBackground(ctx, cell)
            renderSlot(ctx, cell, count - minCount)
            if (SETTING_RENDER_NUMBERS.get()) {
                ctx.centeredText(
                    minecraft.font,
                    "$count",
                    cell.x + cell.w / 2, cell.y + cell.h / 4, -1
                )
            }

            cancel()
        }

        override fun cancelClick(slot: Slot): Boolean {
            return slots.getOrElse(slot.containerSlot) { 0 } == 0
        }
    },
    COLORS("^Select all the (.*?) items!$".toRegex()) {
        override val changesWindow: Boolean = true
        override fun getSlotsBox(): TerminalSolvers.Cell = TerminalSolvers.Cell(1, 1, 7, 4)

        private var slots = emptyArray<Boolean>()
        private val fixedColorItems = mapOf(
            "light gray" to "silver",
            "wool" to "white",
            "bone" to "white",
            "ink" to "black",
            "lapis" to "blue",
            "cocoa" to "brown",
            "dandelion" to "yellow",
            "rose" to "red",
            "cactus" to "green",
        )

        override fun reset() {
            slots = emptyArray()
        }

        override fun onTick() {
            val screen = minecraft.gui.screen() ?: return
            val toFind = title.matchEntire(screen.title.string)?.groupValues?.drop(1)?.getOrNull(0) ?: return
            val items = (screen as AbstractContainerScreen<*>).menu.items

            slots = Array(items.size) { idx ->
                val stack = items[idx]
                if (stack.get(DataComponents.ENCHANTMENT_GLINT_OVERRIDE) == true) return@Array false

                var name = stack.customName?.string ?: stack.itemName.string
                for (fixed in fixedColorItems) {
                    if (name.startsWith(fixed.key, ignoreCase = true))
                        name = fixed.value
                }

                return@Array name.startsWith(toFind, ignoreCase = true)
            }
        }

        override fun onRenderSlot(
            ctx: GuiGraphicsExtractor,
            slot: Slot,
            cell: TerminalSolvers.Cell,
            cancel: () -> Unit
        ) {
            if (!slots.getOrElse(slot.containerSlot) { false }) {
                if (SETTING_HIDE_DONE.get()) {
                    renderSlotBackground(ctx, cell)
                    cancel()
                }
                return
            }

            renderSlotBackground(ctx, cell)
            renderSlot(ctx, cell, 0)
            if (SETTING_HIDE_ITEMS.get()) cancel()
        }

        override fun cancelClick(slot: Slot): Boolean {
            return !slots.getOrElse(slot.containerSlot) { false }
        }
    },
    STARTS_WITH("^What starts with: '(.*?)'\\?$".toRegex()) {
        override val changesWindow: Boolean = true
        override fun getSlotsBox(): TerminalSolvers.Cell = TerminalSolvers.Cell(1, 1, 7, 3)

        private var slots = emptyArray<Boolean>()

        override fun reset() {
            slots = emptyArray()
        }

        override fun onTick() {
            val screen = minecraft.gui.screen() ?: return
            val toFind = title.matchEntire(screen.title.string)?.groupValues?.drop(1)?.getOrNull(0) ?: return
            val items = (screen as AbstractContainerScreen<*>).menu.items

            slots = Array(items.size) { idx ->
                val stack = items[idx]
                if (stack.get(DataComponents.ENCHANTMENT_GLINT_OVERRIDE) == true) return@Array false

                var name = stack.customName?.string ?: stack.itemName.string

                return@Array name.startsWith(toFind, ignoreCase = true)
            }
        }

        override fun onRenderSlot(
            ctx: GuiGraphicsExtractor,
            slot: Slot,
            cell: TerminalSolvers.Cell,
            cancel: () -> Unit
        ) {
            if (!slots.getOrElse(slot.containerSlot) { false }) {
                if (SETTING_HIDE_DONE.get()) {
                    renderSlotBackground(ctx, cell)
                    cancel()
                }
                return
            }

            renderSlotBackground(ctx, cell)
            renderSlot(ctx, cell, 0)
            if (SETTING_HIDE_ITEMS.get()) cancel()
        }

        override fun cancelClick(slot: Slot): Boolean {
            return !slots.getOrElse(slot.containerSlot) { false }
        }
    },
    RUBIX("^Change all to same color!$".toRegex()) {
        override val changesWindow: Boolean = true
        override fun getSlotsBox(): TerminalSolvers.Cell = TerminalSolvers.Cell(3, 1, 3, 3)

        private val rubixIndices = listOf(12, 13, 14, 21, 22, 23, 30, 31, 32)
        private val isRubix = Array(45) { false }.also {
            rubixIndices.forEach { i -> it[i] = true }
        }
        // left click = ++, right click = --
        private val rubixOrder = listOf(
            Items.STAINED_GLASS_PANE.orange,
            Items.STAINED_GLASS_PANE.yellow,
            Items.STAINED_GLASS_PANE.green,
            Items.STAINED_GLASS_PANE.blue,
            Items.STAINED_GLASS_PANE.red,
        )
        private val rubixColors = arrayOf(
            DyeColor.ORANGE.textureDiffuseColor,
            DyeColor.YELLOW.textureDiffuseColor,
            DyeColor.GREEN.textureDiffuseColor,
            DyeColor.BLUE.textureDiffuseColor,
            DyeColor.RED.textureDiffuseColor,
        )
        private val strings = arrayOf(
            Component.literal("§e-2"),
            Component.literal("§a-1"),
            null,
            Component.literal("§a1"),
            Component.literal("§e2"),
            Component.literal("§e3"),
            Component.literal("§c4"),
        )

        private var slots = emptyArray<Int>()
        private var customSlotColors = IntArray(45) { -1 }
        private var lastClicked = -1
        private var lastClickType = false

        override fun reset() {
            slots = emptyArray()
            customSlotColors.fill(-1)
            lastClicked = -1
            lastClickType = false
        }

        override fun onTick() {
            val screen = minecraft.gui.screen() ?: return
            val items = (screen as AbstractContainerScreen<*>).menu.items
            val slotsIn = mutableListOf<TerminalSolvers.InterimRubixSlot>()

            val held = screen.menu.carried

            rubixIndices.forEach { idx ->
                var item = items.getOrNull(idx)
                if (item?.isEmpty != false) {
                    if (idx == lastClicked) item = held
                    if (item?.isEmpty != false) return@forEach
                }

                var color = rubixOrder.indexOf(item.item)
                if (color < 0) return@forEach

                if (item === held) {
                    if (lastClickType) color++
                    else color--

                    if (color < 0) color += rubixOrder.size
                    if (color >= rubixOrder.size) color -= rubixOrder.size
                }

                slotsIn.add(TerminalSolvers.InterimRubixSlot(idx, color))
            }

            var best = 19
            for (target in rubixOrder.indices) {
                var clicks = 0
                val needClicks = slotsIn.filter {
                    var dist = abs(target - it.color)
                    if (dist >= 3) dist = 5 - dist
                    clicks += dist
                    dist > 0
                }

                if (clicks < best) {
                    best = clicks
                    slots = Array(items.size) { idx ->
                        val tmp = needClicks.find { it.idx == idx } ?: return@Array 0
                        var lc = target - tmp.color
                        if (lc < 0) lc += 5
                        return@Array lc
                    }
                }
            }

            customSlotColors.fill(-1)
            slotsIn.forEach { s ->
                customSlotColors[s.idx] = s.color
            }
        }

        override fun onRenderSlot(
            ctx: GuiGraphicsExtractor,
            slot: Slot,
            cell: TerminalSolvers.Cell,
            cancel: () -> Unit
        ) {
            if (!SETTING_HIDE_DONE.get() && !isRubix.getOrElse(slot.containerSlot) { false }) {
                renderSlotBackground(ctx, cell)
            }

            if (!useCustomGui()) return
            val idx = customSlotColors.getOrNull(slot.containerSlot) ?: return
            if (idx == -1) return

            val color = rubixColors.getOrNull(idx) ?: return

            ctx.fill(cell.x, cell.y, cell.x + cell.w, cell.y + cell.h, color)
        }

        override fun doOnAfterRender(): Boolean = true

        override fun onAfterRender(ctx: GuiGraphicsExtractor, slot: Slot, cell: TerminalSolvers.Cell) {
            var clicks = slots.getOrElse(slot.containerSlot) { 0 }
            if (clicks == 0) return

            if (!SETTING_RUBIX_FORCE_POSITIVE.get() && clicks >= 3) clicks -= 5
            val str = strings.getOrNull(clicks + 2) ?: return

            ctx.centeredText(minecraft.font, str, cell.x + cell.w / 2, cell.y + cell.h / 4, -1)
        }

        override fun cancelClick(slot: Slot, lc: Boolean): Boolean {
            val clicks = slots.getOrElse(slot.containerSlot) { 0 }
            if (clicks == 0) return true
            lastClicked = slot.containerSlot
            lastClickType = lc
            return SETTING_RUBIX_BLOCK_SUBOPTIMAL.get() && clicks > 2 == lc
        }
    },
    RED_GREEN("^Correct all the panes!$".toRegex()) {
        override val changesWindow: Boolean = false
        override fun getSlotsBox(): TerminalSolvers.Cell = TerminalSolvers.Cell(2, 1, 5, 3)

        private var slots = emptyArray<TerminalSolvers.RedGreenSlot>()

        override fun reset() {
            slots = emptyArray()
        }

        override fun onTick() {
            val screen = minecraft.gui.screen() ?: return
            val items = (screen as AbstractContainerScreen<*>).menu.items
            val time = System.currentTimeMillis()

            slots = Array(items.size) { idx ->
                val cd = slots.getOrNull(idx)?.clickCd ?: 0L
                TerminalSolvers.RedGreenSlot(
                    cd <= time && items[idx].item == Items.STAINED_GLASS_PANE.red,
                    cd,
                )
            }
        }

        override fun useCustomGui(): Boolean = super.useCustomGui() && !SETTING_RED_GREEN_DISABLE_RENDER.get()

        override fun onRenderSlot(
            ctx: GuiGraphicsExtractor,
            slot: Slot,
            cell: TerminalSolvers.Cell,
            cancel: () -> Unit
        ) {
            if (SETTING_RED_GREEN_DISABLE_RENDER.get()) return

            val data = slots.getOrNull(slot.containerSlot)
            if (data == null || !data.correct) {
                if (SETTING_HIDE_DONE.get()) {
                    renderSlotBackground(ctx, cell)
                    cancel()
                }
                return
            }

            renderSlotBackground(ctx, cell)
            renderSlot(ctx, cell, 0)
            cancel()
        }

        override fun cancelClick(slot: Slot): Boolean {
            val data = slots.getOrNull(slot.containerSlot) ?: return true
            if (!data.correct) return true

            data.clickCd = System.currentTimeMillis() + SETTING_RED_GREEN_PREVENT_RECLICK.get().toInt()
            return false
        }
    },
    MELODY("^Click the button on time!$".toRegex()) {
        override val changesWindow: Boolean = false
        override fun getSlotsBox(): TerminalSolvers.Cell = TerminalSolvers.Cell(0, 0, 9, 6)

        override fun reset() {}

        override fun onTick() {}

        override fun useCustomGui(): Boolean = false
    };

    companion object {
        fun byMatch(string: String) = TerminalData.entries.find { it.title.matches(string) }
    }
}