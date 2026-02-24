package com.github.synnerz.devonian.features.dungeons.f7

import com.github.synnerz.devonian.api.dungeon.Stages
import com.github.synnerz.devonian.api.events.PostExtractRenderEntityEvent
import com.github.synnerz.devonian.api.events.RenderWorldEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.utils.BasicState
import com.github.synnerz.devonian.utils.render.Render3DImmediate
import net.minecraft.client.renderer.entity.state.WitherRenderState
import net.minecraft.world.entity.boss.wither.WitherBoss
import java.awt.Color

object WitherHighlight : Feature(
    "witherHighlight",
    "",
    Categories.F7,
    "catacombs",
    subcategory = "Highlight",
) {
    override fun createRequirements(): List<BasicState<Boolean>?> {
        return super.createRequirements() + listOf(Stages.F7.isActiveState, Stages.WitherKing.isActiveState.map(Boolean::not))
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

    override fun initialize() {
        on<PostExtractRenderEntityEvent> { event ->
            val state = event.state as? WitherRenderState ?: return@on
            val wither = event.entity as? WitherBoss ?: return@on

            if (wither.maxHealth == 300f) return@on
            if (SETTING_BOX.get()) {
                deferredWithers.add(Triple(state.x, state.y, state.z))
            } else if (state.outlineColor == 0) state.outlineColor = SETTING_BOX_WIRE_COLOR.get() and 0xFFFFFF
        }

        on<RenderWorldEvent> {
            deferredWithers.forEach { (x, y, z) ->
                Render3DImmediate.renderWireframeBox(
                    x, y, z,
                    1.2, 3.6,
                    SETTING_BOX_WIRE_COLOR.getColor(),
                    phase = true,
                    lineWidth = SETTING_LINE_WIDTH.get(),
                    centered = true,
                )
                Render3DImmediate.renderFilledBox(
                    x, y, z,
                    1.2, 3.6,
                    SETTING_BOX_FILL_COLOR.getColor(),
                    centered = true,
                )
            }
            deferredWithers.clear()
        }.setEnabled(SETTING_BOX.state)
    }
}