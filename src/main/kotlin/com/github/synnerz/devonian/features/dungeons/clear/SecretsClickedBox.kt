package com.github.synnerz.devonian.features.dungeons.clear

import com.github.synnerz.devonian.api.Scheduler
import com.github.synnerz.devonian.api.dungeon.DungeonEvent
import com.github.synnerz.devonian.api.events.ChatEvent
import com.github.synnerz.devonian.api.events.RenderWorldEvent
import com.github.synnerz.devonian.api.events.UseItemOnEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.utils.render.Render3DImmediate
import net.minecraft.core.BlockPos
import net.minecraft.world.level.EmptyBlockGetter
import net.minecraft.world.level.block.Block
import net.minecraft.world.phys.shapes.CollisionContext
import java.awt.Color

object SecretsClickedBox : Feature(
    "secretsClickedBox",
    "Highlights the secrets you have clicked surrounding them with a box. " +
    "If a chest secret is locked the color will change to red.",
    Categories.DUNGEONS,
    "catacombs",
    subcategory = "Highlights",
) {
    private val lockedChestRegex = "^That chest is locked!$".toRegex()
    private val SETTING_OUTLINE_SUCCESS_COLOR = addColorPicker(
        "outlineSuccessColor",
        Color(0, 255, 255, 255).rgb,
        "",
        "Clicked Block Outline Color",
    )
    private val SETTING_FILLED_SUCCESS_COLOR = addColorPicker(
        "filledSuccessColor",
        Color(0, 255, 255, 50).rgb,
        "",
        "Clicked Block Filled Color",
    )
    private val SETTING_OUTLINE_FAILED_COLOR = addColorPicker(
        "outlineFailedColor",
        Color(255, 0, 0, 255).rgb,
        "",
        "Locked Chest Outline Color",
    )
    private val SETTING_FILLED_FAILED_COLOR = addColorPicker(
        "filledFailedColor",
        Color(255, 0, 0, 50).rgb,
        "",
        "Locked Chest Filled Color",
    )
    private val SETTING_OUTLINE_PHASE = addSwitch(
        "outlinePhase",
        true,
        "Whether it should show the outline through walls",
        "Outline Phase"
    )
    private val SETTING_FILLED_PHASE = addSwitch(
        "filledPhase",
        false,
        "Whether it should show the filled through walls",
        "Filled Phase"
    )
    private val blocks = mutableListOf<SecretData>()
    private var lastChest: BlockPos? = null

    data class SecretData(
        val x: Double,
        val y: Double,
        val z: Double,
        val isItem: Boolean = false,
        val isBat: Boolean = false,
        var locked: Boolean = false,
    ) {
        val blockPos = BlockPos(x.toInt(), y.toInt(), z.toInt())

        init {
            if (!blocks.any { it.blockPos == blockPos }) {
                blocks.add(this)
                Scheduler.scheduleTask(20) {
                    blocks.remove(this)
                }
            }
        }

        fun render() {
            if (isItem || isBat) {
                Render3DImmediate.renderWireframeShape(
                    SMALL_SHAPE,
                    x, y, z,
                    SETTING_OUTLINE_SUCCESS_COLOR.getColor(),
                    phase = SETTING_OUTLINE_PHASE.get(),
                )
                Render3DImmediate.renderFilledShape(
                    SMALL_SHAPE,
                    x, y, z,
                    SETTING_FILLED_SUCCESS_COLOR.getColor(),
                    phase = SETTING_FILLED_PHASE.get(),
                )
                return
            }

            val camera = minecraft.gameRenderer.mainCamera()
            val camEntity = camera.entity() ?: return
            val blockShape = minecraft.level?.getBlockState(blockPos)
                ?.getShape(
                    EmptyBlockGetter.INSTANCE,
                    blockPos,
                    CollisionContext.of(camEntity)
                ) ?: return

            Render3DImmediate.renderWireframeShape(
                blockShape,
                x, y, z,
                if (locked) SETTING_OUTLINE_FAILED_COLOR.getColor() else SETTING_OUTLINE_SUCCESS_COLOR.getColor(),
                phase = SETTING_OUTLINE_PHASE.get(),
            )
            Render3DImmediate.renderFilledShape(
                blockShape,
                x, y, z,
                if (locked) SETTING_FILLED_FAILED_COLOR.getColor() else SETTING_FILLED_SUCCESS_COLOR.getColor(),
                phase = SETTING_FILLED_PHASE.get(),
            )
        }

        companion object {
            val SMALL_SHAPE = Block.column(8.0, 0.0, 8.0)
        }
    }

    override fun initialize() {
        on<DungeonEvent.SecretClicked> {
            SecretData(it.x, it.y, it.z)
        }
        on<DungeonEvent.SecretPickup> {
            SecretData(it.x, it.y, it.z, true)
        }
        on<DungeonEvent.SecretBat> {
            SecretData(it.x, it.y, it.z, isBat = true)
        }
        on<DungeonEvent.SecretBatSound> {
            Scheduler.scheduleTask {
                SecretData(it.x, it.y, it.z, isBat = true)
            }
        }

        on<ChatEvent> { event ->
            event.matches(lockedChestRegex) ?: return@on
            blocks.find { it.blockPos == lastChest }?.let { it.locked = true }
        }

        on<UseItemOnEvent> { event ->
            lastChest = event.blockHitResult.blockPos
        }

        on<RenderWorldEvent> {
            blocks.forEach(SecretData::render)
        }
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        blocks.clear()
    }
}