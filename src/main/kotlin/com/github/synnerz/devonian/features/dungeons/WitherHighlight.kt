package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.barrl.Context
import com.github.synnerz.devonian.api.dungeon.Stages
import com.github.synnerz.devonian.api.events.RenderWorldEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.utils.BasicState
import net.minecraft.client.renderer.entity.state.WitherRenderState
import net.minecraft.world.entity.boss.wither.WitherBoss
import java.awt.Color

object WitherHighlight : Feature(
    "witherHighlight",
    "",
    Categories.DUNGEONS,
    "catacombs",
    subcategory = "World",
) {
    override fun createRequirements(): List<BasicState<Boolean>?> {
        return super.createRequirements() + listOf(Stages.F7.isActiveState)
    }

    private val SETTING_BOX = addSwitch(
        "box",
        false,
        "box instead of outline",
        "Wither Highlight Box",
    )
    private val SETTING_BOX_WIRE_COLOR = addColorPicker(
        "wireColor",
        Color(18, 222, 52).rgb,
        "used for highlight color if box is disabled",
        "Wither Box Wire Color",
    )
    private val SETTING_BOX_FILL_COLOR = addColorPicker(
        "fillColor",
        Color(18, 222, 52, 160).rgb,
        "",
        "Wither Box Fill Color",
    )
    private val SETTING_LINE_WIDTH = addSlider(
        "lineWidth",
        3.0,
        0.0, 10.0,
        "",
        "Wither Box Line Width",
    )

    private val deferredWithers = mutableListOf<Triple<Double, Double, Double>>()

    fun extractWither(wither: WitherBoss, state: WitherRenderState) {
        if (wither.maxHealth == 300f) return
        if (SETTING_BOX.get()) {
            deferredWithers.add(Triple(state.x, state.y, state.z))
        } else if (state.outlineColor == 0) state.outlineColor = SETTING_BOX_WIRE_COLOR.get() and 0xFFFFFF
    }

    override fun initialize() {
        on<RenderWorldEvent> {
            deferredWithers.forEach { (x, y, z) ->
                Context.Immediate?.renderFilledBox(
                    x - 0.6, y, z - 0.6,
                    1.2, 3.6,
                    SETTING_BOX_FILL_COLOR.getColor(),
                )
                Context.Immediate?.renderBox(
                    x - 0.6, y, z - 0.6,
                    1.2, 3.6,
                    SETTING_BOX_WIRE_COLOR.getColor(),
                    phase = true,
                    lineWidth = SETTING_LINE_WIDTH.get(),
                )
            }
            deferredWithers.clear()
        }.setEnabled(SETTING_BOX.state)
    }
}