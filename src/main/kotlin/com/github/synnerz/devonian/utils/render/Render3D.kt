package com.github.synnerz.devonian.utils.render

import com.github.synnerz.devonian.Devonian
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext
import net.minecraft.client.MinecraftClient
import net.minecraft.client.font.TextRenderer
import net.minecraft.client.render.*
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.util.math.ColorHelper
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.RotationAxis
import net.minecraft.util.shape.VoxelShape
import org.joml.Matrix4f
import java.awt.Color
import kotlin.math.sqrt

object Render3D {
    val camera = Devonian.minecraft.gameRenderer.camera

    /**
     * - Draws a filled shape
     * @param ctx The WorldRenderContext
     * @param shape The VoxelShape to render
     * @param ox The x offset
     * @param oy The Y offset
     * @param oz The Z offset
     * @param color The color
     * @param phase Whether to render through walls or not (`false` = no)
     */
    fun renderFilled(
        ctx: WorldRenderContext,
        shape: VoxelShape,
        ox: Double,
        oy: Double,
        oz: Double,
        color: Color,
        phase: Boolean = false
    ) {
        val consumers = ctx.consumers() ?: return
        val matrices = ctx.matrixStack() ?: return
        val layer = if (phase) DLayers.TRIANGLE_STRIP_ESP else DLayers.TRIANGLE_STRIP

        // TODO: make this more efficient later on
        //  (this does way too many calls but shouldn't matter much as of right now)
        shape.forEachBox { minX: Double, minY: Double, minZ: Double, maxX: Double, maxY: Double, maxZ: Double ->
            VertexRendering.drawFilledBox(
                matrices,
                consumers.getBuffer(layer),
                minX + ox, minY + oy, minZ + oz,
                maxX + ox, maxY + oy, maxZ + oz,
                color.red / 255f, color.green / 255f, color.blue / 255f, color.alpha / 255f
            )
        }
    }

    /**
     * - Draws a outline shape
     * @param ctx The WorldRenderContext
     * @param shape The VoxelShape to render
     * @param ox The x offset
     * @param oy The Y offset
     * @param oz The Z offset
     * @param color The color
     * @param phase Whether to render through walls or not (`false` = no)
     */
    fun renderOutline(
        ctx: WorldRenderContext,
        shape: VoxelShape,
        ox: Double,
        oy: Double,
        oz: Double,
        color: Color,
        phase: Boolean = false
    ) {
        val consumers = ctx.consumers() ?: return
        val matrices = ctx.matrixStack() ?: return
        val layer = if (phase) DLayers.LINES_ESP else DLayers.LINES

        VertexRendering.drawOutline(
            matrices,
            consumers.getBuffer(layer),
            shape,
            ox, oy, oz,
            color.rgb
        )
    }

    @JvmOverloads
    fun renderBeam(
        ctx: WorldRenderContext,
        x: Double,
        y: Double,
        z: Double,
        color: Color = Color.CYAN,
        phase: Boolean = false
    ) {
        val consumers = ctx.consumers() ?: return
        val matrices = ctx.matrixStack() ?: return
        val partialTicks = MinecraftClient.getInstance().renderTickCounter.getTickProgress(true)
        val cam = Devonian.minecraft.gameRenderer.camera.pos

        matrices.push()
        matrices.translate(
            x - cam.x,
            y - cam.y,
            z - cam.z
            )

        renderBeam(
            matrices,
            consumers,
            partialTicks,
            MinecraftClient.getInstance().world!!.time,
            color.rgb,
            phase
        )

        matrices.pop()
    }

    @JvmOverloads
    fun renderBox(
        ctx: WorldRenderContext,
        x: Double,
        y: Double,
        z: Double,
        width: Double,
        height: Double,
        color: Color = Color.CYAN,
        phase: Boolean = false
    ) {
        val consumers = ctx.consumers() ?: return
        val matrices = ctx.matrixStack() ?: return
        val cam = camera.pos.negate()
        val layer = if (phase) DLayers.LINES_ESP else DLayers.LINES
        val cx = x + 0.5
        val cz = z + 0.5
        val halfWidth = width / 2

        matrices.push()
        matrices.translate(cam.x, cam.y, cam.z)

        VertexRendering.drawBox(
            matrices,
            consumers.getBuffer(layer),
            cx - halfWidth, y, cz - halfWidth,
            cx + halfWidth, y + height, cz + halfWidth,
            color.red / 255f, color.green / 255f, color.blue / 255f, color.alpha / 255f
        )

        matrices.pop()
    }

