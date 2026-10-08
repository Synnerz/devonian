package com.github.synnerz.devonian.features.dungeons.solvers

import com.github.synnerz.devonian.GuiTextRenderStateAccessor
import com.github.synnerz.devonian.api.Scheduler
import com.github.synnerz.devonian.api.ScreenUtils
import com.github.synnerz.devonian.api.dungeon.Stages
import com.github.synnerz.devonian.api.events.*
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.features.dungeons.solvers.TerminalSolvers.SETTING_BACKGROUND_SLOT
import com.github.synnerz.devonian.features.dungeons.solvers.TerminalSolvers.SETTING_BACKGROUND_TERMINAL_COLOR
import com.github.synnerz.devonian.features.dungeons.solvers.TerminalSolvers.SETTING_CUSTOM_GUI
import com.github.synnerz.devonian.features.dungeons.solvers.TerminalSolvers.SETTING_CUSTOM_GUI_OUTLINE_LEFT
import com.github.synnerz.devonian.features.dungeons.solvers.TerminalSolvers.SETTING_CUSTOM_GUI_OUTLINE_RIGHT
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
import com.github.synnerz.devonian.utils.StringUtils.replaceCodes
import com.github.synnerz.devonian.utils.math.Rectangle
import com.github.synnerz.devonian.utils.render.Render2D
import com.github.synnerz.devonian.utils.render.states.QuadRenderState
import com.github.synnerz.talium.utils.state.GradientRectangleState
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.client.renderer.state.gui.GuiTextRenderState
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.DyeColor
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import org.joml.Matrix3x2f
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
    val SETTING_CUSTOM_GUI_OUTLINE_LEFT = addColorPicker(
        "customGuiOutlineLeft",
        Color(0, 255, 255, 255).rgb,
        "",
        "Custom Terminal Gui Outline Left",
    )
    val SETTING_CUSTOM_GUI_OUTLINE_RIGHT = addColorPicker(
        "customGuiOutlineRight",
        Color(0, 255, 255, 255).rgb,
        "",
        "Custom Terminal Gui Outline Right",
    )

    private var currentSolver: TerminalData? = null

    private val PREVENTED_SOUND = SoundEvents.NOTE_BLOCK_BASS
    private val DROP_KEYBIND by lazy { minecraft.options.keyDrop }

    private fun onInteractSlot(solver: TerminalData, slot: Slot, event: CancellableEvent?, btn: Int): Boolean {
        if (slot.container == minecraft.player?.inventory) return false

        if (SETTING_CANCEL_WRONG_CLICKS.get() && solver.cancelClick(slot, btn)) {
            event?.cancel()
            minecraft.level?.playPlayerSound(
                PREVENTED_SOUND.value(),
                SoundSource.MASTER,
                1f, 0.5f,
            )
            return true
        } else {
            solver.onClickSlot(slot, btn)
            return false
        }
    }

    override fun initialize() {
        // TODO: this is only for testing
        on<GuiOpenEvent> {
            Scheduler.scheduleTask(2) {
                val solver = currentSolver ?: return@scheduleTask
                val screen = minecraft.gui.screen() as? AbstractContainerScreen<*> ?: return@scheduleTask
                screen.menu.items.forEachIndexed { idx, stack -> solver.onSetSlot(idx, stack) }
            }
        }

        on<ServerContainerOpenEvent> { event ->
            val title = event.titleStr
            currentSolver = TerminalData.byMatch(title)
        }

        on<GuiCloseEvent> {
            currentSolver?.reset()
            currentSolver = null
        }

        on<TickEvent> {
            val solver = currentSolver ?: return@on
            solver.onTick()
        }

        on<ServerContainerSetSlotEvent> { event ->
            val solver = currentSolver ?: return@on
            Scheduler.scheduleTask {
                val idx = event.slot
                solver.onSetSlot(idx, event.itemStack)
            }
        }

        on<RenderGuiEvent> { event ->
            val solver = currentSolver ?: return@on
            if (!solver.useCustomGui()) return@on

            event.cancel()

            if (SETTING_BACKGROUND_SLOT.get()) solver.onRenderBackground(event.ctx)

            val gui = (event.screen as? AbstractContainerScreen<*>) ?: return@on
            gui.menu.slots.forEach { slot ->
                val cell = solver.getCustomLocation(slot) ?: return@forEach
                solver.onRenderSlot(event.ctx, slot, cell) {}
                if (!solver.doOnAfterRender()) return@forEach
                solver.onAfterRender(event.ctx, slot, cell)
            }
        }.setEnabled(SETTING_CUSTOM_GUI.state)

        on<RenderSlotEvent> { event ->
            val solver = currentSolver ?: return@on
            val cell = solver.getCustomLocation(event.slot) ?: return@on
            solver.onRenderSlot(event.ctx, event.slot, cell) { event.cancel() }
        }

        on<PostRenderSlotsEvent> { event ->
            val solver = currentSolver ?: return@on
            if (!solver.doOnAfterRender()) return@on

            event.container.menu.slots.forEach { slot ->
                val cell = solver.getCustomLocation(slot) ?: return@forEach
                solver.onAfterRender(event.ctx, slot, cell)
            }
        }

        on<DropItemEvent> { event ->
            val solver = currentSolver ?: return@on
            if (solver.useCustomGui()) {
                event.cancel()
                return@on
            }

            val slot = event.slot ?: return@on
            if (onInteractSlot(solver, slot, event, 0)) return@on
        }

        on<PickupItemInventoryEvent> { event ->
            val solver = currentSolver ?: return@on

            if (event.isAll && SETTING_CANCEL_NONCLICKS.get()) {
                event.cancel()
                return@on
            }

            val btn = if (event.isSplitItem) 1 else 0
            if (onInteractSlot(solver, event.slot, event, btn)) return@on

            if (SETTING_MIDDLE_CLICK.get() && currentSolver != TerminalData.RUBIX) {
                event.cancel()
                ScreenUtils.click(event.slot.index, false, "MIDDLE")
            }
        }

        on<MiddleClickItemEvent> { event ->
            val solver = currentSolver ?: return@on

            if (onInteractSlot(solver, event.slot, event, 2)) return@on
        }

        on<TooltipRenderEvent> { event ->
            if (currentSolver != null) event.cancel()
        }.setEnabled(SETTING_DISABLE_TOOLTIP.state)

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
                3 -> "RIGHT"
                2 -> "MIDDLE"
                else -> "LEFT" // 1
            }
            // ONLY WORKS IN >26.3
            val legacyBtn = when (event.mbtn) {
                1 -> 0
                3 -> 1
                else -> event.mbtn
            }

            val slot = gui.menu.getSlot(idx)
            if (onInteractSlot(solver, slot, null, legacyBtn)) return@on

            ScreenUtils.click(idx, false, click)
        }.setEnabled(SETTING_CUSTOM_GUI.state)

        on<GuiKeyDownEvent> { event ->
            val solver = currentSolver ?: return@on
            if (!solver.useCustomGui()) return@on
            if (!DROP_KEYBIND.matches(event.event)) return@on

            event.cancel()

            val gui = minecraft.gui.screen() as? AbstractContainerScreen<*> ?: return@on
            val idx = solver.getSlotCustom(Render2D.Mouse.x.toInt(), Render2D.Mouse.y.toInt()) ?: return@on
            if (idx !in gui.menu.slots.indices) return@on

            val slot = gui.menu.getSlot(idx)
            if (onInteractSlot(solver, slot, null, 0)) return@on

            ScreenUtils.click(idx, false, "LEFT")
        }.setEnabled(SETTING_CUSTOM_GUI.state)
    }

    fun color(idx: Int): Int = when (idx) {
        0 -> SETTING_CORRECT_COLOR.get()
        1 -> SETTING_SECOND_CORRECT_COLOR.get()
        2 -> SETTING_THIRD_CORRECT_COLOR.get()
        else -> SETTING_OTHER_CORRECT_COLOR.get()
    }
}

