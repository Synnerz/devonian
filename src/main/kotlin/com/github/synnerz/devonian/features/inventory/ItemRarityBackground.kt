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

object ItemRarityBackground : Feature(
    "itemRarityBackground",
    subcategory = "Inventory",
) {
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
        val color = rarities.entries.find { type.contains(it.key) }?.value ?: return

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