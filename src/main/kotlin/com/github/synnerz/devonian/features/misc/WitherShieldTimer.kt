package com.github.synnerz.devonian.features.misc

import com.github.synnerz.devonian.api.ItemUtils
import com.github.synnerz.devonian.api.events.ClientThreadServerTickEvent
import com.github.synnerz.devonian.api.events.EventBus
import com.github.synnerz.devonian.api.events.RenderOverlayEvent
import com.github.synnerz.devonian.api.events.SoundPlayEvent
import com.github.synnerz.devonian.api.events.UseItemEvent
import com.github.synnerz.devonian.api.events.UseItemOnEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.hud.texthud.TextHudFeature
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
                }
            }

            setLine("&6Shield: $str")
        }

        on<RenderOverlayEvent> { event ->
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

    override fun getEditText(): List<String> = listOf("&6Shield: &aREADY")
}