    @JvmOverloads
    fun renderFilledBox(
        ctx: WorldRenderContext,
        x: Double,
        y: Double,
        z: Double,
        width: Double,
        height: Double,
        color: Color = Color.CYAN,
        phase: Boolean = false
    ) {
        val consumers = ctx.consumers() ?: return
        val matrices = ctx.matrixStack() ?: return
        val cam = camera.pos.negate()
        val layer = if (phase) DLayers.TRIANGLE_STRIP_ESP else DLayers.TRIANGLE_STRIP
        val cx = x + 0.5
        val cz = z + 0.5
        val halfWidth = width / 2

        matrices.push()
        matrices.translate(cam.x, cam.y, cam.z)

        VertexRendering.drawFilledBox(
            matrices,
            consumers.getBuffer(layer),
            cx - halfWidth, y, cz - halfWidth,
            cx + halfWidth, y + height, cz + halfWidth,
            color.red / 255f, color.green / 255f, color.blue / 255f, color.alpha / 255f
        )

        matrices.pop()
    }

    @JvmOverloads
    fun renderWaypoint(
        ctx: WorldRenderContext,
        x: Double,
        y: Double,
        z: Double,
        color: Color = Color.CYAN,
        title: String? = null,
        increase: Boolean = false,
        phase: Boolean = false
    ) {
        val pos = Devonian.minecraft.player ?: return
        val dx = x - pos.x
        val dy = y + 5 - pos.y
        val dz = z - pos.z

        renderFilledBox(ctx, x, y, z, 1.0, 1.0, Color(color.red, color.green, color.blue, 80), phase)
        renderBox(ctx, x, y, z, 1.0, 1.0, color, phase)
        renderBeam(ctx, x, y, z, color, phase)
        renderString(
            title ?: "%.2fm".format(sqrt(dx * dx + dy * dy + dz * dz)),
            x + 0.5,
            y + 5.0,
            z + 0.5,
            bgBox = true,
            increase = increase,
            phase = phase
        )
    }

    @JvmOverloads
    fun renderString(
        text: String,
        x: Double,
        y: Double,
        z: Double,
        scale: Float = 1f,
        bgBox: Boolean = false,
        increase: Boolean = false,
        phase: Boolean = false
    ) {
        var toScale = scale
        val matrices = Matrix4f()
        val textRenderer = Devonian.minecraft.textRenderer
        val dx = x - camera.pos.x
        val dy = y - camera.pos.y
        val dz = z - camera.pos.z

        toScale *= if (increase) sqrt(dx * dx + dy * dy + dz * dz).toFloat() / 120f else 0.025f

        matrices
            .translate(
                dx.toFloat(),
                dy.toFloat(),
                dz.toFloat()
            )
            .rotate(camera.rotation)
            .scale(toScale, -toScale, toScale)

        val consumer = Devonian.minecraft.bufferBuilders.entityVertexConsumers
        val offset = -textRenderer.getWidth(text) / 2f
        val textLayer = if (phase) TextRenderer.TextLayerType.SEE_THROUGH else TextRenderer.TextLayerType.NORMAL

        if (bgBox) {
            textRenderer.draw(
                text,
                offset,
                0f,
                0x20FFFFFF,
                true,
                matrices,
                consumer,
                textLayer,
                (Devonian.minecraft.options.getTextBackgroundOpacity(0.25f) * 255).toInt() shl 24,
                LightmapTextureManager.MAX_BLOCK_LIGHT_COORDINATE
            )
        }

        textRenderer.draw(
            text,
            offset,
            0f,
            0xFFFFFFFF.toInt(),
            true,
            matrices,
            consumer,
            textLayer,
            0,
            LightmapTextureManager.MAX_BLOCK_LIGHT_COORDINATE
        )
        consumer.draw()
    }

