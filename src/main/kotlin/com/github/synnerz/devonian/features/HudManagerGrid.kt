package com.github.synnerz.devonian.features

import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.hud.texthud.TextHudFeature

object HudManagerGrid : TextHudFeature(
    "hudManagerGrid",
    "Enables snapping to a grid in the HUD editor.",
    Categories.GLOBAL,
    displayName = "Snap HUDs to Grid",
    subcategory = "Mod",
    isInternal = true,
) {
    override fun getEditText(): List<String> =
        if (isEnabled()) listOf(
            "Grid: &aEnabled",
            "Tip: scale me",
        )
        else listOf("Grid: &cDisabled")

    override fun setDefaultValues() {
        super.setDefaultValues()

        x = window.guiScaledWidth * 3.0 / 4.0
        y = window.guiScaledHeight / 4.0
    }
}