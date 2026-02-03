package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.devonian.api.dungeon.Dungeons
import com.github.synnerz.devonian.api.events.PostExtractRenderEntityEvent
import com.github.synnerz.devonian.api.events.RenderWorldEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.utils.render.Render3DImmediate
import net.minecraft.client.player.LocalPlayer
import net.minecraft.client.renderer.entity.state.EntityRenderState
import net.minecraft.world.entity.player.Player
import java.awt.Color

object HighlightTeammates : Feature(
    "highlightTeammates",
    "",
    Categories.DUNGEONS,
    subcategory = "Highlights",
) {
    private val SETTING_BOX = addSwitch(
        "box",
        false,
        "box instead of outline",
        "Teammate Highlight Box",
    )
    private val SETTING_SELF = addSelection(
        "self",
        1,
        listOf("None", "Glow", "Box"),
        "",
        "Highlight Self",
    )
    private val SETTING_BOX_WIRE_ALPHA = addDecimalSlider(
        "wireAlpha",
        1.0,
        0.0, 1.0,
        "",
        "Teammate Box Wire Alpha",
    )
    private val SETTING_BOX_FILL_ALPHA = addDecimalSlider(
        "fillAlpha",
        0.0,
        0.0, 1.0,
        "",
        "Teammate Box Fill Alpha",
    )
    private val SETTING_LINE_WIDTH = addSlider(
        "lineWidth",
        3.0,
        0.0, 10.0,
        "",
        "Teammate Box Line Width",
    )

    private val deferred = mutableListOf<Pair<EntityRenderState, Color>>()

    override fun initialize() {
        on<PostExtractRenderEntityEvent> { event ->
            val ent = event.entity as? Player? ?: return@on
            val teammate = Dungeons.players[ent.gameProfile.name] ?: return@on
            val state = event.state

            state.outlineColor = 0

            val type = if (ent is LocalPlayer) SETTING_SELF.get()
                else if (SETTING_BOX.get()) 2 else 1
            if (type == 0) return@on

            if (type == 2) {
                deferred.add(state to teammate.role.color)
            } else state.outlineColor = teammate.role.colorRgb
        }

        on<RenderWorldEvent> {
            deferred.forEach { (state, col) ->
                Render3DImmediate.renderWireframeBox(
                    state.x, state.y, state.z,
                    state.boundingBoxWidth.toDouble(), state.boundingBoxHeight.toDouble(),
                    col.let {
                        Color(it.red, it.green, it.blue, (SETTING_BOX_WIRE_ALPHA.get() * 255.0).toInt())
                    },
                    SETTING_LINE_WIDTH.get(),
                    phase = true,
                    centered = true,
                )
                Render3DImmediate.renderFilledBox(
                    state.x, state.y, state.z,
                    state.boundingBoxWidth.toDouble(), state.boundingBoxHeight.toDouble(),
                    col.let {
                        Color(it.red, it.green, it.blue, (SETTING_BOX_FILL_ALPHA.get() * 255.0).toInt())
                    },
                    phase = false,
                    centered = true,
                )
            }
            deferred.clear()
        }.setEnabled(SETTING_BOX.state.zip(SETTING_SELF.state) { a, b -> a || b == 2 })
    }
}