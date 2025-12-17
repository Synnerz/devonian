package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.devonian.api.events.ChatEvent
import com.github.synnerz.devonian.api.events.RenderOverlayEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.hud.texthud.TextHudFeature

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
    private const val IMMUNITY_TIME = 3 * 1000L
    private var COOLDOWN_TIME = 180 * 1000L // TODO: make this dynamic with lore
    private val maskRegex = "^Your( ⚚)? Bonzo's Mask saved your life!$".toRegex()
    private var startedAt = -1L
    private var cdStartedAt = -1L

    override fun initialize() {
        on<ChatEvent> { event ->
            if (event.matches(maskRegex) == null) return@on

            startedAt = System.currentTimeMillis() + IMMUNITY_TIME
            cdStartedAt = System.currentTimeMillis() + COOLDOWN_TIME
        }

        on<RenderOverlayEvent> {
            if (SETTING_HIDE_AFTER_IM.get() && startedAt == -1L) return@on

            if (cdStartedAt == -1L) {
                setLine("&9Bonzo's Mask&f: &l&aREADY")
                draw(it.ctx)
                return@on
            }

            val timeImmune = (startedAt - System.currentTimeMillis()) / 1000f
            val secondImmune = "%.2fs".format(timeImmune)
            val immuneStr = if (timeImmune <= 0f && startedAt != -1L) "&c" else "&a$secondImmune &7"
            val timeCooldown = (cdStartedAt - System.currentTimeMillis()) / 1000f
            val secondCooldown = "%.2fs".format(timeCooldown)
            val cooldownStr = if (timeCooldown <= 0f) "&l&aREADY" else "(${secondCooldown})"

            setLine("&9Bonzo's Mask&f: $immuneStr$cooldownStr")
            draw(it.ctx)
            if (SETTING_HIDE_AFTER_IM.get() && timeImmune <= 0f)
                startedAt = -1
        }
    }

    override fun getEditText(): List<String> = listOf("&9Bonzo's Mask&f: &a3.00s &7(180s)")

    override fun onWorldChange(event: WorldChangeEvent) {
        startedAt = -1
        cdStartedAt = -1
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
        }

        on<RenderOverlayEvent> {
            if (SETTING_HIDE_AFTER_IM.get() && startedAt == -1L) return@on

            if (cdStartedAt == -1L) {
                setLine("&fSpirit Mask&f: &l&aREADY")
                draw(it.ctx)
                return@on
            }

            val timeImmune = (startedAt - System.currentTimeMillis()) / 1000f
            val secondImmune = "%.2fs".format(timeImmune)
            val immuneStr = if (timeImmune <= 0f && startedAt != -1L) "&c" else "&a$secondImmune &7"
            val timeCooldown = (cdStartedAt - System.currentTimeMillis()) / 1000f
            val secondCooldown = "%.2fs".format(timeCooldown)
            val cooldownStr = if (timeCooldown <= 0f) "&l&aREADY" else "(${secondCooldown})"

            setLine("&fSpirit Mask&f: $immuneStr$cooldownStr")
            draw(it.ctx)
            if (SETTING_HIDE_AFTER_IM.get() && timeImmune <= 0f)
                startedAt = -1
        }
    }

    override fun getEditText(): List<String> = listOf("&fSpirit Mask&f: &a1.00s &7(30s)")

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
        "Hide Spirit After IMM"
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
        }

        on<RenderOverlayEvent> {
            if (SETTING_HIDE_AFTER_IM.get() && startedAt == -1L) return@on

            if (cdStartedAt == -1L) {
                setLine("&cPhoenix&f: &l&aREADY")
                draw(it.ctx)
                return@on
            }

            val timeImmune = (startedAt - System.currentTimeMillis()) / 1000f
            val secondImmune = "%.2fs".format(timeImmune)
            val immuneStr = if (timeImmune <= 0f && startedAt != -1L) "&c" else "&a$secondImmune &7"
            val timeCooldown = (cdStartedAt - System.currentTimeMillis()) / 1000f
            val secondCooldown = "%.2fs".format(timeCooldown)
            val cooldownStr = if (timeCooldown <= 0f) "&l&aREADY" else "(${secondCooldown})"

            setLine("&cPhoenix&f: $immuneStr$cooldownStr")
            draw(it.ctx)
            if (SETTING_HIDE_AFTER_IM.get() && timeImmune <= 0f)
                startedAt = -1
        }
    }

    override fun getEditText(): List<String> = listOf("&cPhoenix&f: &a4.00s &7(60s)")

    override fun onWorldChange(event: WorldChangeEvent) {
        startedAt = -1
        cdStartedAt = -1
        clearLines()
    }
}