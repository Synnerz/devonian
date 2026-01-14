package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.devonian.api.ItemUtils
import com.github.synnerz.devonian.api.events.*
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import net.minecraft.world.item.Items
import java.awt.Color
import java.util.concurrent.CopyOnWriteArraySet

object CroesusHighlightUnopened : Feature(
    "croesusHighlightUnopened",
    "Highlights the chests in croesus that are not yet opened",
    Categories.DUNGEONS,
    "Dungeon Hub",
    subcategory = "QOL"
) {
    private val SETTING_HIDE_BLACKLISTED = addSwitch(
        "hideBlacklisted",
        false,
        "Hides the croesus chests that have been opened",
        "Croesus Highlight Blacklist"
    )
    private var inCroesus = false
    private val whitelist = CopyOnWriteArraySet<Int>()
    private val blacklist = CopyOnWriteArraySet<Int>()

    override fun initialize() {
        on<ServerContainerOpenEvent> { event ->
            inCroesus = event.titleStr == "Croesus"
        }

        on<ServerContainerCloseEvent> {
            blacklist.clear()
            whitelist.clear()
        }

        on<ClientContainerCloseEvent> {
            blacklist.clear()
            whitelist.clear()
        }

        on<ServerContainerSetSlotEvent> { event ->
            if (event.slot == 0) {
                blacklist.clear()
                whitelist.clear()
            }
            if (event.slot > 45) return@on

            val itemStack = event.itemStack
            if (itemStack.item != Items.PLAYER_HEAD) return@on

            val lore = ItemUtils.lore(itemStack) ?: return@on
            for (line in lore) {
                if (line.contains("Opened Chest: ")) {
                    blacklist.add(event.slot)
                    continue
                }
                if (line != "No chests opened yet!") continue

                whitelist.add(event.slot)
            }
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