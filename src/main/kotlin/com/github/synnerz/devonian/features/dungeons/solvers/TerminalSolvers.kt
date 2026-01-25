package com.github.synnerz.devonian.features.dungeons.solvers

import com.github.synnerz.devonian.api.ScreenUtils
import com.github.synnerz.devonian.api.dungeon.Stages
import com.github.synnerz.devonian.api.events.*
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.features.dungeons.solvers.TerminalSolvers.SETTING_HIDE_DONE
import com.github.synnerz.devonian.features.dungeons.solvers.TerminalSolvers.SETTING_HIDE_ITEMS
import com.github.synnerz.devonian.features.dungeons.solvers.TerminalSolvers.SETTING_RED_GREEN_DISABLE_RENDER
import com.github.synnerz.devonian.features.dungeons.solvers.TerminalSolvers.SETTING_RENDER_NUMBERS
import com.github.synnerz.devonian.features.dungeons.solvers.TerminalSolvers.SETTING_RUBIX_FORCE_POSITIVE
import com.github.synnerz.devonian.features.dungeons.solvers.TerminalSolvers.TerminalSlot
import com.github.synnerz.devonian.features.dungeons.solvers.TerminalSolvers.color
import com.github.synnerz.devonian.features.dungeons.solvers.TerminalSolvers.minecraft
import com.github.synnerz.devonian.utils.BasicState
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import kotlin.math.abs

