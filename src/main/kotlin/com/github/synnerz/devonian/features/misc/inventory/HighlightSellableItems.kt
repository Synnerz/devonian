package com.github.synnerz.devonian.features.misc.inventory

import kotlin.jvm.optionals.getOrNull

object HighlightSellableItems : com.github.synnerz.devonian.features.Feature(
    "highlightSellableItems",
    "Highlights sellable items whenever inside ophelia/trades/booster cookie gui",
    subcategory = "Inventory"
) {
    private val SETTING_HIGHLIGHT_COLOR = addColorPicker(
        "highlightColor",
        _root_ide_package_.java.awt.Color.RED.rgb,
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
    private val slotsToHighlight = _root_ide_package_.java.util.concurrent.CopyOnWriteArrayList<Int>()

    override fun initialize() {
        on<com.github.synnerz.devonian.api.events.ServerContainerOpenEvent> { event ->
            val title = event.titleStr
            inSellable = title == "Trades" || title == "Booster Cookie" || title == "Ophelia"
            scan = inSellable
        }

        on<com.github.synnerz.devonian.api.events.ServerContainerCloseEvent> {
            inSellable = false
            scan = false
            _root_ide_package_.com.github.synnerz.devonian.api.Scheduler.scheduleTask { slotsToHighlight.clear() }
        }

        on<com.github.synnerz.devonian.api.events.ClientContainerCloseEvent> {
            inSellable = false
            scan = false
            _root_ide_package_.com.github.synnerz.devonian.api.Scheduler.scheduleTask { slotsToHighlight.clear() }
        }

        on<com.github.synnerz.devonian.api.events.ServerContainerSetContentEvent> { event ->
            if (!scan) return@on

            event.forEach { idx, itemStack ->
                if (idx < 54) return@forEach
                if (itemStack == null || itemStack.isEmpty) return@forEach

                val itemName = itemStack.customName?.string
                if (itemName != null) {
                    if (itemNames.contains(itemName.replace(" x\\d+".toRegex(), ""))) {
                        _root_ide_package_.com.github.synnerz.devonian.api.Scheduler.scheduleTask { slotsToHighlight.add(idx) }
                        return@forEach
                    }
                }

                val extraAttributes = _root_ide_package_.com.github.synnerz.devonian.api.ItemUtils.extraAttributes(itemStack) ?: return@forEach
                if (extraAttributes.getString("id").getOrNull() == "ICE_SPRAY_WAND") return@forEach

                val baseStat = extraAttributes.getInt("baseStatBoostPercentage")

                if (!baseStat.isPresent || baseStat.get() == 50) return@forEach
                if (extraAttributes.getInt("upgrade_level").isPresent) return@forEach

                _root_ide_package_.com.github.synnerz.devonian.api.Scheduler.scheduleTask { slotsToHighlight.add(idx) }
            }

            scan = false
        }

        on<com.github.synnerz.devonian.api.events.PacketReceivedEvent> { event ->
            val packet = event.packet
            if (packet !is net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket) return@on
            val slot = packet.slot
            val itemStack = packet.item

            if (!slotsToHighlight.contains(slot)) return@on
            if (!itemStack.isEmpty) return@on

            _root_ide_package_.com.github.synnerz.devonian.api.Scheduler.scheduleTask { slotsToHighlight.remove(slot) }
        }

        on<com.github.synnerz.devonian.api.events.RenderSlotEvent> { event ->
            if (!inSellable) return@on
            val slot = event.slot
            if (!event.isInventory()) return@on
            // i NEED (im lazy) slot.index chick
            if (!slotsToHighlight.contains(slot.index)) return@on

            event.ctx.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, SETTING_HIGHLIGHT_COLOR.get())
        }.prio = 30
    }

    override fun onWorldChange(event: com.github.synnerz.devonian.api.events.WorldChangeEvent) {
        inSellable = false
        scan = false
        slotsToHighlight.clear()
    }
}