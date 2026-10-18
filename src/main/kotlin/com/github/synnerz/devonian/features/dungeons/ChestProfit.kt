package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.devonian.api.*
import com.github.synnerz.devonian.api.dungeon.CroesusListener
import com.github.synnerz.devonian.api.events.*
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.hud.texthud.TextHudFeature
import com.github.synnerz.devonian.utils.BasicState
import com.github.synnerz.devonian.utils.StringUtils
import kotlinx.atomicfu.atomic
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.item.Items

object ChestProfit : TextHudFeature(
    "chestProfit",
    "Displays the amount of profit that is in the dungeon chests you currently have opened.",
    Categories.DUNGEONS,
    subcategory = "HUD",
) {
    override fun createRequirements(): List<BasicState<Boolean>?> {
        return super.createRequirements() + listOf(Location.stateInArea("catacombs", "dungeon hub"))
    }

    private val SETTING_USE_ESSENCE_PROFIT = addSwitch(
        "useEssenceProfit",
        true,
        "Whether to use Wither/Undead essence for profit.",
        "Chest Profit Essence",
    )
    private val SETTING_COMPACT_MODE = addSwitch(
        "compactMode",
        false,
        "Compact mode stops showing the entire item list and only shows the chest name with its profit.",
        "Chest Profit Compact Mode",
    )
    private val SETTING_BLOCK_KISMET = addSwitch(
        "blockKismet",
        false,
        "Blocks kismet if the profit is above the threshold",
        "Block Kismet"
    )
    private val SETTING_KISMET_THRESHOLD = addSlider(
        "kismetThreshold",
        1.0,
        1.0, 100.0,
        "value will be multiplied by a million (so if 1 = 1m threshold)",
        "Block Kismet Threshold"
    )

    private val blockSound = SoundEvents.NOTE_BLOCK_BASS.value()
    private var inChest = false
    private var openedChest = false
    private var updateLines = atomic(false)
    private var currentChest: CroesusListener.ChestContents? = null
    private var oldChests = emptyList<CroesusListener.ChestContents>()

    // i was too lazy to make actual example
    override fun getEditText(): List<String> = listOf("&5Obsidian Chest", "&aA", "&aLife", "&bProfit&f: &a100")

    override fun initialize() {
        on<CroesusListener.ClosedChestMenu> {
            openedChest = inChest && Location.area == "catacombs"
            reset()
            if (openedChest) updateLines.value = true
        }

        on<CroesusListener.OpenedChestMenu> {
            inChest = true
            openedChest = false
        }

        on<CroesusListener.LoadedChestItems> { event ->
            currentChest = event.chest
            oldChests = (oldChests + event.chest).sortedByDescending {
                it.totalProfit(!SETTING_USE_ESSENCE_PROFIT.get())
            }
            updateLines.value = true
        }

        on<RenderOverlayEvent> {
            if (!openedChest) return@on

            if (updateLines.getAndSet(false)) updateDisplay()

            draw(it.ctx)
        }

        on<PostRenderGuiEvent> {
            if (!inChest) return@on

            if (updateLines.getAndSet(false)) updateDisplay()

            draw(it.ctx)
        }

        on<GuiClickEvent> { event ->
            val chest = currentChest ?: return@on
            val slot = ScreenUtils.cursorSlot(event.screen) ?: return@on
            if (slot.container == minecraft.player?.inventory) return@on
            if (slot.item.item != Items.FEATHER) return@on

            val limit = SETTING_KISMET_THRESHOLD.get() * 1_000_000
            val profit = chest.totalProfit(!SETTING_USE_ESSENCE_PROFIT.get())
            if (profit < limit) return@on

            minecraft.level?.playPlayerSound(
                blockSound,
                SoundSource.MASTER,
                1f, 0.5f,
            )
            event.cancel()
        }.setEnabled(SETTING_BLOCK_KISMET.state)
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        reset()
        openedChest = false
        oldChests = emptyList()
    }

    private fun reset() {
        inChest = false
        currentChest = null
        invalidateLines()
    }

    private fun updateDisplay() {
        clearLines()

        val chest = currentChest
        if (chest != null) addChestToDisplay(chest)
        else oldChests.forEach(::addChestToDisplay)
    }

    private fun addChestToDisplay(chest: CroesusListener.ChestContents) {
        val items = chest.items
        if (items.isEmpty()) return

        addLine(chest.type.displayName)
        if (!SETTING_COMPACT_MODE.get()) addLines(items.map { "  ${it.displayName}  " })
        val profit = chest.totalProfit(!SETTING_USE_ESSENCE_PROFIT.get())
        addLine("&bProfit&f: ${if (profit < 0) "&c" else "&a"}${StringUtils.addCommas(profit)}")
    }
}