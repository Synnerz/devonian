package com.github.synnerz.devonian.features

import com.github.synnerz.devonian.hud.texthud.TextHudFeature

object HudManagerHider : TextHudFeature("hudManagerHider") {
    override fun getEditText(): List<String> = listOf("Hide &cDisabled&r HUDs")
}