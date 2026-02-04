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
    private var lastCloseTick = -1
    private var lastCloseTime = 0L
    @JvmField
    var ignoreFirstBatch = 0

    override fun initialize() {
        on<ServerContainerOpenEvent> {
            val t = System.currentTimeMillis()
            if (
                EventBus.serverTicks() - lastCloseTick < 2 &&
                t - lastCloseTime < 500L
            ) {
                lastOpenTick = EventBus.serverTicks()
                lastOpenTime = t
            }
        }

        on<ServerContainerCloseEvent> {
            lastCloseTick =  EventBus.serverTicks()
            lastCloseTime = System.currentTimeMillis()
        }

        on<ClientContainerCloseEvent> {
            lastOpenTick = -1
            lastOpenTime = 0L
        }
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        lastOpenTick = -1
        lastOpenTime = 0L
        lastCloseTick = -1
        lastCloseTime = 0L
    }

    fun shouldReset(): Boolean {
        if (!isEnabled()) return true
        return lastOpenTick == -1 ||
            EventBus.serverTicks() - lastOpenTick >= 3 ||
            System.currentTimeMillis() - lastOpenTime > 500L
    }
}