package com.github.synnerz.devonian.features.misc

import com.github.synnerz.devonian.api.ItemUtils
import com.github.synnerz.devonian.api.Scheduler
import com.github.synnerz.devonian.api.events.*
import com.github.synnerz.devonian.features.Feature
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket
import java.awt.Color
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.jvm.optionals.getOrNull

object HighlightSellableItems : Feature(
    "highlightSellableItems",
    "Highlights sellable items whenever inside ophelia/trades/booster cookie gui",
    subcategory = "General"
) {
    private val SETTING_HIGHLIGHT_COLOR = addColorPicker(
        "highlightColor",
        Color.RED.rgb,
        "The color to use for highlight sellable items",
        "Sellable Highlight Color"
    )
    // yoink from skytils <https://github.com/Skytils/SkytilsMod/blob/618bc6d5c03fb026ebbd27ed45484d9fc698138a/mod/src/main/kotlin/gg/skytils/skytilsmod/features/impl/misc/ItemFeatures.kt#L222-L232>
    private val itemNames = setOf(
        "Defuse Kit",
        "Lever",
        "Torch",
        "Stone Button",
        "Tripwire Hook",
        "Journal Entry",
        "Training Weights",
        "Mimic Fragment",
        "Healing 8 Splash Potion",
        "Healing VIII Splash Potion",
        "Premium Flesh",
        "Decoy",
        "Trap",
        "Inflatable Jerry"
    )
    private var inSellable = false
    private var scan = false
    private val slotsToHighlight = CopyOnWriteArrayList<Int>()

    override fun initialize() {
        on<ServerContainerOpenEvent> { event ->
            val title = event.titleStr
            inSellable = title == "Trades" || title == "Booster Cookie" || title == "Ophelia"
            scan = inSellable
        }

        on<ServerContainerCloseEvent> {
            inSellable = false
            scan = false
            Scheduler.scheduleTask { slotsToHighlight.clear() }
        }

        on<ClientContainerCloseEvent> {
            inSellable = false
            scan = false
            Scheduler.scheduleTask { slotsToHighlight.clear() }
        }

        on<ServerContainerSetContentEvent> { event ->
            if (!scan) return@on

            event.forEach { idx, itemStack ->
                if (idx < 54) return@forEach
                if (itemStack == null || itemStack.isEmpty) return@forEach

                val itemName = itemStack.customName?.string
                if (itemName != null) {
                    if (itemNames.contains(itemName.replace(" x\\d+".toRegex(), ""))) {
                        Scheduler.scheduleTask { slotsToHighlight.add(idx) }
                        return@forEach
                    }
                }

                val extraAttributes = ItemUtils.extraAttributes(itemStack) ?: return@forEach
                if (extraAttributes.getString("id").getOrNull() == "ICE_SPRAY_WAND") return@forEach

                val baseStat = extraAttributes.getInt("baseStatBoostPercentage")

                if (!baseStat.isPresent || baseStat.get() == 50) return@forEach
                if (extraAttributes.getInt("upgrade_level").isPresent) return@forEach

                Scheduler.scheduleTask { slotsToHighlight.add(idx) }
            }

            scan = false
        }

        on<PacketReceivedEvent> { event ->
            val packet = event.packet
            if (packet !is ClientboundContainerSetSlotPacket) return@on
            val slot = packet.slot
            val itemStack = packet.item

            if (!slotsToHighlight.contains(slot)) return@on
            if (!itemStack.isEmpty) return@on

            Scheduler.scheduleTask { slotsToHighlight.remove(slot) }
        }

        on<RenderSlotEvent> { event ->
            if (!inSellable) return@on
            val slot = event.slot
            if (!event.isInventory()) return@on
            // i NEED (im lazy) slot.index chick
            if (!slotsToHighlight.contains(slot.index)) return@on

            event.ctx.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, SETTING_HIGHLIGHT_COLOR.get())
        }.prio = 30
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        inSellable = false
        scan = false
        slotsToHighlight.clear()
    }
}