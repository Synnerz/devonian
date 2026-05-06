package com.github.synnerz.devonian.features.dungeons.solvers

import com.github.synnerz.devonian.api.ScreenUtils
import com.github.synnerz.devonian.api.dungeon.Stages
import com.github.synnerz.devonian.api.events.*
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.features.dungeons.solvers.TerminalSolvers.SETTING_BACKGROUND_SLOT
import com.github.synnerz.devonian.features.dungeons.solvers.TerminalSolvers.SETTING_BACKGROUND_TERMINAL_COLOR
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
import net.minecraft.world.item.Items
import java.awt.Color
import kotlin.math.abs
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
        "Makes it so every slot rendered on the Terminal gui will have a custom color (may negatively impact performance.)",
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

    private var currentSolver: TerminalData? = null

    private val PREVENTED_SOUND = SoundEvents.NOTE_BLOCK_BASS

    data class InterimRubixSlot(val idx: Int, val color: Int, val clicks: Int = 0)
    data class RedGreenSlot(val correct: Boolean, var clickCd: Long = 0L)

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

        on<RenderSlotEvent> { event ->
            currentSolver?.onRenderSlot(event)
        }

        on<PostRenderSlotsEvent> { event ->
            currentSolver?.onAfterRender(event)
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

    fun reset()

    fun onTick()

    fun onRenderSlot(event: RenderSlotEvent) {}

    fun onAfterRender(event: PostRenderSlotsEvent) {}

    fun cancelClick(slot: Slot, lc: Boolean): Boolean = cancelClick(slot)
    fun cancelClick(slot: Slot): Boolean = false

    fun renderSlotBackground(ctx: GuiGraphicsExtractor, slot: Slot) {
        if (!SETTING_BACKGROUND_SLOT.get()) return
        if (SETTING_BACKGROUND_TERMINAL_COLOR.getColor().alpha == 0) return
        ctx.fill(slot.x - 2, slot.y - 2, slot.x + 18, slot.y + 18, SETTING_BACKGROUND_TERMINAL_COLOR.get())
    }

    fun renderSlot(ctx: GuiGraphicsExtractor, slot: Slot, idx: Int) {
        ctx.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, color(idx))
    }
}

