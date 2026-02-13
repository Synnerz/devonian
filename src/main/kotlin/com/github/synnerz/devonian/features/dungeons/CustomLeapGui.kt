package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.devonian.api.Scheduler
import com.github.synnerz.devonian.api.ScreenUtils
import com.github.synnerz.devonian.api.dungeon.DungeonClass
import com.github.synnerz.devonian.api.dungeon.Dungeons
import com.github.synnerz.devonian.api.dungeon.Stages
import com.github.synnerz.devonian.api.events.*
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.utils.BasicState
import com.github.synnerz.talium.components.UIRect
import com.github.synnerz.talium.components.UIText
import com.github.synnerz.talium.effects.OutlineEffect
import net.minecraft.network.protocol.game.ClientboundContainerClosePacket
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket
import net.minecraft.world.item.Items
import java.awt.Color

object CustomLeapGui : Feature(
    "customLeapGui",
    "Changes the gui from the spirit leap to a custom one.",
    Categories.DUNGEONS,
    "catacombs",
    subcategory = "QOL",
) {
    override fun createRequirements(): List<BasicState<Boolean>?> {
        return super.createRequirements() + listOf(Stages.Root.isActiveState)
    }

    // TODO: integrate keybinds here instead of being standalone (maybe)
    //  also implement different type of scales since people don't like current sizes ):
    private val SETTING_PLAYER_SORTING = addSelection(
        "playerSorting",
        0,
        listOf("a-z", "z-a", "a-z name", "z-a name"),
        "Sorting order for CustomLeapGui",
        "CustomLeap Sorting",
    )
    private val SETTING_STATIC = addSwitch(
        "static",
        false,
        "When enabled, it will \"fill\" the remaining class roles with empty slots so the sorting is always the same to help with muscle memory (NOTE: it will still use the sorting YOU chose)",
        "CustomLeap Static"
    )
    private val SETTING_BACKGROUND_COLOR = addColorPicker(
        "backgroundColor",
        Color(25, 25, 25, 255).rgb,
        "Sets the background color for custom leap gui",
        "CustomLeap Background"
    )
    private const val CONTAINER_NAME = "Spirit Leap"
    private val closeChestKey get() = minecraft.options.keyInventory
    private val background = UIRect(0.0, 0.0, 100.0, 100.0)
    private var containerId = -1
    private val roles = listOf("Healer", "Tank", "Mage", "Berserk", "Archer")
    private val playerList = mutableListOf<LeapPlayer>()
    private val sortingComparators = listOf(
        compareBy<LeapPlayer> { it.role.singleLetter }.thenBy { it.name.lowercase() },
        compareByDescending<LeapPlayer> { it.role.singleLetter }.thenByDescending { it.name.lowercase() },
        compareBy { it.name.lowercase() },
        compareByDescending { it.name.lowercase() },
    )

    val leapComparator get() = sortingComparators[SETTING_PLAYER_SORTING.get()]
    data class LeapPlayer(val slot: Int, val name: String, val role: DungeonClass, val isDead: Boolean)

    override fun initialize() {
        on<PacketReceivedEvent> { event ->
            val packet = event.packet
            if (packet is ClientboundContainerClosePacket) {
                Scheduler.scheduleTask { background.clearChildren() }
                containerId = -1
                return@on
            }

            if (packet is ClientboundOpenScreenPacket) {
                if (packet.title.string != CONTAINER_NAME) {
                    Scheduler.scheduleTask { background.clearChildren() }
                    containerId = -1
                    return@on
                }
                containerId = packet.containerId
                return@on
            }
        }

        on<ServerContainerSetSlotEvent> { event ->
            if (containerId == -1) return@on

            val idx = event.slot
            if (idx !in 9..18) return@on
            if (idx == 17) {
                val player = Dungeons.players.firstEntry()?.value
                val pl = when {
                    SETTING_STATIC.get() && playerList.size < 4 -> {
                        if (player == null || player.role == DungeonClass.Unknown || player.isDead)
                            playerList
                        else {
                            val mut = mutableListOf<LeapPlayer>()
                            val currentRoles = playerList.map { it.role.name }
                            val amounts = currentRoles.groupingBy { it }.eachCount()
                            if (amounts.any { it.value > 1 }) {
                                playerList
                            } else {
                                val currentRole = player.role.name
                                val missing = roles.toMutableSet().apply { remove(currentRole) } - currentRoles.toSet()
                                mut.addAll(playerList)
                                missing.forEachIndexed { jdx, it -> mut.add(LeapPlayer(100 + jdx, "FAKE", DungeonClass.from(it), true)) }
                                mut
                            }
                        }
                    }
                    else -> playerList
                }

                val list = pl.sortedWith(leapComparator)
                playerList.clear()
                Scheduler.scheduleTask {
                    list.forEachIndexed { i, v -> create(v, i) }
                    background.scaledResolution?.let {
                        background.propagateResize(background, it)
                    }
                }
                return@on
            }
            val itemStack = event.itemStack
            if (itemStack.item != Items.PLAYER_HEAD) return@on
            val name = itemStack.customName?.string ?: return@on

            val data = Dungeons.players[name] ?: return@on
            playerList.add(LeapPlayer(idx, name, data.role, data.isDead))
        }

        on<PrePacketSentEvent> { event ->
            val packet = event.packet
            if (packet !is ServerboundContainerClosePacket) return@on

            containerId = -1
            background.clearChildren()
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
        if (data.slot >= 100 && data.isDead) return
        // TODO: add customizable sorting
        val r = idx / 2
        val c = idx % 2

        val w = 40.0
        val h = 30.0
        val gapX = 1.0
        val gapY = 2.0

        val xo = (100.0 - w - w - gapX) * 0.5
        val yo = (100.0 - h - h - gapY) * 0.5
        val x = xo + (w + gapX) * c
        val y = yo + (h + gapY) * r

        val role = "§7[${data.role.colorCode}${data.role.name}§7]"
        val outlineHover = data.role.color
        val outline = outlineHover.let { Color(it.red, it.green, it.blue, 150) }

        UIRect(x, y, w, h, parent = background).apply {
            val outlineEffect = OutlineEffect(1.5, outline)
            setColor(SETTING_BACKGROUND_COLOR.getColor())
            addEffect(outlineEffect)
            addChild(UIText(0.0, 35.0, 100.0, 16.0, data.name, true).apply {
                onResize { _, w ->
                    textScale = 6f / w.scaleFactor
                }
            })
            addChild(UIText(0.0, 60.0, 100.0, 16.0, role, true).apply {
                onResize { _, w ->
                    textScale = 5f / w.scaleFactor
                }
            })

            onMouseEnter { outlineEffect.color = outlineHover }
            onMouseLeave { outlineEffect.color = outline }
        }
        UIRect(if (idx % 2 == 0) 0.0 else x, if (idx < 2) 0.0 else y, 50.0, 50.0, parent = background).apply {
            onMouseRelease { event ->
                if (event.button !in 0..1 || data.isDead) return@onMouseRelease

                ScreenUtils.click(data.slot)
                Scheduler.scheduleTask { background.clearChildren() }
            }
        }
    }
}