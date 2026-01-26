package com.github.synnerz.devonian.features.misc.inventory

import com.github.synnerz.devonian.Devonian
import com.github.synnerz.devonian.api.ChatUtils
import com.github.synnerz.devonian.api.ItemUtils
import com.github.synnerz.devonian.api.Location
import com.github.synnerz.devonian.api.ScreenUtils
import com.github.synnerz.devonian.api.events.GuiKeyDownEvent
import com.github.synnerz.devonian.api.events.PickupItemInventoryEvent
import com.github.synnerz.devonian.config.Config
import com.github.synnerz.devonian.features.Feature
import com.google.gson.JsonArray
import com.google.gson.JsonPrimitive
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.minecraft.client.KeyMapping
import org.lwjgl.glfw.GLFW

object MiddleClickGui : Feature(
    "middleClickGui",
    "Cancels your left clicks and turns it into a middle clicks on certain guis. " +
    "Keybind in controls to blacklist a gui.",
    subcategory = "Inventory",
) {
    private const val KEY_NAME = "mcgBlacklist"
    private var blacklisted = mutableListOf<String>()
    private val keybind = KeyBindingHelper.registerKeyBinding(
        KeyMapping(
            "key.devonian.mcgBlacklist",
            GLFW.GLFW_KEY_UNKNOWN,
            Devonian.keybindCategory
        )
    )
    private val avoidGuis = listOf(
        "Wardrobe",
        "Drill Anvil",
        "Anvil",
        "Storage",
        "The Hex",
        "Composter",
        "Auctions",
        "Abiphone",
        "Chest",
        "Large Chest",
    )
    private val terminalGuis = listOf(
        "^Click in order!$".toRegex(),
        "^Select all the (.+?) items!$".toRegex(),
        "^What starts with: '(.+?)'\\?$".toRegex(),
        "^Change all to same color!$".toRegex(),
        "^Correct all the panes!$".toRegex(),
        "^Click the button on time!$".toRegex(),
    )
    private val avoidItems = setOf(
        "Reforge Item",
        "Salvage Items",
        "Experience Bottles",
    )

    override fun initialize() {
        Config.set(KEY_NAME, JsonArray())

        Config.onAfterLoad {
            blacklisted = Config.get<List<JsonPrimitive>>(KEY_NAME)?.map { it.asString }?.toMutableList() ?: mutableListOf()
        }

        Config.onPreSave {
            val array = JsonArray()

            blacklisted.forEach { array.add(it) }

            Config.set(KEY_NAME, array)
        }

        on<PickupItemInventoryEvent> { event ->
            if (Location.area == null) return@on
            if (event.isSplitItem) return@on

            val slot = event.slot
            if (slot.container == minecraft.player?.inventory) return@on

            val stack = slot.item
            if (stack.isEmpty) return@on
            if (ItemUtils.skyblockId(stack) != null) return@on

            val screenName = event.screen.title?.string ?: return@on
            if (avoidGuis.any { screenName.startsWith(it) } || blacklisted.any { screenName.startsWith(it) }) return@on
            if (terminalGuis.any { it.matches(screenName) }) return@on

            val itemName = stack.customName?.string
            if (itemName != null && avoidItems.contains(itemName)) return@on

            event.cancel()
            ScreenUtils.click(slot.index, false, "MIDDLE")
        }

        on<GuiKeyDownEvent> { event ->
            if (!keybind.matches(event.event)) return@on
            if (event.screen == minecraft.player?.inventory) return@on
            val title = event.screen.title.string
            val containsIn = blacklisted.contains(title)

            if (containsIn) blacklisted.remove(title)
            else blacklisted.add(title)

            ChatUtils.sendMessage("&bMCG Blacklist ${if (containsIn) "&cRemoved" else "&aAdded"} &b$title", true)
        }
    }
}