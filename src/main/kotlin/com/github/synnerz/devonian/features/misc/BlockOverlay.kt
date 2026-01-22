package com.github.synnerz.devonian.features.misc

import com.github.synnerz.devonian.api.events.BeforeBlockOutlineEvent
import com.github.synnerz.devonian.api.events.RenderWorldEvent
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.utils.render.Render3DImmediate
import net.minecraft.world.level.EmptyBlockGetter
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.EntityHitResult
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.phys.shapes.VoxelShape
import java.awt.Color

object BlockOverlay : Feature(
    "blockOverlay",
    "Adds a more customizable Block Overlay (highlighting the block you are looking at).",
    subcategory = "Tweaks",
) {
    private val SETTING_BOX_ENTITY = addSwitch(
        "boxEntity",
        false,
        "Highlights any entity you are looking at.",
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
        "Sets the phase to false if you're in third person, otherwise it is always set to true.",
        "Dynamic Phase",
    )

    private var harharImLosingMyFuckingSanity: VoxelShape? = null
    private var offset = Vec3(0.0, 0.0, 0.0)

    override fun initialize() {
        on<BeforeBlockOutlineEvent> { event ->
            val hit = event.hitResult ?: return@on
            event.cancel()

            when (hit.type) {
                HitResult.Type.MISS -> return@on

                HitResult.Type.ENTITY -> {
                    if (!SETTING_BOX_ENTITY.get()) return@on
                    val entity = (hit as? EntityHitResult)?.entity ?: return@on
                    Shapes.create(entity.boundingBox)
                    offset = entity.getPosition(event.renderContext.tickCounter().getGameTimeDeltaPartialTick(false))
                }

                HitResult.Type.BLOCK -> {
                    val world = minecraft.level ?: return@on
                    val blockPos = (event.hitResult as? BlockHitResult)?.blockPos ?: return@on
                    val camera = minecraft.gameRenderer.mainCamera
                    // accurate bounding box
                    val shape = world.getBlockState(blockPos)
                        .getShape(
                            EmptyBlockGetter.INSTANCE,
                            blockPos,
                            CollisionContext.of(camera.entity)
                        )
                    harharImLosingMyFuckingSanity = shape
                    offset = Vec3(blockPos)
                }
            }
        }

        on<RenderWorldEvent> {
            val shape = harharImLosingMyFuckingSanity ?: return@on
            harharImLosingMyFuckingSanity = null
            val isFirstPerson = minecraft.options.cameraType.isFirstPerson

            Render3DImmediate.renderWireframeShape(
                shape,
                offset.x, offset.y, offset.z,
                SETTING_WIRE_COLOR.getColor(),
                SETTING_WIRE_WIDTH.get(),
                if (SETTING_DYNAMIC_PHASE.get()) isFirstPerson else SETTING_WIRE_PHASE.get(),
            )
            Render3DImmediate.renderFilledShape(
                shape,
                offset.x, offset.y, offset.z,
                SETTING_FILL_COLOR.getColor(),
                if (SETTING_DYNAMIC_PHASE.get()) isFirstPerson else SETTING_FILL_PHASE.get(),
            )
        }
    }
}