data class Cell(val x: Int, val y: Int, val w: Int, val h: Int)

interface ITerminalSolver {
    fun getSlotsBox(): Cell

    fun reset()

    fun onTick() {}

    fun onSetSlot(slot: Int, stack: ItemStack)

    fun onClickSlot(slot: Slot, btn: Int)

    fun onRenderSlot(ctx: GuiGraphicsExtractor, slot: Slot, loc: Rectangle, cancel: () -> Unit) {}

    fun doOnAfterRender() = false
    fun onAfterRender(ctx: GuiGraphicsExtractor, slot: Slot, loc: Rectangle) {}

    fun cancelClick(slot: Slot, btn: Int): Boolean = cancelClick(slot)
    fun cancelClick(slot: Slot): Boolean = false

    fun useCustomGui() = SETTING_CUSTOM_GUI.get()

    fun renderRectangle(ctx: GuiGraphicsExtractor, rect: Rectangle, color: Int) {
        renderRectangle(ctx, rect.x1, rect.y1, rect.x2, rect.y2, color)
    }

    fun renderRectangle(ctx: GuiGraphicsExtractor, x1: Double, y1: Double, x2: Double, y2: Double, color: Int) {
        ctx.guiRenderState.addGuiElement(
            QuadRenderState(
                RenderPipelines.GUI,
                Matrix3x2f(ctx.pose()),
                x1.toFloat(), y1.toFloat(),
                x2.toFloat(), y2.toFloat(),
                color,
                ctx.scissorStack.peek(),
            )
        )
    }

