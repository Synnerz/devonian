package com.github.synnerz.devonian.features.dungeons.clear

import com.github.synnerz.devonian.api.dungeon.DungeonScanner
import com.github.synnerz.devonian.api.dungeon.Dungeons
import com.github.synnerz.devonian.api.dungeon.Stages
import com.github.synnerz.devonian.api.dungeon.mapEnums.RoomTypes
import com.github.synnerz.devonian.api.events.RenderWorldEvent
import com.github.synnerz.devonian.api.events.TickEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.utils.BasicState
import com.github.synnerz.devonian.utils.render.Render3DImmediate
import java.awt.Color

object ColorPortal : Feature(
    "colorPortal",
    "recolors blood portal based on score",
    Categories.DUNGEONS,
    "catacombs",
    subcategory = "Highlights",
) {
    override fun createRequirements(): List<BasicState<Boolean>?> {
        return super.createRequirements() + listOf(Stages.PortalEnter.isActiveState)
    }

    private val SETTING_COMP_COLOR = addColorPicker(
        "compColor",
        Color(255, 16, 16, 160).rgb,
        "color for < 270 score",
        "< 270 Color",
    )
    private val SETTING_S_COLOR = addColorPicker(
        "sColor",
        Color(255, 215, 0, 160).rgb,
        "color for S",
        "S Color",
    )
    private val SETTING_PLUS_COLOR = addColorPicker(
        "plusColor",
        Color(64, 255, 0, 160).rgb,
        "color for S+",
        "S+ Color",
    )

    private var found = false
    private var x = 0.0
    private var z = 0.0
    private var wx = 0.0
    private var wz = 0.0

    override fun initialize() {
        on<TickEvent> {
            if (found) return@on

            val blood = DungeonScanner.rooms.find { it?.type == RoomTypes.BLOOD } ?: return@on
            if (!blood.hasRotation()) return@on

            val l = blood.fromComp(16, 29) ?: return@on
            val r = blood.fromComp(14, 29) ?: return@on

            when (blood.rotation) {
                0 -> {
                    x = r.first.toDouble()
                    z = l.second + 0.375
                    wx = 3.0
                    wz = 0.25
                }

                90 -> {
                    x = l.first + 0.375
                    z = r.second.toDouble()
                    wx = 0.25
                    wz = 3.0
                }

                180 -> {
                    x = l.first.toDouble()
                    z = r.second + 0.375
                    wx = 3.0
                    wz = 0.25
                }

                270 -> {
                    x = r.first + 0.375
                    z = l.second.toDouble()
                    wx = 0.25
                    wz = 3.0
                }

                else -> return@on
            }

            found = true
        }

        on<RenderWorldEvent> {
            if (!found) return@on

            val score = Dungeons.score.value
            val color = when {
                score < 270 -> SETTING_COMP_COLOR.getColor()
                score < 300 -> SETTING_S_COLOR.getColor()
                else -> SETTING_PLUS_COLOR.getColor()
            }

            Render3DImmediate.renderFilledBox(
                x, 69.0, z,
                wx, 4.0, color,
                phase = false,
                wz = wz,
            )
        }
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        found = false
        x = 0.0
        z = 0.0
        wx = 0.0
        wz = 0.0
    }
}