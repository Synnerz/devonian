package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.barrl.Context
import com.github.synnerz.devonian.api.ChatUtils
import com.github.synnerz.devonian.api.Scheduler
import com.github.synnerz.devonian.api.dungeon.DungeonClass
import com.github.synnerz.devonian.api.dungeon.Dungeons
import com.github.synnerz.devonian.api.events.*
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import java.awt.Color
import kotlin.math.ceil
import kotlin.math.floor

object PositionMessages : Feature(
    "positionMessages",
    "Sends a party chat message if you're standing near a specified area for your Dungeon Class (f7 boss fight)",
    Categories.DUNGEONS,
    "catacombs",
    subcategory = "QOL"
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
    private val positionList = mapOf(
        'a' to listOf(
            PosMsg(108.0, 120.0, 94.0, 1.5, "at ss!"),
            PosMsg(58.0, 109.0, 131.0, 1.5, "at ee2!"),
            PosMsg(60.5, 132.0, 139.5, 1.5, "at ee2 high!"),
            PosMsg(69.5, 109.0, 121.5, 0.4999, "at ee2 safe spot!"),
            PosMsg(67.5, 109.0, 121.5, 0.4999, "at ee2 safe spot!"),
            PosMsg(48.5, 109.0, 121.5, 0.4999, "at ee2 safe spot!"),
            PosMsg(46.5, 109.0, 121.5, 0.4999, "at ee2 safe spot!"),
            PosMsg(54.5, 65.0, 76.5, 7.5, "at mid!"),
            PosMsg(54.5, 63.7, 114.5, 3.0, null),
        ),
        'b' to listOf(
            PosMsg(63.5, 127.0, 35.5, 1.5, "at i4!"),
            PosMsg(54.5, 65.0, 76.5, 7.5, "at mid!"),
            PosMsg(54.5, 63.7, 114.5, 3.0, null),
        ),
        'm' to listOf(
            PosMsg(58.5, 123.0, 122.5, 0.3, "entering core!"),
            PosMsg(54.5, 115.0, 50.5, 1.5, "at core!"),
            PosMsg(54.5, 65.0, 76.5, 7.5, "at mid!"),
            PosMsg(54.5, 63.7, 114.5, 3.0, null),
        ),
        't' to listOf(
            PosMsg(54.5, 65.0, 76.5, 7.5, "at mid!"),
            PosMsg(54.5, 63.7, 114.5, 3.0, null),
        ),
        'h' to listOf(
            PosMsg(108.0, 120.0, 94.0, 1.5, "at ss!"),
            PosMsg(2.0, 109.0, 104.0, 1.5, "at ee3!"),
            PosMsg(18.5, 121.0, 91.5, 0.45, "at ee3 safe spot!"),
            PosMsg(18.5, 121.0, 92.5, 0.45, "at ee3 safe spot!"),
            PosMsg(18.5, 121.0, 93.5, 0.45, "at ee3 safe spot!"),
            PosMsg(18.5, 121.0, 94.5, 0.45, "at ee3 safe spot!"),
            PosMsg(18.5, 121.0, 95.5, 0.45, "at ee3 safe spot!"),
            PosMsg(18.5, 121.0, 96.5, 0.45, "at ee3 safe spot!"),
            PosMsg(18.5, 121.0, 97.5, 0.45, "at ee3 safe spot!"),
            PosMsg(18.5, 121.0, 98.5, 0.45, "at ee3 safe spot!"),
            PosMsg(18.5, 121.0, 99.5, 0.45, "at ee3 safe spot!"),
            PosMsg(-0.5, 120.0, 77.5, 1.5, "at arrows dev!"),
            PosMsg(60.5, 132.0, 139.5, 1.5, "at levers dev!"),
            PosMsg(54.5, 65.0, 76.5, 7.5, "at mid!"),
            PosMsg(54.5, 5.0, 76.5, 3.0, "at p5!"),
            PosMsg(54.5, 63.7, 114.5, 3.0, null),
        )
    )
    private var currentPos: List<PosMsg>? = null

    data class AxisBox(val minx: Double, val miny: Double, val minz: Double, val maxx: Double, val maxy: Double, val maxz: Double)
    data class PosMsg(
        val x: Double,
        val y: Double,
        val z: Double,
        val dist: Double = 1.0,
        val message: String?,
        var sent: Boolean = false,
        val box: AxisBox = AxisBox(
            floor(x - dist),
            floor(y - dist),
            floor(z - dist),
            ceil(x + dist),
            ceil(y + dist),
            ceil(z + dist)
        )
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

            currentPos = positionList[playerData.role.singleLetter]
        }

        on<ServerTickEvent> { event ->
            if (!Dungeons.inBoss.value || Dungeons.floor.floorNum != 7) return@on

            val pos = minecraft.player ?: return@on
            val msg = currentPos?.find {
                pos.distanceToSqr(it.x, it.y, it.z) <= it.dist
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
                    it.x - it.dist / 2, it.y, it.z - it.dist / 2,
                    it.dist * 2, 0.5,
                    SETTING_RENDER_COLOR.getColor(),
                )
            }
        }
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        currentPos = null
    }
}