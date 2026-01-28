package com.github.synnerz.devonian.features

import com.github.synnerz.devonian.hud.HudManager
import com.github.synnerz.devonian.hud.texthud.TextHudFeature

object HudManagerName : TextHudFeature("hudManagerName", isInternal = true) {
    override fun getEditText(): List<String> = listOf(
        HudManager.selectedHud?.displayName ?: "<None>"
    )
}