package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.devonian.api.dungeon.CroesusListener
import com.github.synnerz.devonian.api.events.*
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import java.awt.Color

object CroesusHighlightUnopened : Feature(
    "croesusHighlightUnopened",
    "Highlights the chests in croesus that are not yet opened.",
    Categories.DUNGEONS,
    "Dungeon Hub",
    subcategory = "QOL",
) {
    private val SETTING_HIDE_BLACKLISTED = addSwitch(
        "hideBlacklisted",
        false,
        "Hides the croesus chests that have been opened.",
        "Croesus Hide Opened",
    )
    private val whitelist = mutableListOf<Int>()
    private val blacklist = mutableListOf<Int>()

    override fun initialize() {
        on<CroesusListener.CroesusChestSet> { event ->
            val data = event.data
            if (data.hasNoChest || data.hasOpened) {
                blacklist.add(data.slot)
                return@on
            }
            if (!data.canOpen) return@on

            whitelist.add(data.slot)
        }

        on<CroesusListener.CroesusPageSwitch> {
            whitelist.clear()
            blacklist.clear()
        }

        on<CroesusListener.ClosedCroesus> {
            whitelist.clear()
            blacklist.clear()
        }

        on<RenderSlotEvent> { event ->
            val slot = event.slot
            if (slot.container == minecraft.player?.inventory) return@on
            val title = event.screen.title.string
            if (title != "Croesus") return@on
            if (SETTING_HIDE_BLACKLISTED.get() && blacklist.contains(slot.containerSlot)) {
                event.cancel()
                return@on
            }
            if (!whitelist.contains(slot.containerSlot)) return@on

            event.ctx.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, Color.GREEN.rgb)
        }
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        blacklist.clear()
        whitelist.clear()
    }
}