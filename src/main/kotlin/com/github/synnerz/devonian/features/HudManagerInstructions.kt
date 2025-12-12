package com.github.synnerz.devonian.features

import com.github.synnerz.devonian.hud.texthud.TextHudFeature

object HudManagerInstructions : TextHudFeature("hudManagerInstructions", isInternal = true) {
    override fun getEditText(): List<String> = listOf(
        "&lLeft Click&r to select an element.",
        "&lRight Click&r to toggle an element.",
        "&lDrag&r to move an element.",
        "Alternatively, use the &lArrow Keys&r.",
        "&lScroll&r to resize an element.",
        "Alternatively, use the &l-&r/&l+ Keys.",
        "For Text Elements ONLY:",
        "&l1&r to change the anchor.",
        "&l2&r to change the alignment.",
        "&l3&r to toggle the text shadow.",
        "&l4&r to change the backdrop."
    )
}