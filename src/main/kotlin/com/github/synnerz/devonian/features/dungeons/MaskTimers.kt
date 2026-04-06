package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.devonian.api.ItemUtils
import com.github.synnerz.devonian.api.bufimgrenderer.BufferedImageRenderer
import com.github.synnerz.devonian.api.bufimgrenderer.BufferedImageUploader
import com.github.synnerz.devonian.api.dungeon.Dungeons
import com.github.synnerz.devonian.api.events.ChatEvent
import com.github.synnerz.devonian.api.events.RenderOverlayEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.hud.texthud.Alert
import com.github.synnerz.devonian.hud.texthud.TextHudFeature
import com.github.synnerz.devonian.utils.BoundingBox
import com.github.synnerz.devonian.utils.render.states.TexturedQuadRenderState
import net.minecraft.client.gui.render.TextureSetup
import net.minecraft.resources.Identifier
import org.joml.Matrix3x2f
import kotlin.math.roundToInt

fun colorForNumberReverse(num: Float, max: Float) = when {
    num >= max * 0.75 -> "&c"
    num >= max * 0.50 -> "&6"
    num >= max * 0.25 -> "&e"
    else -> "&a"
}

abstract class ImmunityTimer(val formattedName: String, itemName: String, configName: String) : TextHudFeature(
    configName,
    "Displays the immunity time as well as the cooldown for $itemName.",
    Categories.DUNGEONS,
    "catacombs",
    subcategory = "HUD",
    subcategories = listOf("Alerts"),
) {
    private val SETTING_STYLE = addSelection(
        "style",
        0,
        listOf("Icon", "Text"),
        "",
        "HUD Style",
    )
    private val SETTING_ONLY_SHOW_IMMUNITY = addSwitch(
        "hideAfterImm",
        false,
        "Only shows the $itemName hud for the immunity timer.",
        "Only Show Immunity",
    )
    private val SETTING_ONLY_SHOW_READY = addSwitch(
        "onlyShowReady",
        false,
        "Only shows the $itemName hud when ready to be used.",
        "Only Show Ready",
    )
    private val SETTING_ONLY_SHOW_IN_BOSS = addSwitch(
        "onlyShowInBoss",
        false,
        "Only displays the $itemName hud while inside of a dungeon boss room.",
        "Only In Boss",
    )
    private val SETTING_PROC_ALERT = addSwitch(
        "procAlert",
        false,
        "Displays an alert whenever the $itemName is used.",
        "Proc Alert",
        subcategory = "Alerts",
    )
    private val SETTING_PROC_ALERT_TIME = addSlider(
        "procAlertTime",
        1.0,
        0.0, 10.0,
        "The amount of time the alert will display for (in seconds).",
        "Proc Alert Time",
        subcategory = "Alerts",
    )

    protected abstract val IMMUNITY_TIME: Long
    protected abstract val triggerRegex: Regex
    private var lastProc = -1L
    private var cooldownTime = 0L
    protected abstract fun getCooldown(): Long
    protected abstract fun getIcon(): BufferedImageUploader

    override fun getBounds(): BoundingBox {
        val bounds = super.getBounds()
        if (SETTING_STYLE.get() != 0) return bounds
        val d = 15.0 * scale
        return BoundingBox(
            bounds.x - d,
            bounds.y,
            bounds.w + d,
            bounds.h,
        )
    }

    override fun initialize() {
        on<ChatEvent> { event ->
            if (!triggerRegex.matches(event.message)) return@on

            lastProc = System.currentTimeMillis()
            cooldownTime = getCooldown()

            if (SETTING_PROC_ALERT.get()) {
                Alert.show("$formattedName Used", (SETTING_PROC_ALERT_TIME.get() * 1000).roundToInt())
            }
        }

        on<RenderOverlayEvent> { event ->
            val imm = SETTING_ONLY_SHOW_IMMUNITY.get()
            val rdy = SETTING_ONLY_SHOW_READY.get()

            val str = if (lastProc == -1L) {
                if (imm && !rdy) return@on
                "&a&lREADY"
            } else {
                val t = System.currentTimeMillis()
                val dt = t - lastProc
                val immune = (IMMUNITY_TIME - dt) / 1000f
                val cooldown = (cooldownTime - dt) / 1000f

                if (cooldown < 0f) lastProc = -1L
                if (rdy && !imm) return@on

                if (immune > 0f) "&b%.2fs".format(immune)
                else if (imm) return@on
                else colorForNumberReverse(cooldown, cooldownTime / 1000f) + "%.2fs".format(cooldown)
            }

            val style = SETTING_STYLE.get()
            setLine(if (style == 0) str else "$formattedName&f: $str")

            draw(event.ctx)

            if (style != 0) return@on

            val bounds = getBounds()
            event.ctx.guiRenderState.addGuiElement(
                TexturedQuadRenderState(
                    BufferedImageRenderer.pipeline,
                    TextureSetup(
                        getIcon().textureView, null, null,
                        getIcon().sampler, null, null,
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
        }.setEnabled(SETTING_ONLY_SHOW_IN_BOSS.state.zip(Dungeons.inBoss) { a, b -> !a || b })
    }

    override fun getEditText(): List<String> = listOf(
        if (SETTING_STYLE.get() == 0) "&a&lREADY"
        else "$formattedName&f: &a&lREADY"
    )

    override fun onWorldChange(event: WorldChangeEvent) {
        lastProc = -1
        hud.clearLines()
    }
}

object BonzoMask : ImmunityTimer(
    "&9Bonzo's Mask",
    "bonzo mask",
    "bonzoMaskTimer",
) {
    override val IMMUNITY_TIME: Long = 3_000L
    override val triggerRegex: Regex = "^Your( ⚚)? Bonzo's Mask saved your life!$".toRegex()

    private const val DEFAULT_COOLDOWN = 180_000L
    private val cooldownItemRegex = "^Cooldown: (\\d+)s$".toRegex()

    override fun getCooldown(): Long {
        val helmLore = minecraft.player?.inventory?.getItem(39) ?: return DEFAULT_COOLDOWN
        val lore = ItemUtils.lore(helmLore) ?: return DEFAULT_COOLDOWN

        val timeIdx = lore.indexOfLast { it.contains("Cooldown: ") }
        if (timeIdx == -1) return DEFAULT_COOLDOWN

        val cdStr = lore[timeIdx]

        val match = cooldownItemRegex.matchEntire(cdStr)?.groupValues?.drop(1) ?: return DEFAULT_COOLDOWN
        val time = match[0].toIntOrNull() ?: return DEFAULT_COOLDOWN

        return time * 1000L
    }

    private val mcidIcon = Identifier.fromNamespaceAndPath("devonian", "dungeons/bonzo_mask")
    private val iconUploader = BufferedImageUploader.fromResource("/assets/devonian/dungeons/bonzo_mask.png")!!
        .register(mcidIcon)

    override fun getIcon(): BufferedImageUploader = iconUploader
}

object SpiritMask : ImmunityTimer(
    "&fSpirit Mask",
    "spirit mask",
    "spiritMaskTimer",
) {
    override val IMMUNITY_TIME: Long = 3_000L
    override val triggerRegex: Regex = "^Second Wind Activated! Your Spirit Mask saved your life!$".toRegex()
    override fun getCooldown(): Long = 30_000L

    private val mcidIcon = Identifier.fromNamespaceAndPath("devonian", "dungeons/spirit_mask")
    private val iconUploader = BufferedImageUploader.fromResource("/assets/devonian/dungeons/spirit_mask.png")!!
        .register(mcidIcon)

    override fun getIcon(): BufferedImageUploader = iconUploader
}

object PhoenixTimer : ImmunityTimer(
    "&cPhoenix",
    "phoenix",
    "phoenixTimer",
) {
    override val IMMUNITY_TIME: Long = 4_000L
    override val triggerRegex: Regex = "^Your Phoenix Pet saved you from certain death!$".toRegex()
    override fun getCooldown(): Long = 60_000L

    private val mcidIcon = Identifier.fromNamespaceAndPath("devonian", "dungeons/phoenix")
    private val iconUploader = BufferedImageUploader.fromResource("/assets/devonian/dungeons/phoenix.png")!!
        .register(mcidIcon)

    override fun getIcon(): BufferedImageUploader = iconUploader
}