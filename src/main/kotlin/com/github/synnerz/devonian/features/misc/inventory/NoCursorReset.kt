package com.github.synnerz.devonian.features.misc.inventory

import com.github.synnerz.devonian.api.events.*
import com.github.synnerz.devonian.features.Feature

object NoCursorReset : Feature(
    "noCursorReset",
    "Avoids resetting your cursor whenever navigating guis.",
    subcategory = "Inventory",
    searchTags = setOf("keep"),
) {
    private var lastOpenTick = -1
    private var lastOpenTime = 0L
    private var lastClose = -1
    @JvmField
    var ignoreFirstBatch = 0

    override fun initialize() {
        on<ServerContainerOpenEvent> {
            if (EventBus.serverTicks() - lastClose < 2) {
                lastOpenTick = EventBus.serverTicks()
                lastOpenTime = System.currentTimeMillis()
            }
        }

        on<ServerContainerCloseEvent> {
            lastClose =  EventBus.serverTicks()
        }

        on<ClientContainerCloseEvent> {
            lastOpenTick = -1
        }
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        lastOpenTick = -1
        lastClose = -1
    }

    fun shouldReset(): Boolean {
        if (!isEnabled()) return true
        return lastOpenTick == -1 ||
            EventBus.serverTicks() - lastOpenTick > 3 ||
            System.currentTimeMillis() - lastOpenTime > 500L
    }
}