    fun renderGradientRectangle(
        ctx: GuiGraphicsExtractor,
        rect: Rectangle,
        color1: Int,
        color2: Int,
    ) {
        renderGradientRectangle(ctx, rect.x1, rect.y1, rect.x2, rect.y2, color1, color2, color1, color2)
    }

    fun renderGradientRectangle(
        ctx: GuiGraphicsExtractor,
        x1: Double, y1: Double,
        x2: Double, y2: Double,
        color1: Int, color2: Int,
        color3: Int, color4: Int,
    ) {
        ctx.guiRenderState.addGuiElement(
            GradientRectangleState(
                Matrix3x2f(ctx.pose()),
                x1, y1,
                x2, y2,
                color1, color2,
                color3, color4,
                scissorArea = ctx.scissorStack.peek(),
            )
        )
    }

    fun renderCenteredString(ctx: GuiGraphicsExtractor, str: String, x: Double, y: Double) {
        renderCenteredString(ctx, Component.literal(str.replaceCodes()), x, y)
    }

    fun renderCenteredString(ctx: GuiGraphicsExtractor, comp: Component, x: Double, y: Double) {
        var _x = x.toFloat()
        var _y = y.toFloat()

        val font = minecraft.font

        val fcs = comp.visualOrderText
        val w = font.width(fcs)
        val s = ceil(SETTING_CUSTOM_GUI_SCALE.get()).toInt()

        if (s != 1) {
            ctx.pose().pushMatrix()
            ctx.pose().translate(x.toFloat(), y.toFloat())
            ctx.pose().scale(SETTING_CUSTOM_GUI_SCALE.get().toFloat())

            _x = 0f
            _y = 2f
        }

        ctx.guiRenderState.addText(
            GuiTextRenderState(
                font,
                fcs,
                Matrix3x2f(ctx.pose()),
                (_x - w / 2).toInt(),
                _y.toInt(),
                -1,
                0,
                false,
                false,
                ctx.scissorStack.peek(),
            ).also {
                @Suppress("CAST_NEVER_SUCCEEDS")
                (it as? GuiTextRenderStateAccessor?)?.apply {
                    `devonian$setXf`(_x - w / 2)
                    `devonian$setYf`(_y)
                }
            }
        )

        if (s != 1) ctx.pose().popMatrix()
    }

    fun getBackgroundBorderSize() = 0.5 * SETTING_CUSTOM_GUI_SCALE.get()
    fun getSlotBorderSize() = 0.5 * SETTING_CUSTOM_GUI_SCALE.get()
    fun getSlotSize() = 24.0 * SETTING_CUSTOM_GUI_SCALE.get()
    fun getSlotPadding() = 1.5 * SETTING_CUSTOM_GUI_SCALE.get()

