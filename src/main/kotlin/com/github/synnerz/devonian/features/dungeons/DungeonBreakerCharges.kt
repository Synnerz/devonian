package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.devonian.api.ItemUtils
import com.github.synnerz.devonian.api.events.PacketReceivedEvent
import com.github.synnerz.devonian.api.events.RenderOverlayEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.hud.texthud.TextHudFeature
import net.minecraft.component.DataComponentTypes
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket

object DungeonBreakerCharges : TextHudFeature(
    "dungeonBreakerDisplay",
    "Displays the amount of charges left",
    "Dungeons",
    "catacombs"
) {
    var colorCode = "&6"
    var charges = 20
    var displayStr = "&aCharges&f: ${colorCode}${charges}"

    override fun initialize() {
        on<PacketReceivedEvent> { event ->
            val packet = event.packet
            if (packet !is ScreenHandlerSlotUpdateS2CPacket) return@on
            val itemStack = packet.stack ?: return@on
            if (ItemUtils.skyblockId(itemStack) != "DUNGEONBREAKER") return@on
            val usedCharge = itemStack.get(DataComponentTypes.DAMAGE) ?: 1

            charges = 20 - (usedCharge / 78)

            if (charges >= 15) colorCode = "&6"
            else if (charges >= 10) colorCode = "&a"
            else colorCode = "&c"

            displayStr = "&aCharges&f: ${colorCode}${charges}"
        }

        on<RenderOverlayEvent> { event ->
            setLine(displayStr)
            draw(event.ctx)
        }

        on<WorldChangeEvent> {
            charges = 20
            colorCode = "&6"
            displayStr = "&aCharges&f: ${colorCode}${charges}"
        }
    }

    override fun getEditText(): List<String> = listOf("&aCharges&f: &620")
}