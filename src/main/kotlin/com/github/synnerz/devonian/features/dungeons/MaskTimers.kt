package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.devonian.api.ItemUtils
import com.github.synnerz.devonian.api.Scheduler
import com.github.synnerz.devonian.api.dungeon.Dungeons
import com.github.synnerz.devonian.api.events.ChatEvent
import com.github.synnerz.devonian.api.events.RenderOverlayEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.hud.texthud.Alert
import com.github.synnerz.devonian.hud.texthud.TextHudFeature
import kotlin.math.roundToInt

private fun colorForNumber(num: Float, max: Float) = when {
    num >= max * 0.75 -> "&a"
    num >= max * 0.50 -> "&e"
    else -> "&c"
}

fun colorForNumberReverse(num: Float, max: Float) = when {
    num >= max * 0.75 -> "&c"
    num >= max * 0.50 -> "&6"
    num >= max * 0.25 -> "&e"
    else -> "&a"
}

object BonzoMask : TextHudFeature(
    "bonzoMaskTimer",
    "Displays the immunity time as well as the cooldown for bonzo mask",
    Categories.DUNGEONS,
    "catacombs",
    subcategory = "HUD"
) {
    private val SETTING_HIDE_AFTER_IM = addSwitch(
        "hideAfterImm",
        false,
        "Hides the entire bonzo display after the immunity timer is over",
        "Hide Bonzo After IMM"
    )
    private val SETTING_PROC_ALERT = addSwitch(
        "procAlert",
        false,
        "Displays an alert whenever the bonzo mask is used",
        "Bonzo Proc Alert"
    )
    private val SETTING_PROC_ALERT_TIME = addSlider(
        "procAlertTime",
        1.0,
        0.0, 10.0,
        "The amount of time the alert will display for (in seconds)",
        "Bonzo Proct Alert Time"
    )
    private val SETTING_ONLY_SHOW_IN_BOSS = addSwitch(
        "onlyShowInBoss",
        false,
        "Only displays the hud while inside of a dungeon boss room",
        "Bonzo Only In Boss"
    )
    private const val IMMUNITY_TIME = 3 * 1000L
    private val cooldownItemRegex = "^Cooldown: (\\d+)s$".toRegex()
    private val maskRegex = "^Your( ⚚)? Bonzo's Mask saved your life!$".toRegex()
    private var startedAt = -1L
    private var COOLDOWN_TIME = 180
    private var cdStartedAt = -1L

    override fun initialize() {
        on<ChatEvent> { event ->
            if (event.matches(maskRegex) == null) return@on

            startedAt = System.currentTimeMillis() + IMMUNITY_TIME

            val helmLore = minecraft.player?.inventory?.getItem(39) ?: return@on
            val lore = ItemUtils.lore(helmLore) ?: return@on
            val timeIdx = lore.indexOfLast { it.contains("Cooldown: ") }
            if (timeIdx == -1) return@on
            val cdStr = lore[timeIdx]
            val match = cooldownItemRegex.matchEntire(cdStr)?.groupValues?.drop(1) ?: return@on
            val time = match[0].toIntOrNull() ?: return@on

            COOLDOWN_TIME = time
            cdStartedAt = System.currentTimeMillis() + (COOLDOWN_TIME * 1000L)

            if (!SETTING_PROC_ALERT.get()) return@on
            Alert.show("&9Bonzo Mask Used", (SETTING_PROC_ALERT_TIME.get() * 1000).roundToInt(), playSound = false)
        }

        on<RenderOverlayEvent> {
            if (SETTING_HIDE_AFTER_IM.get() && startedAt == -1L) return@on
            if (SETTING_ONLY_SHOW_IN_BOSS.get() && !Dungeons.inBoss.value) return@on

            if (cdStartedAt == -1L) {
                setLine("&9Bonzo's Mask&f: &l&aREADY")
                draw(it.ctx)
                return@on
            }

            val timeImmune = (startedAt - System.currentTimeMillis()) / 1000f
            val secondImmune = "%.2fs".format(timeImmune)
            val timeCooldown = (cdStartedAt - System.currentTimeMillis()) / 1000f
            val secondCooldown = "%.2fs".format(timeCooldown)
            val cooldownStr = if (timeCooldown <= 0f) "&l&aREADY" else "(${secondCooldown})"
            val str = when {
                timeImmune > 0 -> "${colorForNumber(timeImmune, IMMUNITY_TIME / 1000f)}$secondImmune"
                else -> "${colorForNumberReverse(timeCooldown, COOLDOWN_TIME.toFloat())}$cooldownStr"
            }

            setLine("&9Bonzo's Mask&f: $str")
            draw(it.ctx)
            if (SETTING_HIDE_AFTER_IM.get() && timeImmune <= 0f)
                startedAt = -1
        }
    }

    override fun getEditText(): List<String> = listOf("&9Bonzo's Mask&f: &a3.00s")

    override fun onWorldChange(event: WorldChangeEvent) {
        startedAt = -1
        cdStartedAt = -1
        COOLDOWN_TIME = 180
        clearLines()
    }
}

