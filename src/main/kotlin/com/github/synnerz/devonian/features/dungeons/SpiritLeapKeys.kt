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
    "Adds keys from 1-4 to leap to the respective player class.",
    Categories.DUNGEONS,
    "catacombs",
    subcategory = "QOL",
) {
    override fun createRequirements(): List<BasicState<Boolean>?> {
        return super.createRequirements() + listOf(Stages.Root.isActiveState)
    }

    private val keybinds = listOf(
        GLFW.GLFW_KEY_1,
        GLFW.GLFW_KEY_2,
        GLFW.GLFW_KEY_3,
        GLFW.GLFW_KEY_4,
    )
    private val playersData = mutableListOf<LeapPlayer>()
    val leapComparator = Comparator.comparing<LeapPlayer, Char> { it.role.singleLetter }.thenBy { it.name.lowercase() }
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

            playersData.sortWith(leapComparator)

            containerId = -1
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
    }
}