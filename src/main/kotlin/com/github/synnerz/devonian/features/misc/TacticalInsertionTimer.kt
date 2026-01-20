package com.github.synnerz.devonian.features.misc

import com.github.synnerz.devonian.api.ItemUtils
import com.github.synnerz.devonian.api.events.*
import com.github.synnerz.devonian.hud.texthud.TextHudFeature
import net.minecraft.world.InteractionHand

object TacticalInsertionTimer : TextHudFeature(
    "tacticalInsertionTimer",
    "Displays a 3 second timer whenever you right click Tactical Insertion",
    subcategory = "General"
) {
    private var timer = 0

    override fun initialize() {
        on<SoundPlayEvent> { event ->
            if (event.sound != "minecraft:item.flintandsteel.use" || event.pitch != 0.74603176f) return@on
            if (timer != -1) return@on
            val heldItem = minecraft.player?.getItemInHand(InteractionHand.MAIN_HAND) ?: return@on
            val sbId = ItemUtils.skyblockId(heldItem) ?: return@on
            if (sbId != "TACTICAL_INSERTION") return@on

            timer = EventBus.serverTicks() + 60
        }

        on<ClientThreadServerTickEvent> {
            if (timer == 0) return@on

            val seconds = (timer - EventBus.serverTicks()) * 0.05
            val colorCode = when {
                seconds > 1.5 -> "&a"
                seconds > 0.5 -> "&e"
                else -> "&c"
            }

            setLine("$colorCode%.2fs".format(seconds))

            if (seconds <= 0) timer = 0
        }

        on<RenderOverlayEvent> {
            if (timer == 0) return@on

            draw(it.ctx)
        }
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        timer = 0
    }

    override fun getEditText(): List<String> = listOf("&a3.00s")
}