object SpiritMask : TextHudFeature(
    "spiritMaskTimer",
    "Displays the immunity time as well as the cooldown for spirit mask",
    Categories.DUNGEONS,
    "catacombs",
    subcategory = "HUD"
) {
    private val SETTING_HIDE_AFTER_IM = addSwitch(
        "hideAfterImm",
        false,
        "Hides the entire spirit display after the immunity timer is over",
        "Hide Spirit After IMM"
    )
    private val SETTING_PROC_ALERT = addSwitch(
        "procAlert",
        false,
        "Displays an alert whenever the spirit mask is used",
        "Spirit Proc Alert"
    )
    private val SETTING_PROC_ALERT_TIME = addSlider(
        "procAlertTime",
        1.0,
        0.0, 10.0,
        "The amount of time the alert will display for (in seconds)",
        "Spirit Proct Alert Time"
    )
    private val SETTING_ONLY_SHOW_IN_BOSS = addSwitch(
        "onlyShowInBoss",
        false,
        "Only displays the hud while inside of a dungeon boss room",
        "Spirit Only In Boss"
    )
    private const val IMMUNITY_TIME = 1 * 1000L
    private var COOLDOWN_TIME = 30 * 1000L
    private val maskRegex = "^Second Wind Activated! Your Spirit Mask saved your life!$".toRegex()
    private var startedAt = -1L
    private var cdStartedAt = -1L

    override fun initialize() {
        on<ChatEvent> { event ->
            if (event.matches(maskRegex) == null) return@on

            startedAt = System.currentTimeMillis() + IMMUNITY_TIME
            cdStartedAt = System.currentTimeMillis() + COOLDOWN_TIME

            if (!SETTING_PROC_ALERT.get()) return@on
            Alert.show("&fSpirit Mask Used", (SETTING_PROC_ALERT_TIME.get() * 1000).roundToInt(), playSound = false)
        }

        on<RenderOverlayEvent> {
            if (SETTING_HIDE_AFTER_IM.get() && startedAt == -1L) return@on
            if (SETTING_ONLY_SHOW_IN_BOSS.get() && !Dungeons.inBoss.value) return@on

            if (cdStartedAt == -1L) {
                setLine("&fSpirit Mask&f: &l&aREADY")
                draw(it.ctx)
                return@on
            }

            val timeImmune = (startedAt - System.currentTimeMillis()) / 1000f
            val secondImmune = "%.2fs".format(timeImmune)
            val timeCooldown = (cdStartedAt - System.currentTimeMillis()) / 1000f
            val secondCooldown = "%.2fs".format(timeCooldown)
            val cooldownStr = if (timeCooldown <= 0f) "&l&aREADY" else "(${secondCooldown})"
            val str = when {
                timeImmune > 0 -> "${colorForNumber(timeImmune, IMMUNITY_TIME / 1000f)}$secondImmune"
                else -> "${colorForNumberReverse(timeCooldown, COOLDOWN_TIME / 1000f)}$cooldownStr"
            }

            setLine("&fSpirit Mask&f: $str")
            draw(it.ctx)
            if (SETTING_HIDE_AFTER_IM.get() && timeImmune <= 0f)
                startedAt = -1
        }
    }

    override fun getEditText(): List<String> = listOf("&fSpirit Mask&f: &a1.00s")

    override fun onWorldChange(event: WorldChangeEvent) {
        startedAt = -1
        cdStartedAt = -1
        clearLines()
    }
}

