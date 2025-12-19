package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.devonian.api.Scheduler
import com.github.synnerz.devonian.api.ScreenUtils
import com.github.synnerz.devonian.api.dungeon.DungeonClass
import com.github.synnerz.devonian.api.dungeon.Dungeons
import com.github.synnerz.devonian.api.events.*
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.talium.components.UIRect
import com.github.synnerz.talium.components.UIText
import com.github.synnerz.talium.effects.OutlineEffect
import net.minecraft.network.protocol.game.ClientboundContainerClosePacket
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket
import net.minecraft.world.item.Items
import java.awt.Color

object CustomLeapGui : Feature(
    "customLeapGui",
    "Changes the gui from the spirit leap to a custom one",
    Categories.DUNGEONS,
    "catacombs",
    subcategory = "QOL"
) {
    // make outline color the class' color
    private const val CONTAINER_NAME = "Spirit Leap"
    private val closeChestKey get() = minecraft.options.keyInventory
    private val PRIMARY_COLOR = Color(25, 25, 25, 255)
    private val background = UIRect(0.0, 0.0, 100.0, 100.0)
    private var containerId = -1

    data class LeapPlayer(val slot: Int, val name: String, val role: DungeonClass, val isDead: Boolean)

    override fun initialize() {
        on<PacketReceivedEvent> { event ->
            val packet = event.packet
            if (packet is ClientboundContainerClosePacket) {
                Scheduler.scheduleTask { background.clearChildren() }
                return@on
            }

            if (packet is ClientboundOpenScreenPacket) {
                if (packet.title.string != CONTAINER_NAME) {
                    Scheduler.scheduleTask { background.clearChildren() }
                    return@on
                }
                containerId = packet.containerId
                return@on
            }
            if (containerId == -1 || packet !is ClientboundContainerSetContentPacket) return@on

            val items = packet.items
            var ids = 0
            background.clearChildren()

            for (idx in 9..18) {
                val itemStack = items.getOrNull(idx) ?: continue
                if (itemStack.item != Items.PLAYER_HEAD) continue
                val name = itemStack.customName?.string ?: continue

                val data = Dungeons.players[name] ?: continue
                create(LeapPlayer(idx, name, data.role, data.isDead), ids)
                ids++
            }

            containerId = -1
        }

        on<PacketSentEvent> { event ->
            val packet = event.packet
            if (packet !is ServerboundContainerClosePacket) return@on

            containerId = -1
            Scheduler.scheduleTask { background.clearChildren() }
        }

        on<RenderGuiEvent> { event ->
            val screen = event.screen
            if (screen.title.string != CONTAINER_NAME) return@on

            event.cancel()
            background.draw()
        }

        on<GuiClickEvent> { event ->
            val screen = event.screen
            if (screen.title.string != CONTAINER_NAME) return@on
            event.cancel()
        }

        on<GuiKeyDownEvent> { event ->
            val screen = event.screen
            if (screen.title.string != CONTAINER_NAME) return@on
            if (event.event.isEscape || closeChestKey.matches(event.event)) return@on
            event.cancel()
        }
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        containerId = -1
        background.clearChildren()
    }

    private fun create(data: LeapPlayer, idx: Int) {
        // TODO: add customizable sorting
        val r = idx / 2
        val c = idx % 2
        val xo = 37.0
        val yo = 40.0
        val role = when (data.role) {
            DungeonClass.Archer -> "§f[§6Archer§f]"
            DungeonClass.Mage -> "§f[§bMage§f]"
            DungeonClass.Berserk -> "§f[§cBerserk§f]"
            DungeonClass.Tank -> "§f[§aTank§f]"
            DungeonClass.Healer -> "§f[§dHealer§f]"
            else -> "§f[§4UNKNOWN§f]"
        }
        val outline = when (data.role) {
            DungeonClass.Archer -> Color(255, 170, 0, 150)
            DungeonClass.Mage -> Color(85, 255, 255, 150)
            DungeonClass.Berserk -> Color(255, 85, 85, 150)
            DungeonClass.Tank -> Color(77, 231, 77, 150)
            DungeonClass.Healer -> Color(255, 85, 255, 150)
            else -> Color(170, 0, 0, 150)
        }
        val outlineHover = when (data.role) {
            DungeonClass.Archer -> Color(255, 170, 0, 255)
            DungeonClass.Mage -> Color(85, 255, 255, 255)
            DungeonClass.Berserk -> Color(255, 85, 85, 255)
            DungeonClass.Tank -> Color(77, 231, 77, 255)
            DungeonClass.Healer -> Color(255, 85, 255, 255)
            else -> Color(170, 0, 0, 255)
        }

        UIRect(15.0 + c * xo, 10.0 + r * yo, 35.0, 35.0, parent = background).apply {
            val outlineEffect = OutlineEffect(1.5, outline)
            setColor(PRIMARY_COLOR)
            addEffect(outlineEffect)
            addChild(UIText(0.0, 35.0, 100.0, 9.0, data.name, true).apply { textScale = 3f })
            addChild(UIText(0.0, 50.0, 100.0, 9.0, role, true).apply { textScale = 2.5f })

            onMouseEnter { outlineEffect.color = outlineHover }
            onMouseLeave { outlineEffect.color = outline }

            onMouseRelease { event ->
                if (event.button != 0) return@onMouseRelease

                ScreenUtils.click(data.slot)
                Scheduler.scheduleTask { background.clearChildren() }
            }
        }
    }
}