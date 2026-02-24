package com.github.synnerz.devonian.utils.render

import com.github.synnerz.devonian.Devonian
import com.github.synnerz.devonian.api.events.EventBus
import com.github.synnerz.devonian.api.events.PostClientInitEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.config.Config
import com.github.synnerz.devonian.config.ConfigData
import com.mojang.blaze3d.buffers.GpuBuffer
import com.mojang.blaze3d.buffers.Std140Builder
import com.mojang.blaze3d.buffers.Std140SizeCalculator
import com.mojang.blaze3d.systems.RenderPass
import com.mojang.blaze3d.systems.RenderSystem
import net.fabricmc.fabric.api.resource.v1.ResourceLoader
import net.fabricmc.fabric.api.resource.v1.pack.PackActivationType
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.network.chat.TextColor
import net.minecraft.resources.Identifier
import org.lwjgl.system.MemoryStack
import kotlin.math.pow

object ChromaText {
    val UBO_SIZE = Std140SizeCalculator()
        .putFloat() // time
        .putFloat() // size
        .putFloat() // speed
        .putFloat() // lightness
        .putFloat() // chroma
        .get()
    lateinit var ubo: GpuBuffer
    val SETTING_SIZE = ConfigData.DecimalSlider(
        "chroma\$size",
        3.0,
        null,
        0.0, 16.0,
        "width of each color band",
        "Chroma Size",
        "Chroma",
    ).also {
        Config.registerCategory(it, Categories.GLOBAL, "Chroma")
    }
    val SETTING_SPEED = ConfigData.DecimalSlider(
        "chroma\$speed",
        1.8,
        null,
        -8.0, 8.0,
        "speed the colors move",
        "Chroma Speed",
        "Chroma",
    ).also {
        Config.registerCategory(it, Categories.GLOBAL, "Chroma")
    }
    val SETTING_LIGHTNESS = ConfigData.DecimalSlider(
        "chroma\$lightness",
        0.83,
        null,
        0.0, 1.0,
        "how 'light' each color should be",
        "Chroma Lightness",
        "Chroma",
    ).also {
        Config.registerCategory(it, Categories.GLOBAL, "Chroma")
    }
    val SETTING_CHROMA = ConfigData.DecimalSlider(
        "chroma\$chroma",
        0.2,
        null,
        0.0, 0.4,
        "similar to saturation",
        "Chroma 'Chroma'",
        "Chroma",
    ).also {
        Config.registerCategory(it, Categories.GLOBAL, "Chroma")
    }

    fun initialize() {
        ResourceLoader.registerBuiltinPack(
            Identifier.fromNamespaceAndPath("devonian", "chroma_text_shader"),
            Devonian.container,
            PackActivationType.DEFAULT_ENABLED,
        )

        EventBus.on<PostClientInitEvent> {
            ubo = RenderSystem.getDevice().createBuffer(
                { "Devonian Chroma Info" },
                136,
                UBO_SIZE.toLong(),
            )
        }
    }

    fun updateBuffer(time: Float) {
        if (!::ubo.isInitialized) return
        MemoryStack.stackPush().use { stack ->
            val buf = Std140Builder.onStack(stack, UBO_SIZE)
                .putFloat(time / 20f)
                .putFloat(SETTING_SIZE.get().toFloat().pow(1.2f))
                .putFloat(SETTING_SPEED.get().toFloat())
                .putFloat(SETTING_LIGHTNESS.get().toFloat())
                .putFloat(SETTING_CHROMA.get().toFloat())
                .get()
            RenderSystem.getDevice()
                .createCommandEncoder()
                .writeToBuffer(ubo.slice(), buf)
        }
    }

    fun bindUniforms(renderPass: RenderPass) {
        if (!::ubo.isInitialized) return
        renderPass.setUniform("DevonianChromaInfo", ubo)
    }

    /*
    // for when you can pass in float colors :(
    private fun store(f: Float, min: Float, max: Float): Short {
        val v = MathUtils.rescale(f.toDouble(), min.toDouble(), max.toDouble(), 0.0, 1.0)
        return (v * 0xFFFF).toInt().toShort()
    }

    private fun pack(v1: Short, v2: Short): Float {
        return Float.fromBits((v1.toInt() shl 16) or v2.toInt())
    }

    private fun packChromaColor(size: Float, speed: Float, lightness: Float, chroma: Float, alpha: Float): FloatArray {
        return floatArrayOf(
            pack(
                store(size, 0f, 16f),
                store(speed, -16f, 16f),
            ),
            pack(
                store(lightness, 0f, 1f),
                store(chroma, 0f, 0.4f),
            ),
            alpha,
            Float.fromBits(0x01ABCDEF),
        )
    }
    */

    @JvmOverloads
    fun createComp(
        str: String,
        base: Style = Style.EMPTY,
        shadow: Boolean = true,
    ) = Component.literal(str).withStyle(createStyle(base, shadow))

    @JvmOverloads
    fun createStyle(
        base: Style = Style.EMPTY,
        shadow: Boolean = true,
    ) = Style(
        TextColor.fromRgb(0xABCDEF),
        if (shadow) 0xFFFEDCBA.toInt() else null,
        base.isBold,
        base.isItalic,
        base.isUnderlined,
        base.isStrikethrough,
        base.isObfuscated,
        base.clickEvent,
        base.hoverEvent,
        base.insertion,
        base.font,
    )
}