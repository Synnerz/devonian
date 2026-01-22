package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.devonian.api.ChatUtils
import com.github.synnerz.devonian.api.Scheduler
import com.github.synnerz.devonian.api.dungeon.DungeonClass
import com.github.synnerz.devonian.api.dungeon.Dungeons
import com.github.synnerz.devonian.api.dungeon.Stages
import com.github.synnerz.devonian.api.events.RenderWorldEvent
import com.github.synnerz.devonian.api.events.TickEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.utils.BasicState
import com.github.synnerz.devonian.utils.render.Render3DImmediate
import java.awt.Color
import java.util.*

object PositionMessages : Feature(
    "positionMessages",
    "Sends a party chat message if you're standing near a specified area for your Dungeon Class (f7 boss fight).",
    Categories.DUNGEONS,
    "catacombs",
    subcategory = "F7",
) {
    override fun createRequirements(): List<BasicState<Boolean>?> {
        return super.createRequirements() + listOf(Stages.F7.isActiveState)
    }

    private val SETTING_RENDER_HIGHLIGHT = addSwitch(
        "renderHighlight",
        true,
        "Renders a box around the area for the positional message.",
        "Position Messages Render",
    )
    private val SETTING_RENDER_COLOR = addColorPicker(
        "renderColor",
        Color.CYAN.rgb,
        "The color of the box.",
        "Position Messages Render Color",
    )
    private val SETTING_REMOVE_AT = addSwitch(
        "removeRenderAt",
        false,
        "Removes the highlight around the positional message if you have already sent the message.",
        "Position Messages Remove Highlight",
    )
    private val SETTING_USE_ALL = addSwitch(
        "useAll",
        false,
        "Use all the positional messages not only the current selected class ones.",
        "Position Messages Use All",
    )

    // TODO: make these customizable
    private val positionList = EnumMap(
        mapOf(
            DungeonClass.Archer to listOf(
                PosMsg(108.5, 120.0, 94.5, 1.5, "at ss!"),
                PosMsg(58.5, 109.0, 131.5, 1.5, "at ee2!"),
                PosMsg(60.5, 132.0, 140.5, 1.5, "at ee2 high!"),
                PosMsg(69.5, 109.0, 122.5, 1.0, "at ee2 safe spot!"),
                PosMsg(48.5, 109.0, 122.5, 1.0, "at ee2 safe spot!"),
                PosMsg(54.5, 65.0, 76.5, 33.0, "at mid!"),
            ),
            DungeonClass.Berserk to listOf(
                PosMsg(63.5, 127.0, 35.5, 3.0, "at i4!"),
                PosMsg(54.5, 65.0, 76.5, 33.0, "at mid!"),
            ),
            DungeonClass.Mage to listOf(
                PosMsg(58.5, 123.0, 122.5, 0.3, "entering core!"),
                PosMsg(54.5, 115.0, 51.5, 1.5, "at core!"),
                PosMsg(54.5, 65.0, 76.5, 33.0, "at mid!"),
            ),
            DungeonClass.Tank to listOf(
                PosMsg(54.5, 65.0, 76.5, 33.0, "at mid!"),
            ),
            DungeonClass.Healer to listOf(
                PosMsg(108.5, 120.0, 94.5, 1.5, "at ss!"),
                PosMsg(2.5, 109.0, 104.5, 1.5, "at ee3!"),
                PosMsg(18.5, 121.0, 99.5, 3.0, "at ee3 safe spot!"),
                PosMsg(1.5, 120.0, 77.5, 3.0, "at arrows dev!"),
                PosMsg(60.5, 132.0, 140.5, 1.5, "at levers dev!"),
                PosMsg(54.5, 65.0, 76.5, 33.0, "at mid!"),
                PosMsg(54.5, 5.0, 76.5, 8.0, "at p5!"),
            ),
            DungeonClass.Unknown to emptyList(),
        )
    )
    private var currentPos: MutableList<PosMsg>? = null

    data class PosMsg(
        val x: Double,
        val y: Double,
        val z: Double,
        val dist: Double = 1.0,
        val message: String?,
    ) {
        var sent = false
    }

    override fun initialize() {
        on<TickEvent> {
            if (currentPos == null) {
                val player = Dungeons.players.firstEntry()?.value ?: return@on
                if (player.role == DungeonClass.Unknown || player.isDead) return@on
                currentPos = if (SETTING_USE_ALL.get()) positionList.values.flatMapTo(mutableListOf()) {
                    it.map { it.copy() }
                } else positionList[player.role]?.mapTo(mutableListOf()) { it.copy() }
            }

            val player = minecraft.player ?: return@on
            val pos = currentPos ?: return@on
            var msg: String? = null
            pos.forEach {
                if (it.sent) return@forEach
                if (player.distanceToSqr(it.x, it.y, it.z) > it.dist) return@forEach
                it.sent = true
                msg = it.message
            }

            if (msg != null) Scheduler.scheduleTask {
                ChatUtils.say("/pc $msg)")
            }
        }

        on<RenderWorldEvent> {
            currentPos?.forEach {
                if (SETTING_REMOVE_AT.get() && it.sent) return@forEach

                Render3DImmediate.renderWireframeBox(
                    it.x, it.y, it.z,
                    1.0, 0.5,
                    SETTING_RENDER_COLOR.getColor(),
                    centered = true,
                )
            }
        }.setEnabled(SETTING_RENDER_HIGHLIGHT.state)
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        currentPos = null
    }
}