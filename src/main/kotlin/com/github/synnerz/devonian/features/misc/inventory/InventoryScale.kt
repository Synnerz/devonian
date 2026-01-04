package com.github.synnerz.devonian.features.misc.inventory

import com.github.synnerz.devonian.api.events.TickEvent
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.features.dungeons.CustomTerminalScale
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.inventory.ContainerScreen
import net.minecraft.client.gui.screens.inventory.InventoryScreen
import kotlin.math.roundToInt

object InventoryScale : Feature(
    "inventoryScale",
    "Changes the scale of inventory related guis whenever opening them",
    subcategory = "Inventory"
) {
    private val SETTING_SCALE = addSlider(
        "scale",
        1.0,
        0.0, 5.0,
        "0 = auto",
        "Inventory Scale Amount"
    )
    private var lastScreen: Screen? = null
    private var oldScale = -1

    override fun initialize() {
        on<TickEvent> {
            val screen = minecraft.screen
            if (lastScreen == null && screen != null) {
                if (screen is ContainerScreen || screen is InventoryScreen) {
                    val title = screen.title.string
                    if (!CustomTerminalScale.shouldScale(title)) {
                        val guiScale = minecraft.options.guiScale()

                        oldScale = guiScale.get()
                        guiScale.set(SETTING_SCALE.get().roundToInt())
                    }
                }
                lastScreen = screen
                return@on
            }

            if (lastScreen != null && screen != null && screen != lastScreen) {
                if (screen is ContainerScreen || screen is InventoryScreen) {
                    val title = screen.title.string
                    if (!CustomTerminalScale.shouldScale(title) && oldScale == -1) {
                        val guiScale = minecraft.options.guiScale()

                        oldScale = guiScale.get()
                        guiScale.set(SETTING_SCALE.get().roundToInt())
                    }
                }
                lastScreen = screen
                return@on
            }

            if (lastScreen != null && screen == null) {
                if (lastScreen is ContainerScreen || lastScreen is InventoryScreen) {
                    if (oldScale == -1) return@on

                    minecraft.options.guiScale().set(oldScale)
                    oldScale = -1
                } else if (oldScale != -1) {
                    // failsafe to properly reset scale because we hard check for container-like screen
                    // and the player can open book/sign and it'll break this
                    minecraft.options.guiScale().set(oldScale)
                    oldScale = -1
                }
                lastScreen = null
                return@on
            }
        }
    }
}