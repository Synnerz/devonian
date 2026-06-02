package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.devonian.api.ItemUtils
import com.github.synnerz.devonian.api.Location
import com.github.synnerz.devonian.api.Scheduler
import com.github.synnerz.devonian.api.ScreenUtils
import com.github.synnerz.devonian.api.SkyblockPrices
import com.github.synnerz.devonian.api.events.ClientContainerCloseEvent
import com.github.synnerz.devonian.api.events.GuiClickEvent
import com.github.synnerz.devonian.api.events.PostRenderGuiEvent
import com.github.synnerz.devonian.api.events.RenderOverlayEvent
import com.github.synnerz.devonian.api.events.ServerContainerCloseEvent
import com.github.synnerz.devonian.api.events.ServerContainerOpenEvent
import com.github.synnerz.devonian.api.events.ServerContainerSetContentEvent
import com.github.synnerz.devonian.api.events.ServerContainerSetSlotEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.hud.texthud.TextHudFeature
import com.github.synnerz.devonian.utils.BasicState
import com.github.synnerz.devonian.utils.StringUtils
import com.github.synnerz.devonian.utils.StringUtils.clearCodes
import com.github.synnerz.devonian.utils.StringUtils.colorCodes
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.item.Items
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.roundToInt

object ChestProfit : TextHudFeature(
    "chestProfit",
    "Displays the amount of profit that is in the dungeon chests you currently have opened (only works inside dungeons).",
    Categories.DUNGEONS,
    subcategory = "HUD",
) {
    override fun createRequirements(): List<BasicState<Boolean>?> {
        return super.createRequirements() + listOf(
            Location.stateInArea("catacombs")
                .zip(Location.stateInArea("dungeon hub"), Boolean::or)
        )
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
    private val chestNames = setOf(
        "Wood",
        "Gold",
        "Diamond",
        "Emerald",
        "Obsidian",
        "Bedrock",
    )
    private val essenceRegex = "^(Wither|Undead) Essence x(\\d+)$".toRegex()
    private val blockSound = SoundEvents.NOTE_BLOCK_BASS.value()
    val currentChestData = ConcurrentHashMap<String, ChestData>().apply {
        put("Wood", ChestData("&fWood Chest&r"))
        put("Gold", ChestData("&6Gold Chest&r"))
        put("Diamond", ChestData("&bDiamond Chest&r"))
        put("Emerald", ChestData("&2Emerald Chest&r"))
        put("Obsidian", ChestData("&5Obsidian Chest&r"))
        put("Bedrock", ChestData("&8Bedrock Chest&r"))
    }
    var inChest = false
    var currentChest: String? = null
    var openedChest = false

    data class ItemData(
        val itemName: String,
        val sbId: String,
        val amount: Int,
        val isEssence: Boolean
    ) {
        fun price(): Int {
            if (isEssence && !SETTING_USE_ESSENCE_PROFIT.get()) return 0
            return SkyblockPrices.buyPrice(sbId).roundToInt() * amount
        }
    }

    data class ChestData(
        val name: String, // formatted name
        val itemData: MutableList<ItemData> = mutableListOf(),
        var chestPrice: Int = 0
    ) {
        fun profit(): Int {
            val totalPrices = itemData.sumOf { it.price() }
            return totalPrices - chestPrice
        }
    }

    override fun initialize() {
        on<ServerContainerOpenEvent> { event ->
            val name = event.titleStr
            inChest = chestNames.contains(name)
            if (inChest) openedChest = true
            currentChest = if (inChest) name else null

            if (!inChest) return@on
            val data = currentChestData[name] ?: return@on
            data.chestPrice = 0

            Scheduler.scheduleTask {
                data.itemData.clear()
                updateDisplay()
            }
        }

        on<ServerContainerCloseEvent> {
            if (inChest && Location.area == "dungeon hub") {
                reset()
                Scheduler.scheduleTask {
                    clearLines()
                }
            }
            inChest = false
        }

        on<ClientContainerCloseEvent> {
            if (inChest && Location.area == "dungeon hub") {
                clearLines()
                reset()
            }
            inChest = false
        }

        on<ServerContainerSetSlotEvent> { event ->
            if (!inChest) return@on

            val slot = event.slot
            if (slot == 31) {
                val currentData = currentChestData[currentChest!!] ?: return@on
                val chestItem = event.itemStack
                if (chestItem.item != Items.CHEST) return@on
                val chestLore = ItemUtils.lore(chestItem) ?: return@on
                var chestPrice = 0
                val costIdx = chestLore.indexOf("Cost")
                if (costIdx == -1) return@on

                chestLore[costIdx + 1].replace("(,+| Coins)".toRegex(), "").also {
                    chestPrice +=
                        if (it.isEmpty() || !"\\d+".toRegex().matches(it)) 0
                        else it.toInt()
                }
                chestLore[costIdx + 2].also {
                    if (it != "Dungeon Chest Key") return@also
                    chestPrice += SkyblockPrices.buyPrice("DUNGEON_CHEST_KEY").roundToInt()
                }

                currentData.chestPrice = chestPrice

                return@on
            }

            if (slot !in 9..17) return@on
            val itemStack = event.itemStack
            if (
                itemStack.item == Items.STAINED_GLASS_PANE.black ||
                itemStack.item == Items.STAINED_GLASS_PANE.gray ||
                itemStack.isEmpty
            ) return@on

            val customName = itemStack.customName ?: return@on
            val customNameStr = customName.string
            val isEnchantedBook = customNameStr == "Enchanted Book"
            val itemName =
                if (isEnchantedBook)
                    ItemUtils.lore(itemStack, true)?.getOrNull(2) ?: return@on
                else
                    customName.colorCodes()
            var sbId = ItemUtils.skyblockId(itemStack)
            var amount = 1
            if (sbId == null && itemName.contains(" Essence ")) {
                val match = essenceRegex.matchEntire(itemName.clearCodes())?.groupValues?.drop(1) ?: return@on
                sbId = "ESSENCE_${match[0].uppercase()}"
                amount = match[1].toInt()
            }

            if (sbId == null) return@on
            val currentData = currentChestData[currentChest!!] ?: return@on

            Scheduler.scheduleTask {
                currentData.itemData.add(ItemData(
                    itemName,
                    sbId,
                    amount,
                    itemName.contains(" Essence ")
                ))
            }
        }

        on<ServerContainerSetContentEvent> {
            Scheduler.scheduleTask {
                updateDisplay()
            }
        }

        on<RenderOverlayEvent> {
            if (!openedChest) return@on
            if (inChest) return@on
            draw(it.ctx)
        }

        on<GuiClickEvent> { event ->
            val slot = ScreenUtils.cursorSlot(event.screen) ?: return@on
            if (slot.container == minecraft.player?.inventory || slot.index != 50) return@on
            if (currentChest == null || !SETTING_BLOCK_KISMET.get()) return@on
            val chestData = currentChestData[currentChest] ?: return@on
            val limit = SETTING_KISMET_THRESHOLD.get() * 1_000_000
            if (chestData.profit() < limit) return@on

            minecraft.level?.playPlayerSound(
                blockSound,
                SoundSource.MASTER,
                1f, 0.5f,
            )
            event.cancel()
        }

        on<PostRenderGuiEvent> {
            if (!openedChest) return@on
            draw(it.ctx)
        }
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        inChest = false
        currentChest = null
        openedChest = false
        clearLines()
        reset()
    }

    // i was too lazy to make actual example
    override fun getEditText(): List<String> = listOf("&5Obsidian Chest", "&aA", "&aLife", "&bProfit&f: &a100")

    private fun reset() {
        for (data in currentChestData) {
            data.value.itemData.clear()
            data.value.chestPrice = 0
        }
    }

    private fun updateDisplay() {
        clearLines()
        if (currentChest == null && Location.area == "dungeon hub") {
            reset()
            return
        }

        for (data in currentChestData.entries.toList().sortedByDescending { it.value.profit() }) {
            val v = data.value
            val items = v.itemData
            if (items.isEmpty()) continue
            addLine(v.name)
            if (!SETTING_COMPACT_MODE.get())
                addLines(items.map { "  ${it.itemName}  " })
            val profit = v.profit()
            addLine("&bProfit&f: ${if (profit < 0) "&c" else "&a"}${StringUtils.addCommas(profit)}")
        }
    }
}