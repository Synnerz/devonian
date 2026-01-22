package com.github.synnerz.devonian.features.misc

import com.github.synnerz.devonian.api.events.SelectedItemRenderEvent
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.hud.texthud.TextHudFeature
import com.github.synnerz.devonian.utils.StringUtils.colorCodes

object SelectedItemNameRender : Feature(
    "selectedItemNameRender",
    "Does not render a custom hud.",
    displayName = "HideSelectedItemName",
    subcategory = "Tweaks"
) {
    override fun initialize() {
        on<SelectedItemRenderEvent> { event ->
            event.cancel()
        }
    }
}

object SelectedItemName : TextHudFeature(
    "selectedItemName",
    "Replaces the selected item name with a custom HUD.",
    subcategory = "Tweaks",
) {
    override fun initialize() {
        on<SelectedItemRenderEvent> { event ->
            if (SelectedItemNameRender.isEnabled() || event.isCancelled()) return@on
            event.cancel()

            // TODO: if possible, make fade in animation, it is done simply by adjusting the "l" as alpha to color
            val item = minecraft.player!!.mainHandItem!!
            val name = item.customName?.colorCodes() ?: item.itemName.string
            setLine(name)
            draw(event.ctx)
        }
    }

    override fun getEditText(): List<String> = listOf("&cDungeonbreaker")
}