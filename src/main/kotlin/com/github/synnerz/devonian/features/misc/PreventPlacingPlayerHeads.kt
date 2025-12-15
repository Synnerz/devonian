package com.github.synnerz.devonian.features.misc

import com.github.synnerz.devonian.Devonian
import com.github.synnerz.devonian.api.ChatUtils
import com.github.synnerz.devonian.api.ItemUtils
import com.github.synnerz.devonian.api.events.BlockInteractEvent
import com.github.synnerz.devonian.api.events.KeyPressEvent
import com.github.synnerz.devonian.config.Config
import com.github.synnerz.devonian.features.Feature
import com.google.gson.JsonArray
import com.google.gson.JsonPrimitive
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.minecraft.client.KeyMapping
import net.minecraft.world.item.Items
import org.lwjgl.glfw.GLFW

object PreventPlacingPlayerHeads : Feature(
    "preventPlacingPlayerHeads",
    "Stops Player Heads from being placeable.",
    subcategory = "Tweaks",
) {
    private const val KEY_NAME = "pphBlacklist"
    private var blacklisted = mutableListOf<String>()
    private val keybind = KeyBindingHelper.registerKeyBinding(
        KeyMapping(
            "key.devonian.pphBlacklist",
            GLFW.GLFW_KEY_UNKNOWN,
            Devonian.keybindCategory
        )
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

        on<BlockInteractEvent> { event ->
            if (minecraft.level?.getBlockState(event.pos) == null) return@on
            val itemStack = event.itemStack
            val sbId = ItemUtils.skyblockId(itemStack)
            if (sbId == null || itemStack.item != Items.PLAYER_HEAD) return@on
            val lore = ItemUtils.lore(itemStack) ?: return@on
            if (blacklisted.contains(sbId)) return@on

            if (lore.any { it.contains("RIGHT CLICK") || it.contains("Right-click") })
                event.cancel()
        }

        on<KeyPressEvent> { event ->
            if (!keybind.matches(event.underlying)) return@on
            val item = minecraft.player?.mainHandItem ?: return@on
            val sbId = ItemUtils.skyblockId(item) ?: return@on
            val containsIn = blacklisted.contains(sbId)

            if (containsIn) blacklisted.remove(sbId)
            else blacklisted.add(sbId)

            ChatUtils.sendMessage("&bPPH Blacklist ${if (containsIn) "&cRemoved" else "&aAdded"} &b$sbId", true)
        }
    }
}