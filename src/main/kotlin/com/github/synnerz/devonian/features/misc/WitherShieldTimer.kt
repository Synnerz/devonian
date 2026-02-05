package com.github.synnerz.devonian.features.misc

import com.github.synnerz.devonian.api.ItemUtils
import com.github.synnerz.devonian.api.Location
import com.github.synnerz.devonian.api.events.*
import com.github.synnerz.devonian.hud.texthud.TextHudFeature
import com.github.synnerz.devonian.utils.BasicState
import com.github.synnerz.devonian.utils.BlockTypes
import com.github.synnerz.devonian.utils.StringUtils
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.InteractionHand
import net.minecraft.world.phys.HitResult
import kotlin.jvm.optionals.getOrNull
import kotlin.math.max

object WitherShieldTimer : TextHudFeature(
    "witherShieldTimer",
    "",
    subcategory = "General",
) {
    override fun createRequirements(): List<BasicState<Boolean>?> {
        return super.createRequirements() + listOf(Location.stateInSkyblock)
    }

    private val SETTING_HIDE_READY = addSwitch(
        "hideReady",
        false,
        "Hides the WitherShieldTimer whenever its cooldown is done (instead of staying on with \"READY\" as text)",
        "WitherShieldTimer Hide",
    )
    private val SETTING_COMPACT_MODE = addSwitch(
        "compactMode",
        false,
        "When enabled, it'll make the WitherShieldTimer not have any words only numbers",
        "WitherShieldTimer Compact"
    )

    private var useTime = 0
    private var cooldown = 0
    private var isCooldownPending = false

    private fun onUse(hand: InteractionHand) {
        val player = minecraft.player ?: return
        val item = player.getItemInHand(hand)
        val data = ItemUtils.extraAttributes(item) ?: return

        val scrolls = data.getList("ability_scroll").getOrNull() ?: return
        val hasShield = scrolls.any { it.asString().getOrNull() == "WITHER_SHIELD_SCROLL" }
        if (!hasShield) return

        useTime = EventBus.serverTicks()
        cooldown = if (scrolls.size == 3) 100 else 200
        isCooldownPending = true
    }

    override fun initialize() {
        on<ClientThreadServerTickEvent> {
            var str = "&aREADY"
            if (cooldown > 0) {
                val ttl = useTime + cooldown - EventBus.serverTicks()
                if (ttl < 0 && (isCooldownPending || ttl < -20)) {
                    cooldown = 0
                    isCooldownPending = false
                } else {
                    str =
                        StringUtils.colorForNumber(ttl, cooldown) +
                        "%.2fs".format(max(ttl, 0) * 0.05)
                    if (SETTING_COMPACT_MODE.get()) {
                        setLine(str)
                        return@on
                    }
                }
            }

            setLine("&6Shield: $str")
        }

        on<RenderOverlayEvent> { event ->
            if (SETTING_HIDE_READY.get() && cooldown == 0) return@on

            draw(event.ctx)
        }

        on<UseItemEvent> { event ->
            if (cooldown > 0 && EventBus.serverTicks() < cooldown + useTime) return@on

            onUse(event.hand)
        }

        on<UseItemOnEvent> { event ->
            if (cooldown > 0 && EventBus.serverTicks() < cooldown + useTime) return@on

            if (event.blockHitResult.type == HitResult.Type.MISS) return@on
            val w = minecraft.level ?: return@on

            val block = w.getBlockState(event.blockHitResult.blockPos).block
            if (BlockTypes.Interactable.contains(block)) return@on

            onUse(event.hand)
        }

        on<SoundPlayEvent> { event ->
            when (event.underlyingEvent) {
                SoundEvents.GENERIC_EXPLODE.value() -> {
                    if (event.volume == 1f && event.pitch == 1f) isCooldownPending = false
                }

                SoundEvents.PLAYER_LEVELUP -> {
                    if (event.volume == 1f && event.pitch == 3f) {
                        cooldown = 0
                        isCooldownPending = false
                    }
                }
            }
        }
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        useTime = 0
        cooldown = 0
        isCooldownPending = false
    }

    override fun getEditText(): List<String> = if (SETTING_COMPACT_MODE.get()) listOf("&a1.00s") else listOf("&6Shield: &aREADY")
}