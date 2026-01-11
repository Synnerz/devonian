package com.github.synnerz.devonian.features.misc.inventory

import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.features.Feature

object NoCursorReset : Feature(
    "noCursorReset",
    "Avoids resetting your cursor whenever navigating guis",
    subcategory = "Inventory",
) {
    override fun initialize() {
    }

    override fun onWorldChange(event: WorldChangeEvent) {
    }

    fun shouldReset(): Boolean {
        return true
    }
}
