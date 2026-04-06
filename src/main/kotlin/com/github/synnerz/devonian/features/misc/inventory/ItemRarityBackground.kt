package com.github.synnerz.devonian.features.misc.inventory

import com.github.synnerz.devonian.api.ItemUtils
import com.github.synnerz.devonian.api.bufimgrenderer.BufferedImageRenderer
import com.github.synnerz.devonian.api.bufimgrenderer.BufferedImageUploader
import com.github.synnerz.devonian.api.events.RenderHotbarSlotEvent
import com.github.synnerz.devonian.api.events.RenderSlotEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.utils.FixedIdentityMap
import com.github.synnerz.devonian.utils.render.states.TexturedQuadRenderState
import com.google.gson.JsonParser
import net.minecraft.ChatFormatting
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.render.TextureSetup
import net.minecraft.network.chat.TextColor
import net.minecraft.resources.Identifier
import net.minecraft.world.item.ItemStack
import org.joml.Matrix3x2f
import kotlin.jvm.optionals.getOrNull
import kotlin.math.roundToInt

object ItemRarityBackground : Feature(
    "itemRarityBackground",
    subcategory = "Inventory",
) {
    private val SETTING_RENDER_OPACITY = addDecimalSlider(
        "renderOpacity",
        1.0,
        0.0, 1.0,
        "Opacity of the rarity background.",
        "Rarity Render Opacity",
    )
    private val SETTING_RENDER_MODE = addSelection(
        "renderMode",
        0,
        listOf("Outlines", "Solid", "Circle", "Line + Solid"),
        "The render mode the rarity background will use.",
        "Rarity Render Mode",
    )
    private val SETTING_RENDER_LINE_WIDTH = addSlider(
        "renderLineWidth",
        1.0,
        1.0, 5.0,
        "",
        "Rarity Render Line Width",
    )
    private val SETTING_RENDER_HOTBAR = addSwitch(
        "renderHotbar",
        true,
        "Whether to also render the highlight in the hotbar.",
        "Rarity Render Hotbar",
    )
    private val rarities = listOf(
        "COMMON" to TextColor.fromLegacyFormat(ChatFormatting.WHITE)!!.value,
        "UNCOMMON" to TextColor.fromLegacyFormat(ChatFormatting.GREEN)!!.value,
        "RARE" to TextColor.fromLegacyFormat(ChatFormatting.BLUE)!!.value,
        "EPIC" to TextColor.fromLegacyFormat(ChatFormatting.DARK_PURPLE)!!.value,
        "LEGENDARY" to TextColor.fromLegacyFormat(ChatFormatting.GOLD)!!.value,
        "MYTHIC" to TextColor.fromLegacyFormat(ChatFormatting.LIGHT_PURPLE)!!.value,
        "SPECIAL" to TextColor.fromLegacyFormat(ChatFormatting.RED)!!.value,
        "ULTIMATE" to TextColor.fromLegacyFormat(ChatFormatting.DARK_RED)!!.value,
        "ADMIN" to TextColor.fromLegacyFormat(ChatFormatting.DARK_RED)!!.value,

        "a UNCOMMON" to TextColor.fromLegacyFormat(ChatFormatting.GREEN)!!.value,
        "a RARE" to TextColor.fromLegacyFormat(ChatFormatting.BLUE)!!.value,
        "a EPIC" to TextColor.fromLegacyFormat(ChatFormatting.DARK_PURPLE)!!.value,
        "a LEGENDARY" to TextColor.fromLegacyFormat(ChatFormatting.GOLD)!!.value,
        "a MYTHIC" to TextColor.fromLegacyFormat(ChatFormatting.LIGHT_PURPLE)!!.value,
        "a DIVINE" to TextColor.fromLegacyFormat(ChatFormatting.AQUA)!!.value,
        "a VERY SPECIAL" to TextColor.fromLegacyFormat(ChatFormatting.RED)!!.value,

        "SHINY LEGENDARY" to TextColor.fromLegacyFormat(ChatFormatting.GOLD)!!.value,
        "a SHINY MYTHIC" to TextColor.fromLegacyFormat(ChatFormatting.LIGHT_PURPLE)!!.value,
    )

    private val cache = FixedIdentityMap<ItemStack, Int>(128)

    private fun render(x: Int, y: Int, item: ItemStack, ctx: GuiGraphicsExtractor) {
        if (item.isEmpty) return

        val rgb = cache.getOrPut(item) {
            val petInfo = ItemUtils.extraAttributes(item)?.getString("petInfo")?.getOrNull()
            if (petInfo != null) try {
                val pet = JsonParser.parseString(petInfo).asJsonObject
                val rarity = pet.get("tier")?.asString
                if (rarity != null) rarities.find { it.first == rarity }?.let {
                    return@getOrPut it.second
                }
            } catch (_: Exception) {}
            val lore = ItemUtils.lore(item) ?: return@getOrPut -1
            return@getOrPut findColor(lore) ?: -1
        }
        if (rgb == -1) return
        val opacity = (SETTING_RENDER_OPACITY.get() * 255.0).roundToInt()
        val color = rgb or (opacity shl 24)
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
                ctx.guiRenderState.addGuiElement(
                    TexturedQuadRenderState(
                        BufferedImageRenderer.pipeline,
                        TextureSetup(
                            blurImg.textureView, null, null,
                            blurImg.sampler, null, null,
                        ),
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
        }.prio = 0

        on<RenderHotbarSlotEvent> { event ->
            if (!SETTING_RENDER_HOTBAR.get()) return@on
            render(event.x, event.y, event.item, event.ctx)
        }.prio = 0
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        cache.clear()
    }

    private fun findColor(lore: List<String>): Int? {
        if (lore.isEmpty()) return null
        findRarity(lore.last())?.let { return it }
        return lore.stream().map { findRarity(it) }.filter { it != null }.findAny().orElseGet { null }
    }

    private fun findRarity(str: String): Int? {
        return rarities.find { str.startsWith(it.first) }?.second
    }

    private val blurId = Identifier.fromNamespaceAndPath("devonian", "blur")
    private val blurImg = BufferedImageUploader.fromResource("/assets/devonian/blur.png")!!
        .register(blurId)
}