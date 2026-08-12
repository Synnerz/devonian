package com.github.synnerz.devonian.features.misc

import com.github.synnerz.devonian.api.ChatUtils
import com.github.synnerz.devonian.api.bufimgrenderer.BufferedImageRenderer
import com.github.synnerz.devonian.api.bufimgrenderer.BufferedImageUploader
import com.github.synnerz.devonian.api.events.ChatEvent
import com.github.synnerz.devonian.api.events.RenderOverlayEvent
import com.github.synnerz.devonian.api.events.TickEvent
import com.github.synnerz.devonian.config.Config
import com.github.synnerz.devonian.hud.texthud.TextHudFeature
import com.github.synnerz.devonian.utils.BasicState
import com.github.synnerz.devonian.utils.BoundingBox
import com.github.synnerz.devonian.utils.StringUtils
import com.github.synnerz.devonian.utils.render.states.TexturedQuadRenderState
import net.minecraft.client.gui.render.TextureSetup
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import net.minecraft.network.chat.Style
import net.minecraft.resources.Identifier
import org.joml.Matrix3x2f
import java.time.Instant
import java.time.temporal.ChronoUnit

object CenturyCakeTimer : TextHudFeature(
    "centuryCakeTimer",
    "timer until need eat again",
    subcategory = "General",
) {
    private val SETTING_CHAT_HELPER = addSwitch(
        "chatHelper",
        true,
        "When eating a cake, shows a message displaying which cakes have not been eaten.",
        "Nom Nom Helper",
    )
    private val SETTING_ONLY_SHOW_EXPIRED = addSwitch(
        "onlyShowExpired",
        false,
        "",
        "Only Show When Expired",
    )

    private const val CONFIG_KEY = "cakeEatTime"
    private var expireTime = Instant.EPOCH
    private val CAKES = linkedMapOf(
        "+1\uE013 Pet Luck" to ("§5purple" to "§d"),
        "+3\uE008 Defense" to ("§alime" to "§a"),
        "+10\uE010 Health" to ("§dpink" to "§c"),
        "+5\uE003 Intelligence" to ("§bcyan (bright)" to "§b"),
        "+2\uE00D Strength" to ("§4red" to "§c"),
        "+1\uE021 Sea Creature Chance" to ("§1blue" to "§3"),
        "+5\uE051 Farming Fortune" to ("§6brown" to "§6"),
        "+10\uE022 Speed" to ("§eyellow" to "§f"),
        "+5\uE054 Foraging Fortune" to ("§fwhite" to "§6"),
        "+2\uE00B Ferocity" to ("§6orange" to "§c"),
        "+5\uE053 Mining Fortune" to ("§3teal" to "§6"),
        "+1\uE028 Vitality" to ("§2green" to "§4"),
        "+1\uE027 True Defense" to ("§8gray" to "§f"),
        "+1\uE01A Magic Find" to ("§0black" to "§b"),
        "+10\uE020 Rift Time" to ("§5dark purple §7and §2green" to "§a"),
        "+1\uE006 Cold Resistance" to ("§blight blue (dull)" to "§b"),
        "+1\uE077 Tracking" to ("§5purple" to "§d"),
        "+5\uE023 Sweep" to ("§agreen" to "§2"),
        "+1\uE05B Hunting Fortune" to ("§dpink §7and §blight blue" to "§d"),
        "+1\uE025 Treasure Chance" to ("§6orange §7and §blight blue" to "§6"),
    )
    private val eatRegex1 = "^Yum! You gain (.+?) for 48 hours!$".toRegex()
    private val eatRegex2 = "^Big Yum! You refresh (.+?) for 48 hours!$".toRegex()

    override fun getBounds(): BoundingBox {
        val bounds = super.getBounds()
        val d = 15.0 * scale
        return BoundingBox(
            bounds.x - d * 1.25,
            bounds.y + (bounds.h - d) * 0.5,
            bounds.w + d * 1.25,
            d
        )
    }

    private var lastEatTime = 0L
    private var lastEat = linkedMapOf<String, Pair<String, String>>()
    private var lastMsg: Component? = null
    private val show = BasicState(true)

    fun onEat(cake: String) {
        expireTime = Instant.now().plus(2L, ChronoUnit.DAYS)

        if (!SETTING_CHAT_HELPER.get()) return

        val t = System.currentTimeMillis()
        if (t - lastEatTime > 5L * 60L * 1000L || lastEat.isEmpty()) lastEat = LinkedHashMap(CAKES)
        lastEatTime = t

        lastEat.remove(cake)
        val msg = if (lastEat.isEmpty()) Component.literal("§aAll cakes eaten!")
            else Component.literal("§bEaten ${CAKES.size - lastEat.size}/${CAKES.size} cakes.")
                .withStyle(
                    Style.EMPTY.withHoverEvent(
                        HoverEvent.ShowText(
                            Component.literal(
                                "§aMissing:\n" +
                                lastEat.entries.joinToString("\n") {
                                    "${it.value.second}${it.key} §r§7(${it.value.first}§7)"
                                }
                            )
                        )
                    )
                )

        lastMsg?.let { ChatUtils.deleteMessage(it) }
        ChatUtils.sendMessage(msg)
        lastMsg = msg
    }

    override fun initialize() {
        Config.set(CONFIG_KEY, expireTime.toString())

        Config.onAfterLoad {
            Config.get<String>(CONFIG_KEY)?.let {
                try {
                    expireTime = Instant.parse(it)
                } catch (_: Exception) {}
            }
        }

        Config.onPreSave {
            Config.set(CONFIG_KEY, expireTime.toString())
        }

        on<TickEvent> {
            show.value = if (expireTime.epochSecond == 0L) {
                setLine("&cNONE")
                true
            } else {
                val time = Instant.now().until(expireTime, ChronoUnit.MILLIS)

                setLine(
                    StringUtils.colorForNumber(time, 60L * 60L * 1000L) +
                    StringUtils.formatTime(time, 0, 2)
                )

                if (time < 0L) expireTime = Instant.EPOCH
                false
            } || !SETTING_ONLY_SHOW_EXPIRED.get()
        }

        on<RenderOverlayEvent> { event ->
            draw(event.ctx)

            val bounds = getBounds()
            event.ctx.guiRenderState.addGuiElement(
                TexturedQuadRenderState(
                    BufferedImageRenderer.pipeline,
                    TextureSetup(
                        iconUploader.textureView, null, null,
                        iconUploader.sampler, null, null,
                    ),
                    Matrix3x2f(event.ctx.pose()),
                    (bounds.x + bounds.h * 0.1).toFloat(), (bounds.y + bounds.h * 0.1).toFloat(),
                    (bounds.x + bounds.h * 1.0).toFloat(), (bounds.y + bounds.h * 1.0).toFloat(),
                    0f, 0f,
                    1f, 1f,
                    0xFFFFFFFF.toInt(),
                    event.ctx.scissorStack.peek(),
                )
            )
        }.setEnabled(show)

        on<ChatEvent> { event ->
            event.matches(eatRegex1)?.getOrNull(0)?.let {
                onEat(it)
                return@on
            }
            event.matches(eatRegex2)?.getOrNull(0)?.let {
                onEat(it)
                return@on
            }
        }
    }

    override fun getEditText(): List<String> = listOf("1d 12h 34m 56s")

    private val mcidIcon = Identifier.fromNamespaceAndPath("devonian", "cake")!!
    private val iconUploader = BufferedImageUploader.fromResource("/assets/devonian/cake.png")!!
        .register(mcidIcon)

}