    /**
     * - Edited version of renderBeam from mojang's code to allow for different Rendering Layers
     *  so that see through walls can be set
     * @author Mojang
     */
    private fun renderBeam(
        matrices: MatrixStack,
        vertexConsumer: VertexConsumerProvider,
        partialTicks: Float,
        worldTime: Long,
        color: Int,
        phase: Boolean = false
    ) {
        val opaqueLayer = if (phase) DLayers.BEACON_BEAM_OPAQUE_ESP else DLayers.BEACON_BEAM_OPAQUE
        val translucentLayer = if (phase) DLayers.BEACON_BEAM_TRANSLUCENT_ESP else DLayers.BEACON_BEAM_TRANSLUCENT
        val heightScale = 1f
        val height = 320
        val innerRadius = 0.2f
        val outerRadius = 0.25f
        val time = Math.floorMod(worldTime, 40) + partialTicks
        val fixedTime = -time
        val wavePhase = MathHelper.fractionalPart(fixedTime * 0.2f - MathHelper.floor(fixedTime * 0.1f).toFloat())
        val animationStep = -1f + wavePhase
        var renderYOffset = height.toFloat() * heightScale * (0.5f / innerRadius) + animationStep

        matrices.push()
        matrices.translate(0.5, 0.0, 0.5)

        matrices.push()
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(time * 2.25f - 45.0f))

        renderBeamLayer(
            matrices,
            vertexConsumer.getBuffer(opaqueLayer),
            color,
            0f,
            innerRadius,
            innerRadius,
            0f,
            -innerRadius,
            0f,
            0f,
            -innerRadius,
            renderYOffset,
            animationStep
        )

        matrices.pop()

        renderYOffset = height.toFloat() * heightScale + animationStep

        renderBeamLayer(
            matrices,
            vertexConsumer.getBuffer(translucentLayer),
            ColorHelper.withAlpha(32, color),
            -outerRadius,
            -outerRadius,
            outerRadius,
            -outerRadius,
            -outerRadius,
            outerRadius,
            outerRadius,
            outerRadius,
            renderYOffset,
            animationStep
        )

        matrices.pop()
    }

    private fun renderBeamLayer(
        matrices: MatrixStack,
        vertices: VertexConsumer,
        color: Int,
        x1: Float,
        z1: Float,
        x2: Float,
        z2: Float,
        x3: Float,
        z3: Float,
        x4: Float,
        z4: Float,
        v1: Float,
        v2: Float
    ) {
        val entry = matrices.peek()
        renderBeamFace(entry, vertices, color, x1, z1, x2, z2, v1, v2)
        renderBeamFace(entry, vertices, color, x4, z4, x3, z3, v1, v2)
        renderBeamFace(entry, vertices, color, x2, z2, x4, z4, v1, v2)
        renderBeamFace(entry, vertices, color, x3, z3, x1, z1, v1, v2)
    }

    private fun renderBeamFace(
        matrix: MatrixStack.Entry,
        vertices: VertexConsumer,
        color: Int,
        x1: Float,
        z1: Float,
        x2: Float,
        z2: Float,
        v1: Float,
        v2: Float
    ) {
        renderBeamVertex(matrix, vertices, color, 320, x1, z1, 1f, v1)
        renderBeamVertex(matrix, vertices, color, 0, x1, z1, 1f, v2)
        renderBeamVertex(matrix, vertices, color, 0, x2, z2, 0f, v2)
        renderBeamVertex(matrix, vertices, color, 320, x2, z2, 0f, v1)
    }

    private fun renderBeamVertex(
        matrix: MatrixStack.Entry,
        vertices: VertexConsumer,
        color: Int,
        y: Int,
        x: Float,
        z: Float,
        u: Float,
        v: Float
    ) {
        vertices.vertex(matrix, x, y.toFloat(), z).color(color).texture(u, v).overlay(OverlayTexture.DEFAULT_UV)
            .light(15728880).normal(matrix, 0.0f, 1.0f, 0.0f)
    }
}