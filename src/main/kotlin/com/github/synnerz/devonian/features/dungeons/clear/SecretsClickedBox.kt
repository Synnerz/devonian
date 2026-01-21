package com.github.synnerz.devonian.features.dungeons.clear

import com.github.synnerz.devonian.api.Scheduler
import com.github.synnerz.devonian.api.dungeon.DungeonEvent
import com.github.synnerz.devonian.api.events.ChatEvent
import com.github.synnerz.devonian.api.events.RenderWorldEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.utils.render.Render3DImmediate
import net.minecraft.core.BlockPos
import net.minecraft.world.level.EmptyBlockGetter
import net.minecraft.world.phys.shapes.CollisionContext
import java.awt.Color

object SecretsClickedBox : Feature(
    "secretsClickedBox",
    "Highlights the secrets you have clicked surrounding them with a box, if a chest secret for example is locked the color will change to red.",
    Categories.DUNGEONS,
    "catacombs",
    subcategory = "Highlights",
) {
    private val lockedChestRegex = "^That chest is locked!$".toRegex()
    private val SETTING_OUTLINE_SUCCESS_COLOR = addColorPicker(
        "outlineSuccessColor",
        Color(0, 255, 255, 255).rgb,
        "",
        "Clicked Block Outline Color"
    )
    private val SETTING_FILLED_SUCCESS_COLOR = addColorPicker(
        "filledSuccessColor",
        Color(0, 255, 255, 50).rgb,
        "",
        "Clicked Block Filled Color"
    )
    private val SETTING_OUTLINE_FAILED_COLOR = addColorPicker(
        "outlineFailedColor",
        Color(255, 0, 0, 255).rgb,
        "",
        "Locked Block Outline Color"
    )
    private val SETTING_FILLED_FAILED_COLOR = addColorPicker(
        "filledFailedColor",
        Color(255, 0, 0, 50).rgb,
        "",
        "Locked Block Filled Color"
    )
    var clickedBlock: BlockPos? = null
    var wasLocked = false

    override fun initialize() {
        on<DungeonEvent.SecretClicked> {
            clickedBlock = BlockPos(it.x.toInt(), it.y.toInt(), it.z.toInt())
            val prevBlock = clickedBlock
            Scheduler.scheduleTask(20) {
                if (clickedBlock !== prevBlock) return@scheduleTask
                clickedBlock = null
                wasLocked = false
            }
        }

        on<ChatEvent> { event ->
            event.matches(lockedChestRegex) ?: return@on
            wasLocked = true
        }

        on<RenderWorldEvent> {
            val pos = clickedBlock ?: return@on
            val camera = minecraft.gameRenderer.mainCamera
            val blockShape = minecraft.level?.getBlockState(clickedBlock!!)
                ?.getShape(
                    EmptyBlockGetter.INSTANCE,
                    clickedBlock!!,
                    CollisionContext.of(camera.entity)
                ) ?: return@on

            Render3DImmediate.renderWireframeShape(
                blockShape,
                pos.x.toDouble(),
                pos.y.toDouble(),
                pos.z.toDouble(),
                if (wasLocked) SETTING_OUTLINE_FAILED_COLOR.getColor() else SETTING_OUTLINE_SUCCESS_COLOR.getColor(),
                phase = true,
            )
            Render3DImmediate.renderFilledShape(
                blockShape,
                pos.x.toDouble(),
                pos.y.toDouble(),
                pos.z.toDouble(),
                if (wasLocked) SETTING_FILLED_FAILED_COLOR.getColor() else SETTING_FILLED_SUCCESS_COLOR.getColor(),
                phase = false,
            )
        }
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        clickedBlock = null
        wasLocked = false
    }
}