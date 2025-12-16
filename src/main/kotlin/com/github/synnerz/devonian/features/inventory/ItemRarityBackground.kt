package com.github.synnerz.devonian.features.inventory

import com.github.synnerz.devonian.api.ItemUtils
import com.github.synnerz.devonian.api.bufimgrenderer.BufferedImageRenderer
import com.github.synnerz.devonian.api.bufimgrenderer.BufferedImageUploader
import com.github.synnerz.devonian.api.events.RenderHotbarSlotEvent
import com.github.synnerz.devonian.api.events.RenderSlotEvent
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.utils.TexturedQuadRenderState
import net.minecraft.ChatFormatting
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.render.TextureSetup
import net.minecraft.network.chat.TextColor
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import org.joml.Matrix3x2f
import kotlin.math.roundToInt

object ItemRarityBackground : Feature(
    "itemRarityBackground",
    subcategory = "Inventory",
) {
    private val SETTING_RENDER_OPACITY = addDecimalSlider(
        "renderOpacity",
        1.0,
        0.0, 1.0,
        "How transparent the rarity background will be",
        "Rarity Render Opacity"
    )
    private val SETTING_RENDER_MODE = addSelection(
        "renderMode",
        0,
        listOf("Outlines", "Solid", "Circle", "Line + Solid"),
        "The render mode the rarity background will use",
        "Rarity Render Mode"
    )
    private val SETTING_RENDER_LINE_WIDTH = addSlider(
        "renderLineWidth",
        1.0,
        1.0, 5.0,
        "",
        "Rarity Render Line Width"
    )
    private val rarities = linkedMapOf(
        "UNCOMMON" to (TextColor.fromLegacyFormat(ChatFormatting.GREEN)!!.value or 0xFF000000.toInt()),
        "COMMON" to (TextColor.fromLegacyFormat(ChatFormatting.WHITE)!!.value or 0xFF000000.toInt()),
        "RARE" to (TextColor.fromLegacyFormat(ChatFormatting.BLUE)!!.value or 0xFF000000.toInt()),
        "EPIC" to (TextColor.fromLegacyFormat(ChatFormatting.DARK_PURPLE)!!.value or 0xFF000000.toInt()),
        "LEGENDARY" to (TextColor.fromLegacyFormat(ChatFormatting.GOLD)!!.value or 0xFF000000.toInt()),
        "MYTHIC" to (TextColor.fromLegacyFormat(ChatFormatting.LIGHT_PURPLE)!!.value or 0xFF000000.toInt()),
        "DIVINE" to (TextColor.fromLegacyFormat(ChatFormatting.AQUA)!!.value or 0xFF000000.toInt()),
        "SPECIAL" to (TextColor.fromLegacyFormat(ChatFormatting.RED)!!.value or 0xFF000000.toInt()),
    )

    private fun render(x: Int, y: Int, item: ItemStack, ctx: GuiGraphics) {
        if (item.isEmpty) return

        val type = ItemUtils.lore(item)?.lastOrNull() ?: return
        val rgb = rarities.entries.find { type.contains(it.key) }?.value ?: return
        val opacity = (SETTING_RENDER_OPACITY.get() * 255).roundToInt()
        val color = (rgb and 0x00FFFFFF) or (opacity shl 24)
        val lineWidth = SETTING_RENDER_LINE_WIDTH.get().roundToInt()

        when (SETTING_RENDER_MODE.get()) {
            0 -> {
                ctx.fill(x, y, x + 16, y + lineWidth, color)
                ctx.fill(x, y + 16 - lineWidth, x + 16, y + 16, color)
                ctx.fill(x, y + lineWidth, x + lineWidth, y + 16 - lineWidth, color)
                ctx.fill(x + 16 - lineWidth, y + lineWidth, x + 16, y + 16 - lineWidth, color)
            }
            1 -> {
                ctx.fill(x, y, x + 16, y + 16, color)
            }
            2 -> {
                ctx.guiRenderState.submitGuiElement(
                    TexturedQuadRenderState(
                        BufferedImageRenderer.pipeline,
                        TextureSetup.singleTexture(blurImg.textureView),
                        Matrix3x2f(ctx.pose()),
                        x + 0f, y + 0f,
                        x + 16f, y + 16f,
                        0f, 0f,
                        1f, 1f,
                        color,
                        ctx.scissorStack.peek(),
                    )
                )
            }
            3 -> {
                ctx.fill(x, y, x + 16, y + lineWidth, color)
                ctx.fill(x, y + 16 - lineWidth, x + 16, y + 16, color)
                ctx.fill(x, y + lineWidth, x + lineWidth, y + 16 - lineWidth, color)
                ctx.fill(x + 16 - lineWidth, y + lineWidth, x + 16, y + 16 - lineWidth, color)
                ctx.fill(x, y, x + 16, y + 16, color)
            }
        }
    }

    override fun initialize() {
        on<RenderSlotEvent> { event ->
            render(event.slot.x, event.slot.y, event.slot.item, event.ctx)
        }

        on<RenderHotbarSlotEvent> { event ->
            render(event.x, event.y, event.item, event.ctx)
        }
    }

    private val blurId = ResourceLocation.fromNamespaceAndPath("devonian", "item_rarity_background_blur")!!
    private val blurImg = BufferedImageUploader.fromResource("/assets/devonian/blur.png")!!
        .register(blurId)
}