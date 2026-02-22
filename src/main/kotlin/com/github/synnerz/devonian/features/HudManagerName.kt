package com.github.synnerz.devonian.features

import com.github.synnerz.devonian.hud.HudManager
import com.github.synnerz.devonian.hud.texthud.TextHudFeature

object HudManagerName : TextHudFeature("hudManagerName", isInternal = true) {
    override fun getEditText(): List<String> = listOf(
        HudManager.selectedHud?.displayName ?: "<None>"
    )

    override fun setDefaultValues() {
        super.setDefaultValues()

        x = window.guiScaledWidth * 3.0 / 4.0
        y = window.guiScaledHeight / 2.0
    }
}