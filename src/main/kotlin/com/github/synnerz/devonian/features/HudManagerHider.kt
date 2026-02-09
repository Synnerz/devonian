package com.github.synnerz.devonian.features

import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.hud.texthud.TextHudFeature

object HudManagerHider : TextHudFeature(
    "hudManagerHider",
    "Hides any disabled HUD elements in the HUD Editor (/dv huds).",
    Categories.GLOBAL,
    displayName = "Hide Disabled HUDs",
    subcategory = "Mod",
    isInternal = true,
) {
    override fun getEditText(): List<String> = listOf("Hide &cDisabled&r HUDs")
}