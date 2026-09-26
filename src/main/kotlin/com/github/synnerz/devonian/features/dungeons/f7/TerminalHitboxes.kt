package com.github.synnerz.devonian.features.dungeons.f7

import com.github.synnerz.devonian.api.dungeon.Stages
import com.github.synnerz.devonian.api.events.RenderWorldEvent
import com.github.synnerz.devonian.api.events.TickEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.utils.BasicState
import com.github.synnerz.devonian.utils.render.Render3DImmediate
import java.awt.Color

object TerminalHitboxes : Feature(
    "terminalHitboxes",
    "Shows hitbox of f7 terminals.",
    Categories.F7,
    subcategory = "Terminals",
) {
    override fun createRequirements(): List<BasicState<Boolean>?> {
        return super.createRequirements() + listOf(Stages.Terminals.isActiveState)
    }

    private val SETTING_OUTLINE_COLOR = addColorPicker(
        "outlineColor",
        Color(0, 255, 255, 255).rgb,
        "Color of the outline",
        "Outline Color"
    )
    private val SETTING_FILLED_COLOR = addColorPicker(
        "fillColor",
        Color(0, 255, 255, 64).rgb,
        "",
        "Filled Color",
    )
    private val SETTING_OUTLINE_WIDTH = addSlider(
        "outlineWidth",
        2.0,
        0.0, 10.0,
        "",
        "Outline Width"
    )

    private data class Terminal(val x: Double, val y: Double, val z: Double)
    private val terminals = listOf(
        Terminal(110.5, 112.02, 73.5),
        Terminal(110.5, 118.02, 79.5),
        Terminal(90.5, 111.02, 92.5),
        Terminal(90.5, 121.02, 101.5),
        Terminal(68.5, 108.02, 122.5),
        Terminal(59.5, 119.02, 123.5),
        Terminal(39.5, 107.02, 142.5),
        Terminal(40.5, 123.02, 123.5),
        Terminal(47.5, 108.02, 122.5),
        Terminal(-1.5, 108.01, 112.5),
        Terminal(-1.5, 118.02, 93.5),
        Terminal(18.5, 122.02, 93.5),
        Terminal(-1.5, 108.02, 77.5),
        Terminal(41.5, 108.00, 30.5),
        Terminal(44.5, 120.02, 30.5),
        Terminal(67.5, 108.02, 30.5),
        Terminal(72.5, 114.02, 47.5),
    )

    private var nearby = emptyList<Terminal>()

    override fun initialize() {
        on<TickEvent> {
            val player = minecraft.player ?: return@on

            val x = player.x
            val y = player.y - 2.0
            val z = player.z

            nearby = terminals.filter {
                val dist = (it.x - x) * (it.x - x) + (it.y - y) * (it.y - y) + (it.z - z) * (it.z - z)
                return@filter dist < 25.0
            }
        }

        on<RenderWorldEvent> {
            nearby.forEach {
                Render3DImmediate.renderFilledBox(
                    it.x, it.y, it.z,
                    0.5, 1.975,
                    SETTING_FILLED_COLOR.getColor(),
                    centered = true,
                )
                Render3DImmediate.renderWireframeBox(
                    it.x, it.y, it.z,
                    0.5, 1.975,
                    SETTING_OUTLINE_COLOR.getColor(),
                    centered = true,
                    lineWidth = SETTING_OUTLINE_WIDTH.get(),
                )
            }
        }
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        nearby = emptyList()
    }
}