enum class TerminalData(val title: Regex) : ITerminalSolver {
    NUMBERS("Click in order!".toRegex()) {
        override val changesWindow: Boolean = true

        private var slots = emptyArray<Int>()
        private var minCount = 14

        override fun reset() {
            slots = emptyArray()
        }

        override fun onTick() {
            val screen = minecraft.gui.screen() ?: return
            val items = (screen as AbstractContainerScreen<*>).menu.items

            minCount = 14
            slots = Array(items.size) { idx ->
                val stack = items[idx]
                val count = if (stack.item == Items.STAINED_GLASS_PANE.red) stack.count
                else 0
                if (count > 0) minCount = min(minCount, count)
                return@Array count
            }
        }

        override fun onRenderSlot(event: RenderSlotEvent) {
            val slot = event.slot
            if (slot.container == minecraft.player?.inventory) return

            val count = slots.getOrElse(slot.containerSlot) { 0 }
            if (count == 0) {
                if (SETTING_HIDE_DONE.get()) {
                    renderSlotBackground(event.ctx, slot)
                    event.cancel()
                }
                return
            }

            renderSlotBackground(event.ctx, slot)
            renderSlot(event.ctx, slot, count - minCount)
            if (SETTING_RENDER_NUMBERS.get()) {
                event.ctx.centeredText(
                    minecraft.font,
                    "$count",
                    slot.x + 8, slot.y + 4, -1
                )
            }

            event.cancel()
        }

        override fun cancelClick(slot: Slot): Boolean {
            return slots.getOrElse(slot.containerSlot) { 0 } == 0
        }
    },
    COLORS("^Select all the (.*?) items!$".toRegex()) {
        override val changesWindow: Boolean = true

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

        override fun onRenderSlot(event: RenderSlotEvent) {
            val slot = event.slot
            if (slot.container == minecraft.player?.inventory) return

            if (!slots.getOrElse(slot.containerSlot) { false }) {
                if (SETTING_HIDE_DONE.get()) {
                    renderSlotBackground(event.ctx, slot)
                    event.cancel()
                }
                return
            }

            renderSlotBackground(event.ctx, slot)
            renderSlot(event.ctx, slot, 0)
            if (SETTING_HIDE_ITEMS.get()) event.cancel()
        }

        override fun cancelClick(slot: Slot): Boolean {
            return !slots.getOrElse(slot.containerSlot) { false }
        }
    },
    STARTS_WITH("^What starts with: '(.*?)'\\?$".toRegex()) {
        override val changesWindow: Boolean = true

        private var slots = emptyArray<Boolean>()

        private val legacyNames = mapOf(
            "Grass" to "Grass Block",
            "Redstone Dust" to "Redstone",
            "Empty Map" to "Map",
            "Oak Planks" to "Oak Wood Planks",
            "Spruce Planks" to "Spruce Wood Planks",
            "Birch Planks" to "Birch Wood Planks",
            "Jungle Planks" to "Jungle Wood Planks",
            "Acacia Planks" to "Acacia Wood Planks",
            "Dark Oak Planks" to "Dark Oak Wood Planks",
            "Tall Grass" to "Double Tallgrass",
            "Brown Mushroom" to "Mushroom",
            "Red Mushroom" to "Mushroom",
            "Brick Slab" to "Bricks Slab",
            "Stone Brick Slab" to "Stone Bricks Slab",
            "Oak Slab" to "Oak Wood Slab",
            "Spruce Slab" to "Spruce Wood Slab",
            "Birch Slab" to "Birch Wood Slab",
            "Jungle Slab" to "Jungle Wood Slab",
            "Acacia Slab" to "Acacia Wood Slab",
            "Dark Oak Slab" to "Dark Oak Wood Slab",
            "Mossy Cobblestone" to "Mossy Stone",
            "Oak Stairs" to "Oak Wood Stairs",
            "Spruce Stairs" to "Spruce Wood Stairs",
            "Birch Stairs" to "Birch Wood Stairs",
            "Jungle Stairs" to "Jungle Wood Stairs",
            "Acacia Stairs" to "Acacia Wood Stairs",
            "Dark Oak Stairs" to "Dark Oak Wood Stairs",
            "Oak Pressure Plate" to "Wooden Pressure Plate",
            "Light Weighted Pressure Plate" to "Weighted Pressure Plate (Light)",
            "Heavy Weighted Pressure Plate" to "Weighted Pressure Plate (Heavy)",
            "Oak Button" to "Button",
            "Stone Button" to "Button",
            "White Carpet" to "Carpet",
            "Black Terracotta" to "Black Stained Clay",
            "Red Terracotta" to "Red Stained Clay",
            "Green Terracotta" to "Green Stained Clay",
            "Brown Terracotta" to "Brown Stained Clay",
            "Blue Terracotta" to "Blue Stained Clay",
            "Purple Terracotta" to "Purple Stained Clay",
            "Cyan Terracotta" to "Cyan Stained Clay",
            "Light Gray Terracotta" to "Light Gray Stained Clay",
            "Gray Terracotta" to "Gray Stained Clay",
            "Pink Terracotta" to "Pink Stained Clay",
            "Lime Terracotta" to "Lime Stained Clay",
            "Yellow Terracotta" to "Yellow Stained Clay",
            "Light Blue Terracotta" to "Light Blue Stained Clay",
            "Magenta Terracotta" to "Magenta Stained Clay",
            "Orange Terracotta" to "Orange Stained Clay",
            "White Terracotta" to "White Stained Clay",
            "Terracotta" to "Hardened Clay",
            "Nether Portal" to "Portal",
            "White Wool" to "Wool",
            "Block of Lapis Lazuli" to "Lapis Lazuli Block",
            "Red Bed" to "Bed",
            "White Bed" to "Bed",
            "Oak Trapdoor" to "Wooden Trapdoor",
            "Infested Stone" to "Stone Monster Egg",
            "Infested Cobblestone" to "Cobblestone Monster Egg",
            "Infested Stone Bricks" to "Stone Brick Monster Egg",
            "Infested Mossy Stone Bricks" to "Mossy Stone Brick Monster Egg",
            "Infested Cracked Stone Bricks" to "Cracked Stone Brick Monster Egg",
            "Infested Chiseled Stone Bricks" to "Chiseled Stone Brick Monster Egg",
            "Enchanting Table" to "Enchantment Table",
            "Chipped Anvil" to "Slightly Damaged Anvil",
            "Damaged Anvil" to "Very Damaged Anvil",
            "Daylight Detector" to "Daylight Sensor",
            "Quartz Pillar" to "Pillar Quartz Block",
            "Wheat Seeds" to "Seeds",
            "Chainmail Helmet" to "Chain Helmet",
            "Chainmail Chestplate" to "Chain Chestplate",
            "Chainmail Leggings" to "Chain Leggings",
            "Chainmail Boots" to "Chain Boots",
            "Oak Boat" to "Boat",
            "Milk Bucket" to "Milk",
            "Sugar Cane" to "Sugar Canes",
            "Raw Cod" to "Raw Fish",
            "Tropical Fish" to "Clownfish",
            "Cooked Cod" to "Cooked Fish",
            "Red Dye" to "Rose Red",
            "Green Dye" to "Cactus Green",
            "Yellow Dye" to "Dandelion Yellow",
            "Glistering Melon Slice" to "Glistering Melon",
            "Player Head" to "Head",
            "Golden Horse Armor" to "Gold Horse Armor",
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
                name = legacyNames[name] ?: name

                return@Array name.startsWith(toFind, ignoreCase = true)
            }
        }

