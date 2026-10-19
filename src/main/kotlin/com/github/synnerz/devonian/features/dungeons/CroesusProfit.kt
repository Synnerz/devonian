package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.devonian.api.dungeon.CroesusListener
import com.github.synnerz.devonian.api.events.*
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.hud.texthud.TextHudFeature
import com.github.synnerz.devonian.utils.StringUtils
import com.github.synnerz.devonian.utils.render.Render2D
import com.github.synnerz.devonian.utils.render.Render2D.width
import kotlinx.atomicfu.atomic
import java.awt.Color

object CroesusProfit : TextHudFeature(
    "croesusProfit",
    "Shows the profit of the current chest(s) opened in croesus.",
    Categories.DUNGEONS,
    "Dungeon Hub",
    subcategory = "HUD",
) {
    private val SETTING_USE_ESSENCE_PROFIT = addSwitch(
        "useEssenceProfit",
        true,
        "Whether to use Wither/Undead essence for profit.",
        "Croesus Profit Essence",
    )
    private val SETTING_COMPACT_MODE = addSwitch(
        "compactMode",
        false,
        "Compact mode stops showing the entire item list and only shows the chest name with its profit.",
        "Croesus Profit Compact Mode",
    )

    private val PROFITABLE_COLOR = Color.GREEN.rgb
    private val SECOND_PROFITABLE_COLOR = Color.YELLOW.rgb

    private var chests = emptyList<CroesusListener.RunChest>()
    private var inChest = false
    private var updateLines = atomic(false)
    private var mostProfitable = -1
    private var secondMostProfitable = -1

    // lazy part 2
    override fun getEditText(): List<String> = listOf("&fCroesus Profit", "&5Obsidian Chest", "&aA", "&aLife", "&bProfit&f: &a100")

    override fun initialize() {
        on<CroesusListener.ClosedRunMenu> {
            reset()
        }

        on<CroesusListener.LoadedRunChests> { event ->
            val c = event.chests.sortedByDescending { it.chest.totalProfit(!SETTING_USE_ESSENCE_PROFIT.get()) }
            chests = c
            mostProfitable = c.getOrNull(0)?.slot ?: -1
            secondMostProfitable = c.getOrNull(1)?.slot ?: -1
            updateLines.value = true
            inChest = true
        }

        on<PostRenderGuiEvent> { event ->
            if (!inChest) return@on

            if (updateLines.getAndSet(false)) updateDisplay()

            draw(event.ctx)
        }

        on<RenderSlotEvent> { event ->
            if (!inChest) return@on

            val slot = event.slot
            if (event.isInventory()) return@on

            val color = when (slot.containerSlot) {
                mostProfitable -> PROFITABLE_COLOR
                secondMostProfitable -> SECOND_PROFITABLE_COLOR
                else -> return@on
            }

            event.ctx.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, color)
        }.prio = -1

        on<PostRenderSlotsEvent> { event ->
            if (!inChest) return@on

            val slots = event.container.menu.slots

            chests.forEach { (chest, slot) ->
                val slot = slots.getOrNull(slot) ?: return@forEach
                if (!chest.state.canOpen) return@forEach
                val profit = chest.totalProfit()

                val str = StringUtils.shortenNumber(profit)
                val color = when {
                    chest.items.any { it.blacklisted } -> "§4"
                    profit <= 0 -> "§c"
                    profit <= 500_000 -> "§e"
                    profit <= 10_000_000 -> "§a"
                    else -> "§z"
                }
                val w = str.width()
                Render2D.drawString(
                    event.ctx,
                    "$color$str",
                    slot.x + 8 - w / 4,
                    slot.y + 20,
                    0.5f,
                )
            }
        }
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        reset()
    }

    private fun reset() {
        inChest = false
        mostProfitable = -1
        secondMostProfitable = -1
        invalidateLines()
    }

    private fun updateDisplay() {
        clearLines()
        for ((chest, _) in chests) {
            if (!chest.state.canOpen) continue

            val items = chest.items
            if (items.isEmpty()) continue

            val profit = chest.totalProfit()
            val profitStr = "${if (profit < 0) "&c" else "&a"}${StringUtils.addCommas(profit)}"
            val dkeySymbol = if (chest.state.requiresKey) "&9*" else ""

            if (SETTING_COMPACT_MODE.get()) {
                addLine("${chest.type.displayName}&f: $dkeySymbol$profitStr")
            } else {
                addLine("${chest.type.displayName}&f: $profitStr &7($dkeySymbol&7${StringUtils.addCommasTruncate(chest.openCost)}&7)")
                addLines(items.map { "  ${it.displayName}  " })
            }
        }
    }
}