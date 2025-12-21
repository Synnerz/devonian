package com.github.synnerz.devonian.features.misc

import com.github.synnerz.barrl.utils.RendererLayers
import com.github.synnerz.devonian.api.events.BeforeBlockOutlineEvent
import com.github.synnerz.devonian.api.events.RenderWorldEvent
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.utils.math.ShapeUtils
import net.minecraft.world.level.EmptyBlockGetter
import net.minecraft.world.phys.*
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.phys.shapes.VoxelShape
import org.joml.Vector3f
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

        on<RenderWorldEvent> { event ->
            val EXPAND = 0.01

            val shape = harharImLosingMyFuckingSanity ?: return@on
            harharImLosingMyFuckingSanity = null
            val isFirstPerson = minecraft.options.cameraType.isFirstPerson

            val camPos = event.ctx.worldState().cameraRenderState.pos ?: return@on
            event.ctx.matrices().pushPose()
            event.ctx.matrices().translate(camPos.reverse())
            val mat = event.ctx.matrices().last()

            if (SETTING_FILL_COLOR.getColor().alpha > 0) {
                val layer = if (if (SETTING_DYNAMIC_PHASE.get()) isFirstPerson else SETTING_FILL_PHASE.get()) {
                    if (SETTING_FILL_COLOR.getColor().alpha == 255) RendererLayers.QUADS_OPAQUE_ESP
                    else RendererLayers.QUADS_TRANSLUCENT_ESP
                } else {
                    if (SETTING_FILL_COLOR.getColor().alpha == 255) RendererLayers.QUADS_OPAQUE
                    else RendererLayers.QUADS_TRANSLUCENT
                }
                val consumer = minecraft.renderBuffers().bufferSource().getBuffer(layer)

                val faces = ShapeUtils.getFaces(shape)
                for (i in faces.indices step 3) {
                    val x = faces[i + 0] + offset.x
                    val y = faces[i + 1] + offset.y
                    val z = faces[i + 2] + offset.z
                    var dir = camPos.subtract(x, y, z)
                    dir = dir.scale(EXPAND / dir.length())

                    consumer
                        .addVertex(mat, (x + dir.x).toFloat(), (y + dir.y).toFloat(), (z + dir.z).toFloat())
                        .setColor(SETTING_FILL_COLOR.get())
                }
            }

            if (SETTING_WIRE_COLOR.getColor().alpha > 0) {
                val consumer = minecraft.renderBuffers().bufferSource().getBuffer(
                    RendererLayers.lines(
                        SETTING_WIRE_WIDTH.get(),
                        if (SETTING_DYNAMIC_PHASE.get()) isFirstPerson else SETTING_WIRE_PHASE.get(),
                        SETTING_WIRE_COLOR.getColor().alpha == 255
                    )
                )

                shape.forAllEdges { x1, y1, z1, x2, y2, z2 ->
                    val x1 = (x1 + offset.x).toFloat()
                    val y1 = (y1 + offset.y).toFloat()
                    val z1 = (z1 + offset.z).toFloat()
                    val x2 = (x2 + offset.x).toFloat()
                    val y2 = (y2 + offset.y).toFloat()
                    val z2 = (z2 + offset.z).toFloat()

                    val normalized = Vector3f(x2 - x1, y2 - y1, z2 - z1).normalize()

                    consumer
                        .addVertex(mat, x1, y1, z1)
                        .setColor(SETTING_WIRE_COLOR.get())
                        .setNormal(mat, normalized)

                    consumer
                        .addVertex(mat, x2, y2, z2)
                        .setColor(SETTING_WIRE_COLOR.get())
                        .setNormal(mat, normalized)
                }
            }

            event.ctx.matrices().popPose()
        }
    }
}