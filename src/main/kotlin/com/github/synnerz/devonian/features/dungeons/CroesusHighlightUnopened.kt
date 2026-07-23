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
    private val SETTING_HIGHLIGHT_USED_KISMET = addSwitch(
        "highlightUsedKismet",
        true,
        "Highlights the chests of which you've already used a kismet on as orange",
        "Highlight Used Kismet"
    )
    private val whitelist = mutableListOf<Int>()
    private val blacklist = mutableListOf<Int>()
    private val kismetList = mutableListOf<Int>()

    override fun initialize() {
        on<CroesusListener.CroesusChestSet> { event ->
            val data = event.data
            if (!data.canKismet && SETTING_HIGHLIGHT_USED_KISMET.get())
                kismetList.add(data.slot)
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
            kismetList.clear()
        }

        on<CroesusListener.ClosedCroesus> {
            whitelist.clear()
            blacklist.clear()
            kismetList.clear()
        }

        on<RenderSlotEvent> { event ->
            val slot = event.slot
            if (slot.container == minecraft.player?.inventory) return@on
            val title = event.screen.title.string
            if (!CroesusListener.croesusTitleRegex.matches(title)) return@on
            if (SETTING_HIDE_BLACKLISTED.get() && blacklist.contains(slot.containerSlot)) {
                event.cancel()
                return@on
            }
            val color = when {
                kismetList.contains(slot.containerSlot) -> Color.ORANGE.rgb
                whitelist.contains(slot.containerSlot) -> Color.GREEN.rgb
                else -> return@on
            }

            event.ctx.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, color)
        }
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        whitelist.clear()
        blacklist.clear()
        kismetList.clear()
    }
}