    fun onRenderBackground(ctx: GuiGraphicsExtractor) {
        val bounds = getCustomLocation(getSlotsBox())
        val color = SETTING_BACKGROUND_TERMINAL_COLOR.getColor()
        if (color.alpha == 0) return
        val outlineColorLeft = SETTING_CUSTOM_GUI_OUTLINE_LEFT.getColor()
        val outlineColorRight = SETTING_CUSTOM_GUI_OUTLINE_RIGHT.getColor()

        val s = getSlotSize()
        val b = getBackgroundBorderSize()
        if (outlineColorLeft.alpha != 0 && outlineColorRight.alpha != 0)
            renderGradientRectangle(
                ctx,
                bounds.x1 - s * 0.4,
                bounds.y1 - s * 0.6,
                bounds.x2 + s * 0.4,
                bounds.y2 + s * 0.6,
                outlineColorLeft.rgb, outlineColorRight.rgb,
                outlineColorLeft.rgb, outlineColorRight.rgb,
            )

        renderRectangle(
            ctx,
            bounds.x1 - s * 0.4 + b,
            bounds.y1 - s * 0.6 + b,
            bounds.x2 + s * 0.4 - b,
            bounds.y2 + s * 0.6 - b,
            color.rgb,
        )
    }

    fun renderSlotBackground(ctx: GuiGraphicsExtractor, loc: Rectangle) {
        if (!SETTING_BACKGROUND_SLOT.get()) return
        if (useCustomGui()) return
        if (SETTING_BACKGROUND_TERMINAL_COLOR.getColor().alpha == 0) return
        renderRectangle(
            ctx,
            loc.x1 - 2, loc.y1 - 2,
            loc.x2 + 2, loc.y2 + 2,
            SETTING_BACKGROUND_TERMINAL_COLOR.get(),
        )
    }

    fun renderSlot(ctx: GuiGraphicsExtractor, loc: Rectangle, idx: Int) {
        val b = getSlotBorderSize()
        val c = color(idx)

        renderRectangle(ctx, loc, Color(c, true).brighter().rgb)
        renderRectangle(
            ctx,
            loc.x1 + b,
            loc.y1 + b,
            loc.x2 - b,
            loc.y2 - b,
            c,
        )
    }

    fun getCustomLocation(slot: Slot): Rectangle? {
        if (slot.container == minecraft.player?.inventory) return null
        if (!useCustomGui()) return Rectangle(
            slot.x.toDouble(),
            slot.y.toDouble(),
            slot.x + 16.0,
            slot.y + 16.0,
        )

        val x = slot.containerSlot % 9
        val y = slot.containerSlot / 9

        val cell = Cell(x, y, 1, 1)
        return getCustomLocation(cell)
    }

    fun getCustomLocation(cell: Cell): Rectangle {
        val window = minecraft.window
        val box = getSlotsBox()

        val cx = box.x + box.w / 2.0
        val cy = box.y + box.h / 2.0

        val wx = window.guiScaledWidth / 2
        val wy = window.guiScaledHeight / 2

        val size = getSlotSize()
        val padding = getSlotPadding()
        val slotBounds = size + padding * 2

        val x = (cell.x - cx) * slotBounds + wx + padding
        val y = (cell.y - cy) * slotBounds + wy + padding
        return Rectangle(
            x, y,
            x + slotBounds * (cell.w - 1) + size,
            y + slotBounds * (cell.h - 1) + size,
        )
    }

    fun getSlotCustom(mouseX: Int, mouseY: Int): Int? {
        val window = minecraft.window
        val box = getSlotsBox()

        val cx = box.x + box.w / 2.0
        val cy = box.y + box.h / 2.0

        val wx = window.guiScaledWidth / 2
        val wy = window.guiScaledHeight / 2

        val size = getSlotSize()
        val padding = getSlotPadding()
        val slotBounds = size + padding * 2

        val x = ((mouseX - wx) / slotBounds + cx).toInt()
        val y = ((mouseY - wy) / slotBounds + cy).toInt()

        if (x !in 0 .. 8) return null
        if (y !in 0 .. 5) return null

        return y * 9 + x
    }
}

