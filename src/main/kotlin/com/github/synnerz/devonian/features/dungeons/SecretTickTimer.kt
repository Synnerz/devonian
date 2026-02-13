package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.devonian.api.dungeon.Dungeons
import com.github.synnerz.devonian.api.events.*
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.hud.texthud.TextHudFeature
import com.github.synnerz.devonian.utils.BasicState
import com.github.synnerz.devonian.utils.StringUtils
import net.minecraft.network.protocol.game.ClientboundSetTimePacket

object SecretTickTimer : TextHudFeature(
    "secretTickTimer",
    "",
    Categories.DUNGEONS,
    "catacombs",
    subcategory = "HUD"
) {
    override fun createRequirements(): List<BasicState<Boolean>?> {
        return super.createRequirements() + listOf(Dungeons.inBoss.map(Boolean::not))
    }

    private var secretTicks = 0

    override fun initialize() {
        on<PacketReceivedEvent> { event ->
            val packet = event.packet
            if (packet !is ClientboundSetTimePacket) return@on
            if (Dungeons.timeElapsed.value == 0) return@on

            secretTicks = 20 - (packet.gameTime % 20).toInt()
        }

        on<ServerTickEvent> {
            secretTicks = if (secretTicks <= 0) 20 else secretTicks -1
        }

        on<ClientThreadServerTickEvent> {
            setLine("${StringUtils.colorForNumber(secretTicks, 20)}%.2fs".format(secretTicks * 0.05))
        }

        on<RenderOverlayEvent> {
            draw(it.ctx)
        }
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        secretTicks = 0
    }

    override fun getEditText(): List<String> = listOf("&220s")
}