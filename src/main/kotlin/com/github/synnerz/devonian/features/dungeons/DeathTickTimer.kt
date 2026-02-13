package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.devonian.api.dungeon.Dungeons
import com.github.synnerz.devonian.api.events.*
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.hud.texthud.TextHudFeature
import com.github.synnerz.devonian.utils.StringUtils
import net.minecraft.network.protocol.game.ClientboundSetTimePacket

object DeathTickTimer : TextHudFeature(
    "deathTickTimer",
    "",
    Categories.DUNGEONS,
    "catacombs",
    subcategory = "HUD"
) {
    private var deathTicks = 0

    override fun initialize() {
        on<PacketReceivedEvent> { event ->
            val packet = event.packet
            if (packet !is ClientboundSetTimePacket) return@on
            if (Dungeons.timeElapsed.value != 0) return@on

            deathTicks = 40 - (packet.gameTime % 40).toInt()
        }

        on<ServerTickEvent> {
            deathTicks = if (deathTicks <= 0) 40 else deathTicks - 1
        }

        on<ClientThreadServerTickEvent> {
            setLine("${StringUtils.colorForNumber(deathTicks, 40)}%.2fs".format(deathTicks * 0.05))
        }

        on<RenderOverlayEvent> {
            draw(it.ctx)
        }
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        deathTicks = 0
    }

    override fun getEditText(): List<String> = listOf("&240s")
}