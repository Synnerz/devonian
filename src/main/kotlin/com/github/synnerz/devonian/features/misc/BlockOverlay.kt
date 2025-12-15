package com.github.synnerz.devonian.features.misc

import com.github.synnerz.barrl.Context
import com.github.synnerz.devonian.api.events.BeforeBlockOutlineEvent
import com.github.synnerz.devonian.api.events.RenderWorldEvent
import com.github.synnerz.devonian.features.Feature
import net.minecraft.world.level.EmptyBlockGetter
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.EntityHitResult
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.shapes.CollisionContext
import java.awt.Color

object BlockOverlay : Feature(
    "blockOverlay",
    "Adds a more customizable Block Overlay.",
    subcategory = "Tweaks",
) {
    private val SETTING_BOX_ENTITY = addSwitch(
        "boxEntity",
        false,
        "Highlights any entity you are looking at",
        "Highlight Entity",
    )
    private val SETTING_WIRE_COLOR = addColorPicker(
        "wireColor",
        Color(0, 0, 0, 102).rgb,
        "",
        "Block Outline Color",
    )
    private val SETTING_FILL_COLOR = addColorPicker(
        "fillColor",
        0,
        "",
        "Block Fill Color",
    )
    private val SETTING_WIRE_WIDTH = addSlider(
        "wireWidth",
        3.0,
        0.0, 10.0,
        "",
        "Outline Line Width",
    )
    private val SETTING_WIRE_PHASE = addSwitch(
        "wirePhase",
        true,
        "",
        "Outline Wire Phase",
    )
    private val SETTING_FILL_PHASE = addSwitch(
        "fillPhase",
        false,
        "",
        "Outline Fill Phase",
    )
    private val SETTING_DYNAMIC_PHASE = addSwitch(
        "dynamicPhase",
        false,
        "Sets the phase to false if you're in third person, otherwise it is always set to true",
        "Dynamic Phase"
    )

    private var harharImLosingMyFuckingSanity = listOf<AABB>()

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
                    harharImLosingMyFuckingSanity = listOf(
                        AABB(
                            pos.x - entity.bbWidth * 0.5,
                            pos.y,
                            pos.z - entity.bbWidth * 0.5,
                            pos.x + entity.bbWidth * 0.5,
                            pos.y + entity.bbHeight,
                            pos.z + entity.bbWidth * 0.5,
                        )
                    )
                }

                HitResult.Type.BLOCK -> {
                    val world = minecraft.level ?: return@on
                    val blockPos = (event.hitResult as? BlockHitResult)?.blockPos ?: return@on
                    val camera = minecraft.gameRenderer.mainCamera
                    // accurate bounding box
                    val blockShape = world.getBlockState(blockPos)
                        .getShape(
                            EmptyBlockGetter.INSTANCE,
                            blockPos,
                            CollisionContext.of(camera.entity)
                        )

                    harharImLosingMyFuckingSanity = blockShape.toAabbs().map { it.move(blockPos) }
                }
            }
        }

        on<RenderWorldEvent> {
            val boxes = harharImLosingMyFuckingSanity
            val isFirstPerson = minecraft.options.cameraType.isFirstPerson

            harharImLosingMyFuckingSanity = listOf()

            boxes.forEach {
                val aabb = it.inflate(0.001)
                Context.Immediate?.renderBox(
                    aabb.minX, aabb.minY, aabb.minZ,
                    aabb.maxX - aabb.minX, aabb.maxY - aabb.minY,
                    SETTING_WIRE_COLOR.getColor(),
                    phase = if (SETTING_DYNAMIC_PHASE.get()) isFirstPerson else SETTING_WIRE_PHASE.get(),
                    lineWidth = SETTING_WIRE_WIDTH.get(),
                    widthZ = aabb.maxZ - aabb.minZ
                )
                Context.Immediate?.renderFilledBox(
                    aabb.minX, aabb.minY, aabb.minZ,
                    aabb.maxX - aabb.minX, aabb.maxY - aabb.minY,
                    SETTING_FILL_COLOR.getColor(),
                    phase = if (SETTING_DYNAMIC_PHASE.get()) isFirstPerson else SETTING_FILL_PHASE.get(),
                    widthZ = aabb.maxZ - aabb.minZ
                )
            }
        }
    }
}