enum class TerminalData(val title: Regex) : ITerminalSolver {
    NUMBERS("Click in order!".toRegex()) {
        override fun getSlotsBox(): Cell = Cell(2, 1, 5, 2)

        private val slots = IntArray(36) { 0 }
        private var minCount = 14

        override fun reset() {
            slots.fill(0)
            minCount = 14
        }

        override fun onSetSlot(slot: Int, stack: ItemStack) {
            if (slot !in slots.indices) return
            val count = if (stack.item == Items.STAINED_GLASS_PANE.red) stack.count() else 0
            slots[slot] = count
            if (count > 0) minCount = min(minCount, count)
        }

        override fun onClickSlot(slot: Slot, btn: Int) {
            val idx = slot.containerSlot

            val count = slots.getOrNull(idx) ?: return
            if (count != minCount) return

            slots[idx] = 0
            minCount++
        }

        override fun onRenderSlot(ctx: GuiGraphicsExtractor, slot: Slot, loc: Rectangle, cancel: () -> Unit) {
            val count = slots.getOrElse(slot.containerSlot) { 0 }
            if (count == 0) {
                if (SETTING_HIDE_DONE.get()) {
                    renderSlotBackground(ctx, loc)
                    cancel()
                }
                return
            }

            renderSlotBackground(ctx, loc)
            renderSlot(ctx, loc, count - minCount)
            if (SETTING_RENDER_NUMBERS.get()) {
                renderCenteredString(
                    ctx,
                    "$count",
                    loc.x1 + (loc.x2 - loc.x1) / 2,
                    loc.y1 + (loc.y2 - loc.y1) / 4,
                )
            }

            cancel()
        }

        override fun cancelClick(slot: Slot): Boolean {
            return slots.getOrElse(slot.containerSlot) { 0 } != minCount
        }
    },
    COLORS("^Select all the (.*?) items!$".toRegex()) {
        override fun getSlotsBox(): Cell = Cell(1, 1, 7, 4)

        private var toFind: String? = null
        override var currentTitle: String = ""
            set(value) {
                toFind = title.matchEntire(value)?.groupValues?.drop(1)?.getOrNull(0)
                field = value
            }

        private val slots = BooleanArray(54)
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
            slots.fill(false)
        }

        override fun onSetSlot(slot: Int, stack: ItemStack) {
            if (slot !in slots.indices) return
            if (stack.get(DataComponents.ENCHANTMENT_GLINT_OVERRIDE) == true) return
            val toFind = toFind ?: return

            var name = stack.customName?.string ?: stack.itemName.string
            for ((old, replacement) in fixedColorItems) {
                if (name.startsWith(old, ignoreCase = true))
                    name = replacement
            }

            slots[slot] = name.startsWith(toFind, ignoreCase = true)
        }

        override fun onClickSlot(slot: Slot, btn: Int) {
            if (slot.containerSlot !in slots.indices) return
            slots[slot.containerSlot] = false
        }

        override fun onRenderSlot(ctx: GuiGraphicsExtractor, slot: Slot, loc: Rectangle, cancel: () -> Unit) {
            if (!slots.getOrElse(slot.containerSlot) { false }) {
                if (SETTING_HIDE_DONE.get()) {
                    renderSlotBackground(ctx, loc)
                    cancel()
                }
                return
            }

            renderSlotBackground(ctx, loc)
            renderSlot(ctx, loc, 0)
            if (SETTING_HIDE_ITEMS.get()) cancel()
        }

