package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.barrl.Context
import com.github.synnerz.devonian.api.events.PostRenderTileEntityEvent
import com.github.synnerz.devonian.features.Feature
import net.minecraft.client.renderer.blockentity.state.ChestRenderState
import net.minecraft.world.level.block.state.properties.ChestType
import java.awt.Color

object BoxMimicChest : Feature(
    "boxMimicChest",
    "draws box around mimic chest",
    "Dungeons",
    "catacombs"
) {
    private val SETTING_COLOR = addColorPicker(
        "color",
        Color(209, 29, 5, 160).rgb,
        "",
        "Mimic Chest Color",
    )

    override fun initialize() {
        on<PostRenderTileEntityEvent> { event ->
            val state = event.entityState as? ChestRenderState ?: return@on
            if (state.material != ChestRenderState.ChestMaterialType.TRAPPED) return@on
            if (state.type != ChestType.SINGLE) return@on

            Context.Immediate?.renderFilledBox(
                0.05, 0.0, 0.05,
                0.9, 0.9,
                SETTING_COLOR.getColor(),
                phase = false,
                translate = false,
            )
        }
    }
}