// Credits to <https://github.com/UnclaimedBloom6/BloomModule/blob/main/features/TerminalSolvers.js>
// this fk noob
object TerminalSolvers : Feature(
    "terminalSolvers",
    "Shows the correct slots to click to solve the current terminal.",
    Categories.DUNGEONS,
    "catacombs",
    subcategory = "F7",
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

    private var currentSolver: TerminalData? = null

    private val PREVENTED_SOUND = SoundEvents.NOTE_BLOCK_BASS

    data class TerminalSlot(val slot: Int, val itemStack: ItemStack)
    data class RubixSlot(val slot: Int, val itemStack: ItemStack, val color: Int, val clicks: Int = 0)

    private fun onInteractSlot(slot: Slot, event: CancellableEvent): Boolean {
        return if (SETTING_CANCEL_WRONG_CLICKS.get() && currentSolver?.cancelClick(slot) == true) {
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
            onInteractSlot(slot, event)
        }

        on<PickupItemInventoryEvent> { event ->
            if (currentSolver == null) return@on

            if (event.isAll && SETTING_CANCEL_NONCLICKS.get()) {
                event.cancel()
                return@on
            }

            if (onInteractSlot(event.slot, event)) return@on
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

    fun onTick()

    fun onRenderSlot(event: RenderSlotEvent) {}

    fun onAfterRender(event: PostRenderSlotsEvent) {}

    fun cancelClick(slot: Slot): Boolean
}

enum class TerminalData(val title: Regex) : ITerminalSolver {
    NUMBERS("Click in order!".toRegex()) {
        override val changesWindow: Boolean = true

        private var correctSlots = mutableListOf<TerminalSlot>()

        override fun onTick() {
            val screen = minecraft.screen ?: return
            val items = (screen as AbstractContainerScreen<*>).menu.items

            correctSlots.clear()

            items.forEachIndexed { idx, item ->
                if (item.item != Items.RED_STAINED_GLASS_PANE) return@forEachIndexed
                correctSlots.add(TerminalSlot(idx, item))
            }

            correctSlots.sortBy { it.itemStack.count }
        }

        override fun onRenderSlot(event: RenderSlotEvent) {
            val slot = event.slot
            if (slot.container == minecraft.player?.inventory) return
            val idx = correctSlots.indexOfFirst { it.slot == event.slot.containerSlot }
            if (idx == -1) {
                if (SETTING_HIDE_DONE.get()) event.cancel()
                return
            }
            val data = correctSlots[idx]

            event.ctx.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, color(idx))
            if (SETTING_RENDER_NUMBERS.get())
                event.ctx.drawCenteredString(
                    minecraft.font,
                    "${data.itemStack.count}",
                    slot.x + 8, slot.y + 4, -1
                )
            event.cancel()
        }

        override fun cancelClick(slot: Slot): Boolean {
            return !correctSlots.any { it.slot == slot.containerSlot }
        }
    },
    COLORS("^Select all the (.*?) items!$".toRegex()) {
        override val changesWindow: Boolean = true

        private val correctSlots = mutableListOf<TerminalSlot>()
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

        override fun onTick() {
            val screen = minecraft.screen ?: return
            val toFind = title.matchEntire(screen.title.string)?.groupValues?.drop(1)?.getOrNull(0) ?: return
            val items = (screen as AbstractContainerScreen<*>).menu.items

            correctSlots.clear()

            items.forEachIndexed { idx, item ->
                if (item.get(DataComponents.ENCHANTMENT_GLINT_OVERRIDE) == true) return@forEachIndexed
                var name = item.customName?.string ?: item.itemName.string
                for (fixed in fixedColorItems) {
                    if (name.startsWith(fixed.key, ignoreCase = true))
                        name = fixed.value
                }
                if (!name.startsWith(toFind, ignoreCase = true)) return@forEachIndexed

                correctSlots.add(TerminalSlot(idx, item))
            }
        }

        override fun onRenderSlot(event: RenderSlotEvent) {
            val slot = event.slot
            if (slot.container == minecraft.player?.inventory) return
            val idx = correctSlots.indexOfFirst { it.slot == event.slot.containerSlot }
            if (idx == -1) {
                if (SETTING_HIDE_DONE.get()) event.cancel()
                return
            }

            event.ctx.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, color(0))
            if (SETTING_HIDE_ITEMS.get()) event.cancel()
        }

        override fun cancelClick(slot: Slot): Boolean {
            return !correctSlots.any { it.slot == slot.containerSlot }
        }
    },
    STARTS_WITH("^What starts with: '(.*?)'\\?$".toRegex()) {
        override val changesWindow: Boolean = true

        private val correctSlots = mutableListOf<TerminalSlot>()

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

        override fun onTick() {
            val screen = minecraft.screen ?: return
            val toFind = title.matchEntire(screen.title.string)?.groupValues?.drop(1)?.getOrNull(0) ?: return
            val items = (screen as AbstractContainerScreen<*>).menu.items

            correctSlots.clear()

            items.forEachIndexed { idx, item ->
                if (item.get(DataComponents.ENCHANTMENT_GLINT_OVERRIDE) == true) return@forEachIndexed

                var name = item.customName?.string ?: item.itemName.string
                name = legacyNames[name] ?: name
                if (!name.startsWith(toFind, ignoreCase = true)) return@forEachIndexed

                correctSlots.add(TerminalSlot(idx, item))
            }
        }

        override fun onRenderSlot(event: RenderSlotEvent) {
            val slot = event.slot
            if (slot.container == minecraft.player?.inventory) return

            val idx = correctSlots.indexOfFirst { it.slot == event.slot.containerSlot }
            if (idx == -1) {
                if (SETTING_HIDE_DONE.get()) event.cancel()
                return
            }

            event.ctx.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, color(0))
            if (SETTING_HIDE_ITEMS.get()) event.cancel()
        }

        override fun cancelClick(slot: Slot): Boolean {
            return !correctSlots.any { it.slot == slot.containerSlot }
        }
    },
    RUBIX("^Change all to same color!$".toRegex()) {
        override val changesWindow: Boolean = true

        private val rubixIndices = listOf(12, 13, 14, 21, 22, 23, 30, 31, 32)

        // left click = ++, right click = --
        private val rubixOrder = listOf(
            Items.ORANGE_STAINED_GLASS_PANE,
            Items.YELLOW_STAINED_GLASS_PANE,
            Items.GREEN_STAINED_GLASS_PANE,
            Items.BLUE_STAINED_GLASS_PANE,
            Items.RED_STAINED_GLASS_PANE,
        )
        private var correctSlots = listOf<TerminalSolvers.RubixSlot>()
        private val strings = arrayOf(
            Component.literal("§e-2"),
            Component.literal("§a-1"),
            null,
            Component.literal("§a1"),
            Component.literal("§e2"),
            Component.literal("§e3"),
            Component.literal("§c4"),
        )

        override fun onTick() {
            val screen = minecraft.screen ?: return
            val items = (screen as AbstractContainerScreen<*>).menu.items
            val slotsIn = mutableListOf<TerminalSolvers.RubixSlot>()

            rubixIndices.forEach { idx ->
                val item = items.getOrNull(idx) ?: return@forEach
                val color = rubixOrder.indexOf(item.item)
                if (color < 0) return@forEach
                slotsIn.add(TerminalSolvers.RubixSlot(idx, item, color))
            }

            var best = 19
            for (target in rubixOrder.indices) {
                var clicks = 0
                val slots = slotsIn.filter {
                    var dist = abs(target - it.color)
                    if (dist >= 3) dist = 5 - dist
                    clicks += dist
                    dist > 0
                }

                if (clicks < best) {
                    best = clicks
                    correctSlots = slots.map {
                        var lc = target - it.color
                        if (lc < 0) lc += 5
                        TerminalSolvers.RubixSlot(it.slot, it.itemStack, it.color, lc)
                    }
                }
            }
        }

        override fun onAfterRender(event: PostRenderSlotsEvent) {
            event.container.menu.slots.forEach { slot ->
                if (slot.container == minecraft.player?.inventory) return@forEach

                val data = correctSlots.find { it.slot == slot.containerSlot } ?: return@forEach

                val num = if (!SETTING_RUBIX_FORCE_POSITIVE.get() && data.clicks >= 3) data.clicks - 5
                else data.clicks
                val str = strings.getOrNull(num + 2) ?: return@forEach

                event.ctx.drawCenteredString(minecraft.font, str, slot.x + 8, slot.y + 4, -1)
            }
        }

        override fun cancelClick(slot: Slot): Boolean {
            return (correctSlots.find { it.slot == slot.containerSlot }?.clicks ?: 0) == 0
        }
    },
    RED_GREEN("^Correct all the panes!$".toRegex()) {
        override val changesWindow: Boolean = true

        private val paneSlots = mutableListOf(
            11, 12, 13, 14, 15,
            20, 21, 22, 23, 24,
            29, 30, 31, 32, 33,
        )

        private val correctSlots = mutableListOf<TerminalSlot>()

        override fun onTick() {
            val screen = minecraft.screen ?: return
            val items = (screen as AbstractContainerScreen<*>).menu.items

            correctSlots.clear()

            paneSlots.forEach { idx ->
                val item = items.getOrNull(idx) ?: return@forEach
                if (item.item != Items.RED_STAINED_GLASS_PANE) return@forEach
                correctSlots.add(TerminalSlot(idx, item))
            }
        }

        override fun onRenderSlot(event: RenderSlotEvent) {
            if (SETTING_RED_GREEN_DISABLE_RENDER.get()) return
            val slot = event.slot
            if (slot.container == minecraft.player?.inventory) return

            val idx = correctSlots.indexOfFirst { it.slot == event.slot.containerSlot }
            if (idx == -1) {
                if (SETTING_HIDE_DONE.get()) event.cancel()
                return
            }

            event.ctx.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, color(0))
            event.cancel()
        }

        override fun cancelClick(slot: Slot): Boolean {
            return !correctSlots.any { it.slot == slot.containerSlot }
        }
    },
    MELODY("^Click the button on time!$".toRegex()) {
        override val changesWindow: Boolean = false

        override fun onTick() {}

        override fun cancelClick(slot: Slot): Boolean = false
    };

    companion object {
        fun byMatch(string: String) = TerminalData.entries.find { it.title.matches(string) }
    }
}