package com.github.synnerz.devonian.features.misc

import com.github.synnerz.devonian.api.ChatUtils
import com.github.synnerz.devonian.api.events.ActionbarEvent
import com.github.synnerz.devonian.api.events.EventBus
import com.github.synnerz.devonian.api.events.RenderOverlayEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.hud.texthud.TextHudFeature
import com.github.synnerz.devonian.utils.StringUtils.colorCodes

object CustomManaUseHud : TextHudFeature(
    "customManaUseHud",
    "Allows you to move the mana used \"18 Mana (Instant Transmission)\" that appears in action bar (above your hotbar)",
    subcategory = "General"
) {
    // TODO: whenever SecretsHud is enabled it will conflict with this and actually send the mana usage action message
    private val SETTING_ONLY_HIDE = addSwitch(
        "onlyHide",
        false,
        "Hides the action bar mana use as well as the custom hud",
        "Only Hide Mana Use"
    )
    private val manaUseRegex = ".*-(\\d+) Mana \\(([\\w ]+)\\).*".toRegex()
    private val formattedManaUseRegex = " *§b-\\d+ Mana \\(§\\w[\\w ]+§\\w\\) *".toRegex()
    private var gatheredAt = -1

    override fun initialize() {
        on<ActionbarEvent> { event ->
            if (gatheredAt != -1 && EventBus.serverTicks() - gatheredAt > (1.5 / 0.05)) {
                gatheredAt = -1
                clearLines()
            }
            val match = event.matches(manaUseRegex) ?: return@on
            ChatUtils.sendActionbar(event.text.colorCodes().replace(formattedManaUseRegex, "     "))
            if (SETTING_ONLY_HIDE.get()) return@on
            val manaUse = match[0]
            val itemUse = match[1]
            setLine("&b-${manaUse} &7(&e${itemUse}&7)")
            gatheredAt = EventBus.serverTicks()
        }

        on<RenderOverlayEvent> {
            if (SETTING_ONLY_HIDE.get()) return@on
            draw(it.ctx)
        }
    }

    override fun getEditText(): List<String> = listOf("&b-15 &7(&eInstant Transmission&7)")

    override fun onWorldChange(event: WorldChangeEvent) {
        clearLines()
        gatheredAt = -1
    }
}