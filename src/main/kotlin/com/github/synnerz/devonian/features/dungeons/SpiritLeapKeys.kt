package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.devonian.api.Scheduler
import com.github.synnerz.devonian.api.ScreenUtils
import com.github.synnerz.devonian.api.dungeon.Dungeons
import com.github.synnerz.devonian.api.dungeon.Stages
import com.github.synnerz.devonian.api.events.ClientContainerCloseEvent
import com.github.synnerz.devonian.api.events.GuiKeyDownEvent
import com.github.synnerz.devonian.api.events.ServerContainerCloseEvent
import com.github.synnerz.devonian.api.events.ServerContainerOpenEvent
import com.github.synnerz.devonian.api.events.ServerContainerSetSlotEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.utils.BasicState
import net.minecraft.world.item.Items
import org.lwjgl.sdl.SDLKeycode

object SpiritLeapKeys : Feature(
    "spiritLeapKeys",
    "Adds keys from 1-4 to leap to the respective player class.",
    Categories.DUNGEONS,
    "catacombs",
    subcategory = "QOL",
) {
    override fun createRequirements(): List<BasicState<Boolean>?> {
        return super.createRequirements() + listOf(Stages.Root.isActiveState)
    }

    private val keybinds = listOf(
        SDLKeycode.SDLK_1,
        SDLKeycode.SDLK_2,
        SDLKeycode.SDLK_3,
        SDLKeycode.SDLK_4,
    )
    private val playersData = mutableListOf<CustomLeapGui.LeapPlayer>()
    val leapComparator: Comparator<CustomLeapGui.LeapPlayer> get() {
        // TODO: make this work with Dynamic and Custom sorting (4-5)
        if (CustomLeapGui.isEnabled() && CustomLeapGui.SETTING_PLAYER_SORTING.get() < 4) {
            return CustomLeapGui.leapComparator
        }
        return Comparator.comparing<CustomLeapGui.LeapPlayer, Char> { it.role.singleLetter }.thenBy { it.name.lowercase() }
    }
    private var containerId = -1

    override fun initialize() {
        on<ServerContainerOpenEvent> { event ->
            if (event.titleStr == "Spirit Leap") {
                if (containerId != -1) Scheduler.scheduleTask { playersData.clear() }
                containerId = event.containerId
            }
        }

        on<ServerContainerCloseEvent> {
            Scheduler.scheduleTask { playersData.clear() }
            containerId = -1
        }

        on<ClientContainerCloseEvent> {
            playersData.clear()
            containerId = -1
        }

        on<ServerContainerSetSlotEvent> { event ->
            if (containerId == -1) return@on

            val idx = event.slot
            if (idx !in 9..18) return@on
            if (idx == 17) {
                Scheduler.scheduleTask { playersData.sortWith(leapComparator) }
                containerId = -1
                return@on
            }

            val itemStack = event.itemStack
            if (itemStack.item != Items.PLAYER_HEAD) return@on
            val name = itemStack.customName?.string ?: return@on
            val data = Dungeons.players[name] ?: return@on

            Scheduler.scheduleTask { playersData.add(CustomLeapGui.LeapPlayer(idx, name, data.role, data)) }
        }

        on<GuiKeyDownEvent> { event ->
            val screen = event.screen
            if (screen.title.string != "Spirit Leap") return@on

            keybinds.forEachIndexed { idx, it ->
                if (event.key != it) return@forEachIndexed
                event.cancel()
                val data = playersData.getOrNull(idx) ?: return@on
                val slot = data.slot
                ScreenUtils.click(slot)
                return@on
            }
        }
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        playersData.clear()
        containerId = -1
    }
}