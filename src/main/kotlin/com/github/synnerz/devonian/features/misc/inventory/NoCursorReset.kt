package com.github.synnerz.devonian.features.misc.inventory

import com.github.synnerz.devonian.api.events.*
import com.github.synnerz.devonian.features.Feature

object NoCursorReset : Feature(
    "noCursorReset",
    "Avoids resetting your cursor whenever navigating guis",
    subcategory = "Inventory",
) {
    private var lastOpen = -1
    private var lastClose = -1

    override fun initialize() {
        on<ServerContainerOpenEvent> {
            if (EventBus.serverTicks() - lastClose < 2) lastOpen = EventBus.serverTicks()
        }

        on<ServerContainerCloseEvent> {
            lastClose =  EventBus.serverTicks()
        }

        on<ClientContainerCloseEvent> {
            lastOpen = -1
        }
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        lastOpen = -1
        lastClose = -1
    }

    fun shouldReset(): Boolean {
        if (!isEnabled()) return true
        return lastOpen == -1 || EventBus.serverTicks() - lastOpen > 3
    }
}