object PhoenixTimer : TextHudFeature(
    "phoenixTimer",
    "Displays the immunity time as well as the cooldown for phoenix pet",
    Categories.DUNGEONS,
    "catacombs",
    subcategory = "HUD"
) {
    private val SETTING_HIDE_AFTER_IM = addSwitch(
        "hideAfterImm",
        false,
        "Hides the entire phoenix display after the immunity timer is over",
        "Hide Phoenix After IMM"
    )
    private val SETTING_PROC_ALERT = addSwitch(
        "procAlert",
        false,
        "Displays an alert whenever the phoenix pet is used",
        "Phoenix Proc Alert"
    )
    private val SETTING_PROC_ALERT_TIME = addSlider(
        "procAlertTime",
        1.0,
        0.0, 10.0,
        "The amount of time the alert will display for (in seconds)",
        "Phoenix Proct Alert Time"
    )
    private val SETTING_ONLY_SHOW_IN_BOSS = addSwitch(
        "onlyShowInBoss",
        false,
        "Only displays the hud while inside of a dungeon boss room",
        "Phoenix Only In Boss"
    )
    private const val IMMUNITY_TIME = 4 * 1000L
    private var COOLDOWN_TIME = 60 * 1000L
    private val maskRegex = "^Your Phoenix Pet saved you from certain death!$".toRegex()
    private var startedAt = -1L
    private var cdStartedAt = -1L

    override fun initialize() {
        on<ChatEvent> { event ->
            if (event.matches(maskRegex) == null) return@on

            startedAt = System.currentTimeMillis() + IMMUNITY_TIME
            cdStartedAt = System.currentTimeMillis() + COOLDOWN_TIME

            if (!SETTING_PROC_ALERT.get()) return@on
            Alert.show("&cPhoenix Used", (SETTING_PROC_ALERT_TIME.get() * 1000).roundToInt(), playSound = false)
        }

        on<RenderOverlayEvent> {
            if (SETTING_HIDE_AFTER_IM.get() && startedAt == -1L) return@on
            if (SETTING_ONLY_SHOW_IN_BOSS.get() && !Dungeons.inBoss.value) return@on

            if (cdStartedAt == -1L) {
                setLine("&cPhoenix&f: &l&aREADY")
                draw(it.ctx)
                return@on
            }

            val timeImmune = (startedAt - System.currentTimeMillis()) / 1000f
            val secondImmune = "%.2fs".format(timeImmune)
            val timeCooldown = (cdStartedAt - System.currentTimeMillis()) / 1000f
            val secondCooldown = "%.2fs".format(timeCooldown)
            val cooldownStr = if (timeCooldown <= 0f) "&l&aREADY" else "(${secondCooldown})"
            val str = when {
                timeImmune > 0 -> "${colorForNumber(timeImmune, IMMUNITY_TIME / 1000f)}$secondImmune"
                else -> "${colorForNumberReverse(timeCooldown, COOLDOWN_TIME / 1000f)}$cooldownStr"
            }

            setLine("&cPhoenix&f: $str")
            draw(it.ctx)
            if (SETTING_HIDE_AFTER_IM.get() && timeImmune <= 0f)
                startedAt = -1
        }
    }

    override fun getEditText(): List<String> = listOf("&cPhoenix&f: &a4.00s")

    override fun onWorldChange(event: WorldChangeEvent) {
        startedAt = -1
        cdStartedAt = -1
        clearLines()
    }
}