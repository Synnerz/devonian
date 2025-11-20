package com.github.synnerz.devonian.features.misc

import com.github.synnerz.barrl.Context
import com.github.synnerz.devonian.api.events.BeforeBlockOutlineEvent
import com.github.synnerz.devonian.features.Feature
import net.minecraft.world.level.EmptyBlockGetter
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.EntityHitResult
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.shapes.CollisionContext
import java.awt.Color

object BlockOverlay : Feature(
    "blockOverlay",
    "Adds a more customizable Block Overlay."
) {
    private val SETTING_BOX_ENTITY = addSwitch(
        "boxEntity",
        "Highlights any entity you are looking at",
        "Highlight Entity",
        false
    )
    private val SETTING_WIRE_COLOR = addColorPicker(
        "wireColor",
        "",
        "Block Outline Color",
        Color(0, 0, 0, 102).rgb
    )
    private val SETTING_FILL_COLOR = addColorPicker(
        "fillColor",
        "",
        "Block Fill Color",
        0
    )

    override fun initialize() {
        on<BeforeBlockOutlineEvent> { event ->
            val hit = event.hitResult ?: return@on
            event.cancel()

            when (hit.type) {
                HitResult.Type.MISS -> return@on

                HitResult.Type.ENTITY -> {
                    if (!SETTING_BOX_ENTITY.get()) return@on
                    val entity = (hit as? EntityHitResult)?.entity ?: return@on
                    val pos = entity.getPosition(event.renderContext.tickCounter().getGameTimeDeltaPartialTick(false))
                    Context.Immediate?.renderBox(
                        pos.x - entity.bbWidth * 0.5f,
                        pos.y,
                        pos.z - entity.bbWidth * 0.5,
                        entity.bbWidth.toDouble(),
                        entity.bbHeight.toDouble(),
                        SETTING_WIRE_COLOR.getColor(),
                        phase = minecraft.options.cameraType.isFirstPerson,
                        translate = true
                    )
                    Context.Immediate?.renderFilledBox(
                        pos.x - entity.bbWidth * 0.5f,
                        pos.y,
                        pos.z - entity.bbWidth * 0.5,
                        entity.bbWidth.toDouble(),
                        entity.bbHeight.toDouble(),
                        SETTING_WIRE_COLOR.getColor(),
                        phase = minecraft.options.cameraType.isFirstPerson,
                        translate = true
                    )
                }

                HitResult.Type.BLOCK -> {
                    val world = minecraft.level ?: return@on
                    val blockPos = (event.hitResult as? BlockHitResult)?.blockPos ?: return@on
                    val camera = minecraft.gameRenderer.mainCamera
                    val cam = camera.position
                    // accurate bounding box
                    val blockShape = world.getBlockState(blockPos)
                        .getShape(
                            EmptyBlockGetter.INSTANCE,
                            blockPos,
                            CollisionContext.of(camera.entity)
                        )

                    Context.Immediate?.renderBoxShape(
                        blockShape,
                        blockPos.x - cam.x,
                        blockPos.y - cam.y,
                        blockPos.z - cam.z,
                        SETTING_WIRE_COLOR.getColor(),
                        minecraft.options.cameraType.isFirstPerson
                    )
                    Context.Immediate?.renderFilledShape(
                        blockShape,
                        blockPos.x - cam.x,
                        blockPos.y - cam.y,
                        blockPos.z - cam.z,
                        SETTING_FILL_COLOR.getColor(),
                        minecraft.options.cameraType.isFirstPerson
                    )
                }
            }
        }
    }
}