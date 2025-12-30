package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.barrl.Context
import com.github.synnerz.devonian.api.ChatUtils
import com.github.synnerz.devonian.api.Scheduler
import com.github.synnerz.devonian.api.dungeon.DungeonClass
import com.github.synnerz.devonian.api.dungeon.Dungeons
import com.github.synnerz.devonian.api.events.*
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket
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
    // TODO: make these customizable
    private val positionList = mapOf(
        'a' to listOf(
            PosMsg(108, 120, 94, 1.5, "at ss!"),
            PosMsg(58, 109, 131, 1.5, "at ee2!"),
            PosMsg(60, 132, 139, 1.5, "at ee2 high!"),
            PosMsg(69, 109, 121, 0.4999, "at ee2 safe spot!"),
            PosMsg(67, 109, 121, 0.4999, "at ee2 safe spot!"),
            PosMsg(48, 109, 121, 0.4999, "at ee2 safe spot!"),
            PosMsg(46, 109, 121, 0.4999, "at ee2 safe spot!"),
            PosMsg(54, 65, 76, 7.5, "at mid!"),
        ),
        'b' to listOf(
            PosMsg(63, 127, 35, 1.5, "at i4!"),
            PosMsg(54, 65, 76, 7.5, "at mid!"),
        ),
        'm' to listOf(
            PosMsg(58, 123, 122, 0.3, "entering core!"),
            PosMsg(54, 115, 50, 1.5, "at core!"),
            PosMsg(54, 65, 76, 7.5, "at mid!"),
        ),
        't' to listOf(
            PosMsg(54, 65, 76, 7.5, "at mid!")
        ),
        'h' to listOf(
            PosMsg(108, 120, 94, 1.5, "at ss!"),
            PosMsg(2, 109, 104, 1.5, "at ee3!"),
            PosMsg(18, 121, 91, 0.45, "at ee3 safe spot!"),
            PosMsg(18, 121, 92, 0.45, "at ee3 safe spot!"),
            PosMsg(18, 121, 93, 0.45, "at ee3 safe spot!"),
            PosMsg(18, 121, 94, 0.45, "at ee3 safe spot!"),
            PosMsg(18, 121, 95, 0.45, "at ee3 safe spot!"),
            PosMsg(18, 121, 96, 0.45, "at ee3 safe spot!"),
            PosMsg(18, 121, 97, 0.45, "at ee3 safe spot!"),
            PosMsg(18, 121, 98, 0.45, "at ee3 safe spot!"),
            PosMsg(18, 121, 99, 0.45, "at ee3 safe spot!"),
            PosMsg(-1, 120, 77, 1.5, "at arrows dev!"),
            PosMsg(60, 132, 139, 1.5, "at levers dev!"),
            PosMsg(54, 65, 76, 7.5, "at mid!"),
            PosMsg(54, 5, 76, 3.0, "at p5!"),
        )
    )
    private var currentPos: List<PosMsg>? = null

    data class AxisBox(val minx: Double, val miny: Double, val minz: Double, val maxx: Double, val maxy: Double, val maxz: Double)
    data class PosMsg(
        val x: Int,
        val y: Int,
        val z: Int,
        val dist: Double = 1.0,
        val message: String,
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

            currentPos = positionList[playerData.role.singleLetter]
        }

        on<ServerTickEvent> { event ->
            if (!Dungeons.inBoss.value || Dungeons.floor.floorNum != 7) return@on

            val pos = minecraft.player?.position() ?: return@on
            val msg = currentPos?.find {
                val dx = it.x - pos.x
                val dy = it.y - pos.y
                val dz = it.z - pos.z
                val rad = dx * dx + dy * dy + dz * dz

                if (rad <= it.dist * it.dist) return@find true
                false
            } ?: return@on
            if (msg.sent) return@on

            Scheduler.scheduleTask { ChatUtils.say("/pc ${msg.message}") }
            msg.sent = true
        }

        on<RenderWorldEvent> {
            if (!Dungeons.inBoss.value || Dungeons.floor.floorNum != 7) return@on
            if (!SETTING_RENDER_HIGHLIGHT.get()) return@on

            currentPos?.forEach {
                if (SETTING_REMOVE_AT.get() && it.sent) return@forEach

                Context.Immediate?.renderBox(
                    it.x.toDouble() - it.dist / 2, it.y.toDouble(), it.z.toDouble() - it.dist / 2,
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