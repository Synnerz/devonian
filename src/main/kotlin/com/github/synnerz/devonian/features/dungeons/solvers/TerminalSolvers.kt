package com.github.synnerz.devonian.features.dungeons.solvers

import com.github.synnerz.devonian.api.ScreenUtils
import com.github.synnerz.devonian.api.dungeon.Stages
import com.github.synnerz.devonian.api.events.GuiClickEvent
import com.github.synnerz.devonian.api.events.GuiCloseEvent
import com.github.synnerz.devonian.api.events.GuiOpenEvent
import com.github.synnerz.devonian.api.events.RenderSlotEvent
import com.github.synnerz.devonian.api.events.TickEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.features.dungeons.solvers.TerminalSolvers.SETTING_CANCEL_WRONG_CLICKS
import com.github.synnerz.devonian.features.dungeons.solvers.TerminalSolvers.SETTING_HIDE_DONE
import com.github.synnerz.devonian.features.dungeons.solvers.TerminalSolvers.SETTING_RENDER_NUMBERS
import com.github.synnerz.devonian.features.dungeons.solvers.TerminalSolvers.TerminalSlot
import com.github.synnerz.devonian.features.dungeons.solvers.TerminalSolvers.color
import com.github.synnerz.devonian.features.dungeons.solvers.TerminalSolvers.minecraft
import com.github.synnerz.devonian.utils.BasicState
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import kotlin.math.abs

// Credits to <https://github.com/UnclaimedBloom6/BloomModule/blob/main/features/TerminalSolvers.js>
// this fk noob
object TerminalSolvers : Feature(
    "terminalSolvers",
    "Shows the correct slots to click to solve the current terminal",
    Categories.DUNGEONS,
    "catacombs",
    subcategory = "Terminals"
) {
    override fun createRequirements(): List<BasicState<Boolean>?> {
        return super.createRequirements() + listOf(Stages.Terminals.isActiveState)
    }

    private val SETTING_DISABLE_TOOLTIP = addSwitch(
        "disableTooltip",
        true,
        "Disables the tooltip whenever inside a terminal",
        "Disable Terminal Tooltip"
    )
    private val SETTING_CORRECT_COLOR = addColorPicker(
        "correctColor",
        0xFF4F8F2F.toInt(),
        "The correct color for terminal solver",
        "Terminal Solver Correct Color"
    )
    private val SETTING_SECOND_CORRECT_COLOR = addColorPicker(
        "secondCorrectColor",
        0xFFB5A12E.toInt(),
        "The second correct color for terminal solver",
        "Terminal Solver Second Correct Color"
    )
    private val SETTING_THIRD_CORRECT_COLOR = addColorPicker(
        "thirdCorrectColor",
        0xFFB86A2E.toInt(),
        "The third correct color for terminal solver",
        "Terminal Solver Third Correct Color"
    )
    private val SETTING_OTHER_CORRECT_COLOR = addColorPicker(
        "otherColor",
        0xFF8F2E2E.toInt(),
        "The other color for terminal solver",
        "Terminal Solver Other Color"
    )
    val SETTING_CANCEL_WRONG_CLICKS = addSwitch(
        "cancelWrongClicks",
        true,
        "Cancels the wrong clicks inside of Color and StartsWith terminals",
        "Terminal Solver Cancel Clicks"
    )
    val SETTING_HIDE_DONE = addSwitch(
        "hideDone",
        true,
        "Hides the already clicked slot items or the ones that are not a solution",
        "Terminal Solver Hide Complete"
    )
    // make render name for numbers
    val SETTING_RENDER_NUMBERS = addSwitch(
        "renderNumbers",
        true,
        "Whether to render the numbers from Numbers terminal in the slots or not",
        "Terminal Solver Render Numbers"
    )
    private var currentSolver: TerminalData? = null

    data class TerminalSlot(val slot: Int, val itemStack: ItemStack, var blacklisted: Boolean = false)
    data class RubixSlot(val slot: Int, val itemStack: ItemStack, var blacklisted: Boolean = false, var count: Int = 0)

    override fun initialize() {
        on<GuiOpenEvent> { event ->
            val title = event.screen.title.string
            currentSolver = TerminalData.byMatch(title)
        }

        on<GuiCloseEvent> {
            currentSolver = null
        }

        on<TickEvent> {
            currentSolver?.onTick()
        }

        on<RenderSlotEvent> { event ->
            currentSolver?.onRenderSlot(event)
        }

        on<GuiClickEvent> { event ->
            currentSolver?.onClick(event)
        }
    }

    fun color(idx: Int): Int = when (idx) {
        0 -> SETTING_CORRECT_COLOR.get()
        1 -> SETTING_SECOND_CORRECT_COLOR.get()
        2 -> SETTING_THIRD_CORRECT_COLOR.get()
        else -> SETTING_OTHER_CORRECT_COLOR.get()
    }

    fun disableTooltip(): Boolean {
        return currentSolver != null && SETTING_DISABLE_TOOLTIP.get()
    }
}