        override fun cancelClick(slot: Slot): Boolean {
            return !slots.getOrElse(slot.containerSlot) { false }
        }
    },
    STARTS_WITH("^What starts with: '(.*?)'\\?$".toRegex()) {
        override fun getSlotsBox(): Cell = Cell(1, 1, 7, 4)

        private var toFind: String? = null
        override var currentTitle: String = ""
            set(value) {
                toFind = title.matchEntire(value)?.groupValues?.drop(1)?.getOrNull(0)
                field = value
            }

        private val slots = BooleanArray(45) { false }
        private var firstScan = true

        override fun reset() {
            slots.fill(false)
            firstScan = true
        }

        override fun onSetSlot(slot: Int, stack: ItemStack) {
            if (slot !in slots.indices) return
            if (slot == 44) firstScan = false
            if (!firstScan && stack.get(DataComponents.ENCHANTMENT_GLINT_OVERRIDE) == true) return
            val toFind = toFind ?: return

            val name = stack.customName?.string ?: stack.itemName.string

            slots[slot] = name.startsWith(toFind, ignoreCase = true)
        }

        override fun onClickSlot(slot: Slot, btn: Int) {
            if (slot.containerSlot !in slots.indices) return
            slots[slot.containerSlot] = false
        }

        override fun onRenderSlot(ctx: GuiGraphicsExtractor, slot: Slot, loc: Rectangle, cancel: () -> Unit) {
            if (!slots.getOrElse(slot.containerSlot) { false }) {
                if (SETTING_HIDE_DONE.get()) {
                    renderSlotBackground(ctx, loc)
                    cancel()
                }
                return
            }

            renderSlotBackground(ctx, loc)
            renderSlot(ctx, loc, 0)
            if (SETTING_HIDE_ITEMS.get()) cancel()
        }

        override fun cancelClick(slot: Slot): Boolean {
            return !slots.getOrElse(slot.containerSlot) { false }
        }
    },
    RUBIX("^Change all to same color!$".toRegex()) {
        override fun getSlotsBox(): Cell = Cell(3, 1, 3, 3)

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
            Color(191, 87, 0).rgb,
            Color(184, 134, 11).rgb,
            Color(0, 86, 59).rgb,
            Color(0, 50, 98).rgb,
            Color(150, 0, 24).rgb,
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

        private val slots = IntArray(45) { 0 }
        private val slotColors = IntArray(45) { -1 }
        private var firstCalc = true

        override fun reset() {
            slots.fill(0)
            slotColors.fill(-1)
            firstCalc = true
        }

        override fun onSetSlot(slot: Int, stack: ItemStack) {
            if (slot !in slots.indices) return
            if (!isRubix[slot]) return
            if (!firstCalc) return

            val color = rubixOrder.indexOf(stack.item)
            slotColors[slot] = color

            if (slot != 32) return
            firstCalc = false

            var best = 19
            for (target in rubixOrder.indices) {
                var clicks = 0
                rubixIndices.forEach {
                    val color = slotColors[it]
                    if (color == -1) return@forEach

                    var dist = abs(target - color)
                    if (dist >= 3) dist = 5 - dist

                    clicks += dist
                }

                if (clicks >= best) continue
                best = clicks
                slots.fill(0)
                rubixIndices.forEach {
                    val color = slotColors[it]
                    if (color == -1) return@forEach

                    var lc = target - color
                    if (lc < 0) lc += 5
                    slots[it] = lc
                }
            }
        }

        override fun onClickSlot(slot: Slot, btn: Int) {
            if (slot.containerSlot !in slots.indices) return
            val dir = when (btn) {
                0 -> 1
                1 -> -1
                else -> return
            }

            slots[slot.containerSlot] -= dir
            slotColors[slot.containerSlot] += dir

            if (slots[slot.containerSlot] < 0) slots[slot.containerSlot] += 5
            if (slotColors[slot.containerSlot] < 0) slotColors[slot.containerSlot] += 5
            if (slots[slot.containerSlot] == 5) slots[slot.containerSlot] -= 5
            if (slotColors[slot.containerSlot] == 5) slotColors[slot.containerSlot] -= 5
        }

        override fun onRenderSlot(ctx: GuiGraphicsExtractor, slot: Slot, loc: Rectangle, cancel: () -> Unit) {
            if (SETTING_HIDE_DONE.get() && !isRubix.getOrElse(slot.containerSlot) { false }) {
                renderSlotBackground(ctx, loc)
            }

            if (!useCustomGui()) return
            val idx = slotColors.getOrNull(slot.containerSlot) ?: return
            if (idx == -1) return

            val color = rubixColors.getOrNull(idx) ?: return

            val b = getSlotBorderSize()
            renderRectangle(ctx, loc, Color(color, true).brighter().rgb)
            renderRectangle(
                ctx,
                loc.x1 + b,
                loc.y1 + b,
                loc.x2 - b,
                loc.y2 - b,
                color,
            )
        }

        override fun doOnAfterRender(): Boolean = true

        override fun onAfterRender(ctx: GuiGraphicsExtractor, slot: Slot, loc: Rectangle) {
            var clicks = slots.getOrElse(slot.containerSlot) { 0 }
            if (clicks == 0) return

            if (!SETTING_RUBIX_FORCE_POSITIVE.get() && clicks >= 3) clicks -= 5
            val str = strings.getOrNull(clicks + 2) ?: return

            renderCenteredString(
                ctx,
                str,
                loc.x1 + (loc.x2 - loc.x1) / 2,
                loc.y1 + (loc.y2 - loc.y1) / 4,
            )
        }

        override fun cancelClick(slot: Slot, btn: Int): Boolean {
            if (btn == 2) return true
            val clicks = slots.getOrElse(slot.containerSlot) { 0 }
            return clicks == 0 || SETTING_RUBIX_BLOCK_SUBOPTIMAL.get() && ((clicks > 2) == (btn == 0))
        }
    },
    RED_GREEN("^Correct all the panes!$".toRegex()) {
        override fun getSlotsBox(): Cell = Cell(2, 1, 5, 3)

        private val slots = BooleanArray(45) { false }
        private val slotClickTimes = LongArray(45) { 0L }
        private var sanity = 0

        override fun reset() {
            slots.fill(false)
            slotClickTimes.fill(0L)
            sanity = 0
        }

        override fun onTick() {
            if (slots.any { it }) return

            if (++sanity < 10) return

            val screen = minecraft.gui.screen() as? AbstractContainerScreen<*>? ?: return
            sanity = 0

            screen.menu.items.forEachIndexed { i, stack ->
                if (i !in slots.indices) return

                slots[i] = stack.item == Items.STAINED_GLASS_PANE.red
            }
        }

        override fun onSetSlot(slot: Int, stack: ItemStack) {
            if (slot !in slots.indices) return
            slots[slot] = stack.item == Items.STAINED_GLASS_PANE.red
        }

        override fun onClickSlot(slot: Slot, btn: Int) {
            if (slot.containerSlot !in slots.indices) return
            slots[slot.containerSlot] = false
            slotClickTimes[slot.containerSlot] = System.currentTimeMillis()
        }

        override fun useCustomGui(): Boolean = super.useCustomGui() && !SETTING_RED_GREEN_DISABLE_RENDER.get()

        override fun onRenderSlot(ctx: GuiGraphicsExtractor, slot: Slot, loc: Rectangle, cancel: () -> Unit) {
            if (SETTING_RED_GREEN_DISABLE_RENDER.get()) return

            val data = slots.getOrNull(slot.containerSlot)
            if (data != true) {
                if (SETTING_HIDE_DONE.get()) {
                    renderSlotBackground(ctx, loc)
                    cancel()
                }
                return
            }

            renderSlotBackground(ctx, loc)
            renderSlot(ctx, loc, 0)
            cancel()
        }

        override fun cancelClick(slot: Slot): Boolean {
            if (slot.containerSlot !in slots.indices) return true

            if (!slots[slot.containerSlot]) return true

            val t = System.currentTimeMillis()
            if (slotClickTimes[slot.containerSlot] > t) return true

            slotClickTimes[slot.containerSlot] = t + SETTING_RED_GREEN_PREVENT_RECLICK.get().toInt()
            return false
        }
    },
    MELODY("^Click the button on time!$".toRegex()) {
        override fun getSlotsBox(): Cell = Cell(0, 0, 9, 6)

        override fun reset() {}

        override fun onSetSlot(slot: Int, stack: ItemStack) {}

        override fun onClickSlot(slot: Slot, btn: Int) {}

        override fun useCustomGui(): Boolean = false
    };

    open var currentTitle = ""

    companion object {
        fun byMatch(string: String) = TerminalData.entries.find { it.title.matches(string) }
            ?.also { it.currentTitle = string }
    }
}