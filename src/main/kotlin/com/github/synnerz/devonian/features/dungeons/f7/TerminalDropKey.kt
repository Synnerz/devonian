package com.github.synnerz.devonian.features.dungeons.f7

import com.github.synnerz.devonian.Devonian
import com.github.synnerz.devonian.api.dungeon.Stages
import com.github.synnerz.devonian.api.events.ClientContainerCloseEvent
import com.github.synnerz.devonian.api.events.ServerContainerCloseEvent
import com.github.synnerz.devonian.api.events.ServerContainerOpenEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.mixin.accessor.KeyMappingAccessor
import com.github.synnerz.devonian.utils.BasicState
import com.mojang.blaze3d.platform.InputConstants
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper
import net.minecraft.client.KeyMapping
import org.lwjgl.glfw.GLFW

object TerminalDropKey : Feature(
    "terminalDropKey",
    "Changes your drop key while inside of terminal (you can change the key in minecraft controls for this feature)",
    Categories.F7,
    "catacombs",
    subcategory = "Terminals",
) {
    override fun createRequirements(): List<BasicState<Boolean>?> {
        return super.createRequirements() + listOf(Stages.Terminals.isActiveState)
    }

    private val keybind = KeyMappingHelper.registerKeyMapping(
        KeyMapping(
            "key.devonian.termdropkey",
            GLFW.GLFW_KEY_UNKNOWN,
            Devonian.keybindCategory
        )
    )
    private val terminalGuis = listOf(
        "^Click in order!$".toRegex(),
        "^Select all the (.+?) items!$".toRegex(),
        "^What starts with: '(.+?)'\\?$".toRegex(),
        "^Change all to same color!$".toRegex(),
        "^Correct all the panes!$".toRegex(),
        "^Click the button on time!$".toRegex(),
    )
    private var inTerminal = false
    private var lastDropKey: InputConstants.Key? = null

    override fun initialize() {
        on<ServerContainerOpenEvent> { event ->
            if (terminalGuis.any { it.matches(event.titleStr) } && !inTerminal) {
                inTerminal = true
                lastDropKey = (minecraft.options.keyDrop as KeyMappingAccessor).key
                minecraft.options.keyDrop.setKey((keybind as KeyMappingAccessor).key)
            }
        }

        on<ServerContainerCloseEvent> {
            if (inTerminal && lastDropKey != null)
                minecraft.options.keyDrop.setKey(lastDropKey!!)
            inTerminal = false
        }

        on<ClientContainerCloseEvent> {
            if (inTerminal && lastDropKey != null)
                minecraft.options.keyDrop.setKey(lastDropKey!!)
            inTerminal = false
        }
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        inTerminal = false
    }
}