// TODO: clean this mess up
interface ITerminalSolver {
    fun onTick()

    fun onRenderSlot(event: RenderSlotEvent)

    fun onClick(event: GuiClickEvent) {}
}

enum class TerminalData(val title: Regex) : ITerminalSolver {
    NUMBERS("Click in order!".toRegex()) {
        private var correctSlots = mutableListOf<TerminalSlot>()

        override fun onTick() {
            val screen = minecraft.screen ?: return
            val items = (screen as AbstractContainerScreen<*>).menu.items

            correctSlots.clear()

            for (idx in 0..items.lastIndex) {
                val item = items[idx]
                if (item.item != Items.RED_STAINED_GLASS_PANE) continue
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
            if (data.blacklisted) return

            event.ctx.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, color(idx))
            if (SETTING_RENDER_NUMBERS.get())
                event.ctx.drawString(minecraft.font, "${data.itemStack.count}", slot.x + 4, slot.y + 4, -1)
            event.cancel()
        }
    },
    COLORS("^Select all the (.*?) items!$".toRegex()) {
        // TODO: colors and startswith don't remove the items when clicked because hypixel is very funny
        //  and doesn't enchant them as 1.8 version does
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

            for (idx in 0..items.lastIndex) {
                val item = items.getOrNull(idx) ?: continue
                if (item.isEnchanted) continue
                var name = item.customName?.string ?: item.itemName.string
                for (fixed in fixedColorItems) {
                    if (name.lowercase().startsWith(fixed.key))
                        name = fixed.value
                }
                if (!name.lowercase().startsWith(toFind.lowercase())) continue

                correctSlots.add(TerminalSlot(idx, item))
            }
        }

