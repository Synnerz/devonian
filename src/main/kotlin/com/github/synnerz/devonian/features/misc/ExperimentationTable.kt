package com.github.synnerz.devonian.features.misc

import com.github.synnerz.devonian.api.Scheduler
import com.github.synnerz.devonian.api.ScreenUtils
import com.github.synnerz.devonian.api.events.*
import com.github.synnerz.devonian.features.Feature
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import java.awt.Color

object ExperimentationTable : Feature(
    "experimentationTable",
    "Solvers for experimentation table. only works on metaphysical currently",
    subcategory = "General",
    searchTags = setOf("exp", "table"),
) {
    // TODO: make colors customizable
    private val SETTING_HIDE_TOOLTIP = addSwitch(
        "hideTooltip",
        true,
        "Hides the tooltip while in chrono/ultra",
        "Hide Tooltip"
    )
    private val SETTING_CANCEL_WRONG = addSwitch(
        "cancelWrong",
        true,
        "Cancels the wrong clicks in chrono/ultra",
        "Cancel Wrong"
    )
    private val SETTING_HIDE_WRONG_ULTRA = addSwitch(
        "hideWrongUltra",
        false,
        "Hides the wrong slots in ultrasequence",
        "Hide Wrong Ultra"
    )
    private val SETTING_HIDE_WRONG_CHRONO = addSwitch(
        "hideWrongChrono",
        false,
        "Hides the wrong slots in chronomatron",
        "Hide Wrong Chrono"
    )
    private val superpairsRegex = "^Superpairs \\(\\w+\\)$".toRegex()
    private val chronomatronRegex = "^Chronomatron \\([\\w ]+\\)$".toRegex()
    // TODO: this is only used for debugging, remove once feature is fully done
    private val chronomatronItems = listOf(
        Items.RED_STAINED_GLASS,
        Items.BLUE_STAINED_GLASS,
        Items.LIME_STAINED_GLASS,
        Items.YELLOW_STAINED_GLASS,
        Items.LIGHT_BLUE_STAINED_GLASS,
        Items.PINK_STAINED_GLASS,
        Items.GREEN_STAINED_GLASS,
        Items.CYAN_STAINED_GLASS,
        Items.ORANGE_STAINED_GLASS,
        Items.PURPLE_STAINED_GLASS,
    )
    private val chronomatronHighlightItems = listOf(
        Items.RED_TERRACOTTA,
        Items.BLUE_TERRACOTTA,
        Items.LIME_TERRACOTTA,
        Items.YELLOW_TERRACOTTA,
        Items.LIGHT_BLUE_TERRACOTTA,
        Items.PINK_TERRACOTTA,
        Items.GREEN_TERRACOTTA,
        Items.CYAN_TERRACOTTA,
        Items.ORANGE_TERRACOTTA,
        Items.PURPLE_TERRACOTTA,
    )
    private val ultrasequenceRegex = "^Ultrasequencer \\([\\w ]+\\)$".toRegex()
    private val ultrasequenceNumRegex = "^\\d+$".toRegex()
    private val ultrasequencePanes = listOf(
        Items.WHITE_STAINED_GLASS_PANE,
        Items.ORANGE_STAINED_GLASS_PANE,
        Items.MAGENTA_STAINED_GLASS_PANE,
        Items.LIGHT_BLUE_STAINED_GLASS_PANE,
        Items.YELLOW_STAINED_GLASS_PANE,
        Items.LIME_STAINED_GLASS_PANE,
        Items.PINK_STAINED_GLASS_PANE,
        Items.GRAY_STAINED_GLASS_PANE,
        Items.LIGHT_GRAY_STAINED_GLASS_PANE,
        Items.CYAN_STAINED_GLASS_PANE,
        Items.PURPLE_STAINED_GLASS_PANE,
        Items.BLUE_STAINED_GLASS_PANE,
        Items.BROWN_STAINED_GLASS_PANE,
        Items.GREEN_STAINED_GLASS_PANE,
        Items.RED_STAINED_GLASS_PANE,
        Items.BLACK_STAINED_GLASS_PANE,
    )
    private var inChrono = false
    private var inUltra = false
    private var inSuperpairs = false
    private val chronoSlots = mutableListOf<ChronomatronSlots>()
    private var ultraSlots = mutableListOf<UltraSequenceSlot>()
    private var superpairSlots = MutableList<ItemStack?>(54) { null }
    private var currentSlot49: Item? = null

    // TODO: fix this only working on metaphysical
    enum class ChronomatronSlots(val coloredName: String, val slot1: Int, val slot2: Int, val pitch: Float) {
        RED("&cRed", 11, 20, 0.5555556f),
        PINK("&dPink", 29, 38, 1.1111112f),
        LIME("&aLime", 13, 22, 0.74603176f),
        YELLOW("&eYellow", 14, 23, 0.82539684f),
        ORANGE("&6Orange", 32, 41, 1.6666666f),
        BLUE("&9Blue", 12, 21, 0.61904764f),
        AQUA("&bAqua", 15, 24, 1f),
        PURPLE("&5Purple", 33, 42, 2f),
        CYAN("&3Cyan", 31, 40, 1.4920635f),
        GREEN("&2Green", 30, 39, 1.2539682f),
    }

    data class UltraSequenceSlot(val slot: Int, val count: Int)

    override fun initialize() {
        on<ServerContainerOpenEvent> { event ->
            val title = event.titleStr
            inChrono = title.matches(chronomatronRegex)
            inUltra = title.matches(ultrasequenceRegex)
            inSuperpairs = title.matches(superpairsRegex)
        }

        on<TickEvent> {
            if (!inUltra) return@on
            val screen = minecraft.screen ?: return@on
            val items = (screen as AbstractContainerScreen<*>).menu.items
            val slot49 = items.getOrNull(49) ?: return@on

            if (slot49.item != Items.GLOWSTONE) {
                currentSlot49 = slot49.item
                return@on
            }
            currentSlot49 = slot49.item

            items.forEachIndexed { idx, itemStack ->
                if (idx > 45) return@forEachIndexed
                if (itemStack.isEmpty || ultrasequencePanes.contains(itemStack.item)) return@forEachIndexed
                val name = itemStack.customName?.string ?: return@on
                val match = ultrasequenceNumRegex.matchEntire(name)?.groupValues?.firstOrNull() ?: return@on
                val count = match.toIntOrNull() ?: return@on

                ultraSlots.removeIf { it.slot == idx || it.count == count }
                ultraSlots.add(UltraSequenceSlot(idx, count))
            }

            ultraSlots.sortBy { it.count }
        }

        on<ServerContainerSetSlotEvent> { event ->
            if (inSuperpairs && event.slot < 45) {
                val itemStack = event.itemStack
                if (itemStack.item !in chronomatronItems) {
                    Scheduler.scheduleTask {
                        superpairSlots[event.slot] = itemStack
                    }
                    return@on
                }
                val cache = superpairSlots.getOrNull(event.slot) ?: return@on
                event.cancel()
                Scheduler.scheduleTask {
                    (minecraft.screen as? AbstractContainerScreen<*>)?.menu?.setItem(
                        event.slot,
                        event.stateId,
                        cache
                    )
                }
                return@on
            }
            if (event.slot != 49 || !(inChrono || inUltra)) return@on
            currentSlot49 = event.itemStack.item
        }

        on<SoundPlayEvent> { event ->
            if (event.sound == "minecraft:entity.player.levelup" && (inUltra || inChrono)) {
                Scheduler.scheduleTask { if (inChrono) chronoSlots.clear() else ultraSlots.clear() }
                return@on
            }
            if (inUltra && event.sound == "minecraft:entity.item.pickup" && event.volume == 1f) {
                Scheduler.scheduleTask { ultraSlots.removeFirstOrNull() }
                return@on
            }

            if (!inChrono) return@on
            if (event.sound != "minecraft:block.note_block.pling") return@on

            val data = ChronomatronSlots.entries.find { it.pitch == event.pitch }
            if (currentSlot49 != null && currentSlot49 == Items.CLOCK && data != null) {
                Scheduler.scheduleTask { chronoSlots.removeFirstOrNull() }
                return@on
            }

            if (data != null) {
                Scheduler.scheduleTask { chronoSlots.add(data) }
                return@on
            }
        }

        on<ServerContainerCloseEvent> {
            reset()
        }

        on<ClientContainerCloseEvent> {
            reset()
        }

        on<TooltipRenderEvent> { event ->
            if (!SETTING_HIDE_TOOLTIP.get()) return@on
            if (inUltra || inChrono)
                event.cancel()
        }

        on<GuiClickEvent> { event ->
            // TODO: add failsafe so people can use ctrl to avoid blocking wrong click
            if (!SETTING_CANCEL_WRONG.get()) return@on
            if (!event.state) return@on
            val cursorSlot = ScreenUtils.cursorSlot(event.screen) ?: return@on
            val idx = cursorSlot.containerSlot
            if (idx > 44) return@on

            if (inUltra) {
                if (idx == ultraSlots.firstOrNull()?.slot) return@on
                event.cancel()
            } else if (inChrono) {
                val data = chronoSlots.firstOrNull() ?: return@on
                if (idx != data.slot1 && idx != data.slot2)
                    event.cancel()
            }
        }

        on<RenderSlotEvent> { event ->
            if (event.isInventory()) return@on
            if (inChrono)
                chronomatronRender(event)
            else if (inUltra)
                ultraSequenceRender(event)
        }.prio = 30
    }

    private fun ultraSequenceRender(event: RenderSlotEvent) {
        if (ultraSlots.isEmpty() || currentSlot49 == Items.GLOWSTONE) return
        val slot = event.slot
        val idx = slot.containerSlot
        val jdx = ultraSlots.indexOfFirst { it.slot == idx }
        if (jdx == -1) {
            if (SETTING_HIDE_WRONG_ULTRA.get() && idx != 49)
                event.cancel()
            return
        }
        val data = ultraSlots.getOrNull(jdx) ?: return

        event.cancel()
        event.ctx.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, if (jdx == 0) Color.GREEN.rgb else if (jdx == 1) Color.ORANGE.rgb else Color.RED.rgb)
        event.ctx.drawString(
            minecraft.font,
            "${data.count}",
            slot.x + 4, slot.y + 4, -1
        )
    }

    private fun chronomatronRender(event: RenderSlotEvent) {
        if (chronoSlots.isEmpty() || currentSlot49 == Items.GLOWSTONE) return
        val slot = event.slot
        val idx = slot.containerSlot
        val jdx = chronoSlots.indexOfFirst { it.slot1 == idx || it.slot2 == idx }
        if (jdx == -1) {
            if (SETTING_HIDE_WRONG_CHRONO.get() && idx != 49)
                event.cancel()
            return
        }

        event.ctx.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, if (jdx == 0) Color.GREEN.rgb else if (jdx == 1) Color.ORANGE.rgb else Color.RED.rgb)
    }

    private fun reset() {
        currentSlot49 = null
        inChrono = false
        inUltra = false
        inSuperpairs = false
        chronoSlots.clear()
        ultraSlots.clear()
        superpairSlots.fill(null)
    }
}