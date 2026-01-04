package com.github.synnerz.devonian.features.dungeons.m7

import com.github.synnerz.barrl.Context
import com.github.synnerz.devonian.api.WorldUtils
import com.github.synnerz.devonian.api.dungeon.Stages
import com.github.synnerz.devonian.api.events.RenderWorldEvent
import com.github.synnerz.devonian.api.events.TickEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.utils.BasicState
import java.util.*

object DragonBoxes : Feature(
    "dragonBoxes",
    "",
    Categories.DUNGEONS,
    "catacombs",
    subcategory = "World",
) {
    override fun createRequirements(): List<BasicState<Boolean>?> {
        return super.createRequirements() + Stages.WitherKing.isActiveState
    }

    private val SETTING_LINE_WIDTH = addSlider(
        "lineWidth",
        5.0,
        0.0, 20.0,
        "",
        "Dragon Box Line Width",
    )
    private val SETTING_DRAW_WAYPOINTS = addSwitch(
        "waypoints",
        true,
        "currently only lb waypoints",
        "Dragon Waypoints",
    )

    private var alive = EnumSet.allOf(M7Dragon::class.java)

    override fun initialize() {
        on<TickEvent> {
            val w = minecraft.level ?: return@on
            alive.removeIf {
                if (!WorldUtils.isChunkLoaded(it.chin.x, it.chin.z)) return@removeIf false
                val bs = w.getBlockState(it.chin) ?: return@removeIf false
                bs.isAir
            }
        }

        on<RenderWorldEvent> {
            alive.forEach {
                Context.Immediate?.renderBox(
                    it.box.minX, it.box.minY, it.box.minZ,
                    it.box.maxX - it.box.minX,
                    it.box.maxY - it.box.minY,
                    it.color,
                    phase = false,
                    lineWidth = SETTING_LINE_WIDTH.get(),
                    widthZ = it.box.maxZ - it.box.minZ,
                )
                if (!SETTING_DRAW_WAYPOINTS.get()) return@forEach
                it.waypoints.forEach { bp ->
                    Context.Immediate?.renderBeam(
                        bp.x.toDouble(),
                        bp.y.toDouble(),
                            bp.z.toDouble(),
                        it.color,
                        phase = false,
                    )
                }
            }
        }
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        alive = EnumSet.allOf(M7Dragon::class.java)
    }
}