        override fun onRenderSlot(event: RenderSlotEvent) {
            val slot = event.slot
            if (slot.container == minecraft.player?.inventory) return
            if (slot.item.isEnchanted) return
            val idx = correctSlots.indexOfFirst { it.slot == event.slot.containerSlot }
            if (idx == -1) {
                if (SETTING_HIDE_DONE.get()) event.cancel()
                return
            }
            val data = correctSlots[idx]
            if (data.itemStack.isEnchanted) data.blacklisted = true
            if (data.blacklisted) return

            event.ctx.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, color(0))
            event.cancel()
        }

        override fun onClick(event: GuiClickEvent) {
            if (!SETTING_CANCEL_WRONG_CLICKS.get()) return
            val slot = ScreenUtils.cursorSlot(event.screen) ?: return
            val idx = correctSlots.indexOfFirst { it.slot == slot.containerSlot }
            if (idx != -1) return

            event.cancel()
        }
    },
    STARTS_WITH("^What starts with: '(.*?)'\\?$".toRegex()) {
        private val correctSlots = mutableListOf<TerminalSlot>()

        override fun onTick() {
            val screen = minecraft.screen ?: return
            val toFind = title.matchEntire(screen.title.string)?.groupValues?.drop(1)?.getOrNull(0) ?: return
            val items = (screen as AbstractContainerScreen<*>).menu.items

            correctSlots.clear()

            for (idx in 0..items.lastIndex) {
                val item = items.getOrNull(idx) ?: continue
                if (item.isEnchanted) continue
                val name = item.customName?.string ?: item.itemName.string
                if (!name.lowercase().startsWith(toFind.lowercase())) continue

                correctSlots.add(TerminalSlot(idx, item))
            }
        }

        override fun onRenderSlot(event: RenderSlotEvent) {
            val slot = event.slot
            if (slot.container == minecraft.player?.inventory) return
            if (slot.item.isEnchanted) return
            val idx = correctSlots.indexOfFirst { it.slot == event.slot.containerSlot }
            if (idx == -1) {
                if (SETTING_HIDE_DONE.get()) event.cancel()
                return
            }
            val data = correctSlots[idx]
            if (data.itemStack.isEnchanted) data.blacklisted = true
            if (data.blacklisted) return

            event.ctx.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, color(0))
            event.cancel()
        }

        override fun onClick(event: GuiClickEvent) {
            if (!SETTING_CANCEL_WRONG_CLICKS.get()) return
            val slot = ScreenUtils.cursorSlot(event.screen) ?: return
            val idx = correctSlots.indexOfFirst { it.slot == slot.containerSlot }
            if (idx != -1) return

            event.cancel()
        }
    },
    RUBIX("^Change all to same color!$".toRegex()) {
        private val rubixIndices = listOf(12, 13, 14, 21, 22, 23, 30, 31, 32)
        private val rubixOrder = listOf(
            Items.ORANGE_STAINED_GLASS_PANE,
            Items.YELLOW_STAINED_GLASS_PANE,
            Items.GREEN_STAINED_GLASS_PANE,
            Items.BLUE_STAINED_GLASS_PANE,
            Items.RED_STAINED_GLASS_PANE,
        )
        private var correctSlots = mutableListOf<TerminalSolvers.RubixSlot>()

        override fun onTick() {
            val screen = minecraft.screen ?: return
            val items = (screen as AbstractContainerScreen<*>).menu.items
            val slotsIn = mutableListOf<TerminalSolvers.RubixSlot>()

            for (idx in rubixIndices) {
                val item = items.getOrNull(idx) ?: continue
                if (!rubixOrder.any { it == item.item }) continue
                slotsIn.add(TerminalSolvers.RubixSlot(idx, item))
            }

            correctSlots.clear()

            var leastClicks = -1

            for (jdx in 0..rubixOrder.lastIndex) {
                var clicks = 0
                val leftClicks = mutableListOf<TerminalSolvers.RubixSlot>()

                for (slot in slotsIn) {
                    val idx = rubixOrder.indexOfFirst { it == slot.itemStack.item }
                    if (idx == -1) continue
                    val distance =
                        if (idx < jdx)
                            jdx - idx
                        else
                            jdx + rubixOrder.size - idx

                    clicks += if (distance > 2) abs(distance - 5) else distance

                    slot.count = if (distance > 2) distance - 5 else distance
                    leftClicks.add(slot)
                }

                if (leastClicks == -1 || clicks < leastClicks) {
                    leastClicks = clicks
                    correctSlots = leftClicks
                }
            }
        }

        override fun onRenderSlot(event: RenderSlotEvent) {
            val slot = event.slot
            if (slot.container == minecraft.player?.inventory) return
            if (slot.item.isEnchanted) return
            val idx = correctSlots.indexOfFirst { it.slot == event.slot.containerSlot }
            if (idx == -1) return
            val data = correctSlots[idx]
            if (data.blacklisted) return
            if (data.count == 0) return

            event.ctx.drawString(minecraft.font, "${data.count}", slot.x + 4, slot.y + 5, -1)
        }
    },
    RED_GREEN("^Correct all the panes!$".toRegex()) {
        private val correctSlots = mutableListOf<TerminalSlot>()

        override fun onTick() {
            val screen = minecraft.screen ?: return
            val items = (screen as AbstractContainerScreen<*>).menu.items

            correctSlots.clear()

            for (idx in 0..items.lastIndex) {
                val item = items[idx]
                if (item.item != Items.RED_STAINED_GLASS_PANE) continue
                correctSlots.add(TerminalSlot(idx, item))
            }
        }

        override fun onRenderSlot(event: RenderSlotEvent) {
            val slot = event.slot
            if (slot.container == minecraft.player?.inventory) return
            if (slot.item.isEnchanted) return
            val idx = correctSlots.indexOfFirst { it.slot == event.slot.containerSlot }
            if (idx == -1) {
                if (SETTING_HIDE_DONE.get()) event.cancel()
                return
            }
            val data = correctSlots[idx]
            if (data.blacklisted) return

            event.ctx.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, color(0))
            event.cancel()
        }
    };

    companion object {
        fun byMatch(string: String) = TerminalData.entries.find { it.title.matches(string) }
    }
}