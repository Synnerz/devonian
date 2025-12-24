package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.devonian.api.ScreenUtils
import com.github.synnerz.devonian.api.dungeon.DungeonClass
import com.github.synnerz.devonian.api.dungeon.Dungeons
import com.github.synnerz.devonian.api.dungeon.Stages
import com.github.synnerz.devonian.api.events.GuiKeyDownEvent
import com.github.synnerz.devonian.api.events.PacketReceivedEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.utils.BasicState
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket
import net.minecraft.world.item.Items
import org.lwjgl.glfw.GLFW

object SpiritLeapKeys : Feature(
    "spiritLeapKeys",
    "Adds keys from 1-4(archer, mage, bers, healer/tank) to leap to the respective player class",
    Categories.DUNGEONS,
    "catacombs",
    subcategory = "QOL"
) {
    override fun createRequirements(): List<BasicState<Boolean>?> {
        return super.createRequirements() + listOf(Stages.Root.isActiveState)
    }

    private val keybinds = listOf(
        GLFW.GLFW_KEY_1, // archer
        GLFW.GLFW_KEY_2, // mage
        GLFW.GLFW_KEY_3, // bers
        GLFW.GLFW_KEY_4, // healer/tank
    )
    private val roleSorting = listOf(
        "Archer",
        "Mage",
        "Berserk",
        "Healer",
        "Tank",
    )
    private val roleSorting2 = listOf(
        "Archer",
        "Mage",
        "Berserk",
        "Tank",
        "Healer",
    )
    private val rolesList get() =
        if (Dungeons.players[minecraft.player?.name?.string]?.role == DungeonClass.Healer)
            roleSorting2
        else
            roleSorting
    private val playersData = mutableListOf<LeapPlayer>()
    private var containerId = -1

    data class LeapPlayer(val slot: Int, val name: String, val role: DungeonClass, val isDead: Boolean)

    override fun initialize() {
        on<PacketReceivedEvent> { event ->
            val packet = event.packet
            if (packet is ClientboundOpenScreenPacket) {
                if (packet.title.string != "Spirit Leap") return@on
                containerId = packet.containerId
                return@on
            }
            if (containerId == -1 || packet !is ClientboundContainerSetContentPacket) return@on

            val items = packet.items
            playersData.clear()

            for (idx in 9..18) {
                val itemStack = items.getOrNull(idx) ?: continue
                if (itemStack.item != Items.PLAYER_HEAD) continue
                val name = itemStack.customName?.string ?: continue

                val data = Dungeons.players[name] ?: continue
                playersData.add(LeapPlayer(idx, name, data.role, data.isDead))
            }

            containerId = -1
        }

        on<GuiKeyDownEvent> { event ->
            val screen = event.screen
            if (screen.title.string != "Spirit Leap") return@on

            keybinds.forEachIndexed { idx, it ->
                if (event.key != it) return@forEachIndexed
                event.cancel()
                val role = DungeonClass.from(rolesList[idx])
                val data = playersData.firstOrNull { it.role == role && !it.isDead } ?: return@forEachIndexed
                val slot = data.slot
                ScreenUtils.click(slot)
                return@on
            }
        }
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        playersData.clear()
    }
}