package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.barrl.Context
import com.github.synnerz.devonian.api.ChatUtils
import com.github.synnerz.devonian.api.Scheduler
import com.github.synnerz.devonian.api.dungeon.DungeonClass
import com.github.synnerz.devonian.api.dungeon.Dungeons
import com.github.synnerz.devonian.api.events.RenderWorldEvent
import com.github.synnerz.devonian.api.events.ServerTickEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import java.awt.Color
import java.util.*

object PositionMessages : Feature(
    "positionMessages",
    "Sends a party chat message if you're standing near a specified area for your Dungeon Class (f7 boss fight)",
    Categories.DUNGEONS,
    "catacombs",
    subcategory = "F7"
) {
    private val SETTING_RENDER_HIGHLIGHT = addSwitch(
        "renderHighlight",
        true,
        "Renders a box around the area for the positional message",
        "Position Messages Render"
    )
    private val SETTING_RENDER_COLOR = addColorPicker(
        "renderColor",
        Color.CYAN.rgb,
        "The color of the box",
        "Position Messages Render Color"
    )
    private val SETTING_REMOVE_AT = addSwitch(
        "removeRenderAt",
        false,
        "Removes the highlight around the positional message if you have already sent the message",
        "Position Messages Remove Highlight"
    )
    private val SETTING_USE_ALL = addSwitch(
        "useAll",
        false,
        "Use all the positional messages not only the current selected class ones",
        "Position Messages Use All"
    )
    // TODO: make these customizable
    private val positionList = EnumMap(
        mapOf(
            DungeonClass.Archer to listOf(
                PosMsg(108.0, 120.0, 94.0, 1.5, "at ss!"),
                PosMsg(58.0, 109.0, 131.0, 1.5, "at ee2!"),
                PosMsg(60.0, 132.0, 140.0, 1.5, "at ee2 high!"),
                PosMsg(69.0, 109.0, 122.0, 1.0, "at ee2 safe spot!"),
                PosMsg(48.0, 109.0, 122.0, 1.0, "at ee2 safe spot!"),
                PosMsg(54.0, 65.0, 76.0, 33.0, "at mid!"),
            ),
            DungeonClass.Berserk to listOf(
                PosMsg(63.0, 127.0, 35.0, 3.0, "at i4!"),
                PosMsg(54.0, 65.0, 76.0, 33.0, "at mid!"),
            ),
            DungeonClass.Mage to listOf(
                PosMsg(58.0, 123.0, 122.0, 0.3, "entering core!"),
                PosMsg(54.0, 115.0, 51.0, 1.5, "at core!"),
                PosMsg(54.0, 65.0, 76.0, 33.0, "at mid!"),
            ),
            DungeonClass.Tank to listOf(
                PosMsg(54.0, 65.0, 76.0, 33.0, "at mid!"),
            ),
            DungeonClass.Healer to listOf(
                PosMsg(108.0, 120.0, 94.0, 1.5, "at ss!"),
                PosMsg(2.0, 109.0, 104.0, 1.5, "at ee3!"),
                PosMsg(18.0, 121.0, 99.0, 3.0, "at ee3 safe spot!"),
                PosMsg(1.0, 120.0, 77.0, 3.0, "at arrows dev!"),
                PosMsg(60.0, 132.0, 140.0, 1.5, "at levers dev!"),
                PosMsg(54.0, 65.0, 76.0, 33.0, "at mid!"),
                PosMsg(54.0, 5.0, 76.0, 8.0, "at p5!"),
            ),
        )
    )
    private val specialPos = listOf(
        listOf(53.0, 63.0, 113.0),
        listOf(55.0, 63.0, 113.0),
        listOf(55.0, 63.0, 115.0),
        listOf(53.0, 63.0, 115.0),
    )
    private var currentPos: List<PosMsg>? = null

    data class PosMsg(
        val x: Double,
        val y: Double,
        val z: Double,
        val dist: Double = 1.0,
        val message: String?,
        var sent: Boolean = false
    )

    override fun initialize() {
        on<ServerTickEvent> {
            if (currentPos != null) return@on
            val playerName = minecraft.player?.name?.string
            val playerData = Dungeons.players[playerName] ?: return@on
            if (playerData.role == DungeonClass.Unknown || playerData.isDead) return@on
            if (SETTING_USE_ALL.get()) {
                currentPos = positionList.values.flatten()
                return@on
            }

            currentPos = positionList[playerData.role]
        }

        on<ServerTickEvent> { event ->
            if (!Dungeons.inBoss.value || Dungeons.floor.floorNum != 7) return@on

            val pos = minecraft.player ?: return@on
            val msg = currentPos?.find {
                pos.distanceToSqr(it.x + 0.5, it.y, it.z + 0.5) <= it.dist
            } ?: return@on
            if (msg.sent) return@on

            Scheduler.scheduleTask {
                if (msg.message == null) return@scheduleTask
                ChatUtils.say("/pc ${msg.message}")
            }
            msg.sent = true
        }

        on<RenderWorldEvent> {
            if (!Dungeons.inBoss.value || Dungeons.floor.floorNum != 7) return@on
            if (!SETTING_RENDER_HIGHLIGHT.get()) return@on

            currentPos?.forEach {
                if (SETTING_REMOVE_AT.get() && it.sent) return@forEach

                Context.Immediate?.renderBox(
                    it.x, it.y, it.z,
                    1.0, 0.5,
                    SETTING_RENDER_COLOR.getColor(),
                )
            }
            specialPos.forEach {
                val ( x, y, z ) = it

                Context.Immediate?.renderBox(
                    x, y, z,
                    1.0, 1.05,
                    SETTING_RENDER_COLOR.getColor(),
                )
            }
        }
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        currentPos = null
    }
}