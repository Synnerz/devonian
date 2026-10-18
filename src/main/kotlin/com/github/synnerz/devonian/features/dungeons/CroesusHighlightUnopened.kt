package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.devonian.api.dungeon.CroesusListener
import com.github.synnerz.devonian.api.events.RenderSlotEvent
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
    private val SETTING_USE_DKEY = addSwitch(
        "useDKey",
        false,
        "whether to highlight runs with one chest opened",
        "Croesus Use Dungeon Keys",
    )

    @Volatile
    private var chestStates = arrayOfNulls<CroesusListener.ChestActionable>(54)

    override fun initialize() {
        on<CroesusListener.LoadedCroesusMenu> { event ->
            val arr = arrayOfNulls<CroesusListener.ChestActionable>(54)

            event.runs.forEach { run ->
                val slot = run.slot
                if (slot !in 0 .. 53) return@on

                arr[slot] = run.state
            }

            chestStates = arr
        }

        on<CroesusListener.ClosedCroesusMenu> {
            chestStates = arrayOfNulls(54)
        }

        on<RenderSlotEvent> { event ->
            if (event.isInventory()) return@on

            val slot = event.slot
            val state = chestStates.getOrNull(slot.containerSlot) ?: return@on

            val wantOpen = state.canOpen && (!state.requiresKey || SETTING_USE_DKEY.get())
            if (!wantOpen && SETTING_HIDE_BLACKLISTED.get()) {
                event.cancel()
                return@on
            }

            val color = when {
                !state.canKismet && SETTING_HIGHLIGHT_USED_KISMET.get() -> Color.ORANGE.rgb
                wantOpen -> Color.GREEN.rgb
                else -> return@on
            }

            event.ctx.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, color)
        }.setEnabled(CroesusListener.stateInCroesus)
    }
}