        override fun onRenderSlot(event: RenderSlotEvent) {
            val slot = event.slot
            if (slot.container == minecraft.player?.inventory) return

            if (!slots.getOrElse(slot.containerSlot) { false }) {
                if (SETTING_HIDE_DONE.get()) {
                    renderSlotBackground(event.ctx, slot)
                    event.cancel()
                }
                return
            }

            renderSlotBackground(event.ctx, slot)
            renderSlot(event.ctx, slot, 0)
            if (SETTING_HIDE_ITEMS.get()) event.cancel()
        }

        override fun cancelClick(slot: Slot): Boolean {
            return !slots.getOrElse(slot.containerSlot) { false }
        }
    },
    RUBIX("^Change all to same color!$".toRegex()) {
        override val changesWindow: Boolean = true

        private val rubixIndices = listOf(12, 13, 14, 21, 22, 23, 30, 31, 32)
        // left click = ++, right click = --
        private val rubixOrder = listOf(
            Items.STAINED_GLASS_PANE.orange,
            Items.STAINED_GLASS_PANE.yellow,
            Items.STAINED_GLASS_PANE.green,
            Items.STAINED_GLASS_PANE.blue,
            Items.STAINED_GLASS_PANE.red,
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
        private var lastClicked = -1
        private var lastClickType = false

        override fun reset() {
            slots = emptyArray()
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
        }

        override fun onAfterRender(event: PostRenderSlotsEvent) {
            event.container.menu.slots.forEach { slot ->
                if (slot.container == minecraft.player?.inventory) return@forEach

                var clicks = slots.getOrElse(slot.containerSlot) { 0 }
                if (clicks == 0) return@forEach

                if (!SETTING_RUBIX_FORCE_POSITIVE.get() && clicks >= 3) clicks -= 5
                val str = strings.getOrNull(clicks + 2) ?: return@forEach

                event.ctx.centeredText(minecraft.font, str, slot.x + 8, slot.y + 4, -1)
            }
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

        override fun onRenderSlot(event: RenderSlotEvent) {
            if (SETTING_RED_GREEN_DISABLE_RENDER.get()) return
            val slot = event.slot
            if (slot.container == minecraft.player?.inventory) return

            val data = slots.getOrNull(slot.containerSlot)
            if (data == null || !data.correct) {
                if (SETTING_HIDE_DONE.get()) {
                    renderSlotBackground(event.ctx, slot)
                    event.cancel()
                }
                return
            }

            renderSlotBackground(event.ctx, slot)
            renderSlot(event.ctx, slot, 0)
            event.cancel()
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

        override fun reset() {}

        override fun onTick() {}
    };

    companion object {
        fun byMatch(string: String) = TerminalData.entries.find { it.title.matches(string) }
    }
}