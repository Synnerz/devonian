package com.github.synnerz.devonian.features.misc.inventory

import com.github.synnerz.devonian.api.events.*
import com.github.synnerz.devonian.features.Feature
import kotlinx.atomicfu.atomic

object NoCursorReset : Feature(
    "noCursorReset",
    "Avoids resetting your cursor whenever navigating guis",
    subcategory = "Inventory",
) {
    private var lastContainer = atomic(-1)
    private var lastClose = atomic(-1)

    override fun initialize() {
        on<ServerContainerOpenEvent> { event ->
            if (EventBus.serverTicks() - lastClose.value < 2) lastContainer.value = EventBus.serverTicks()
        }

        on<ServerContainerCloseEvent> {
            lastContainer.value = -1
            lastClose.value = EventBus.serverTicks()
        }

        on<ClientContainerCloseEvent> {
            lastContainer.value = -1
        }
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        lastContainer.value = -1
        lastClose.value = -1
    }

    fun shouldReset(): Boolean {
        if (!isEnabled()) return true
        return lastContainer.value == -1 || lastClose.value == -1 || EventBus.serverTicks() - lastClose.value < 0 || EventBus.serverTicks() - lastContainer.value > 3
    }
}