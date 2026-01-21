package com.github.synnerz.devonian.features.dungeons.clear

import com.github.synnerz.devonian.api.dungeon.Stages
import com.github.synnerz.devonian.api.events.PreExtractRenderEntityEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.utils.BasicState
import com.github.synnerz.devonian.utils.render.Render3DImmediate
import net.minecraft.world.entity.ambient.Bat
import java.awt.Color

object HighlightBat : Feature(
    "highlightBat",
    "",
    Categories.DUNGEONS,
    "catacombs",
    subcategory = "Highlights",
) {
    override fun createRequirements(): List<BasicState<Boolean>?> {
        return super.createRequirements() + listOf(Stages.Clear.isActiveState)
    }

    private val SETTING_COLOR = addColorPicker(
        "color",
        Color(255, 0, 0, 160).rgb,
        "",
        "Bat Color",
    )

    override fun initialize() {
        on<PreExtractRenderEntityEvent> { event ->
            val ent = event.entity as? Bat ?: return@on

            if (ent.maxHealth == 6f) return@on
            if (!ent.isAlive) return@on

            val pos = ent.getPosition(event.pt)
            Render3DImmediate.renderFilledBox(
                pos.x,
                pos.y,
                pos.z,
                ent.bbWidth.toDouble(), ent.bbHeight.toDouble(),
                SETTING_COLOR.getColor(),
                centered = true,
            )
        }
    }
}