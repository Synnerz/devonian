package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.devonian.api.dungeon.Stages
import com.github.synnerz.devonian.api.events.PacketReceivedEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.utils.BasicState
import com.github.synnerz.devonian.utils.StringUtils.clearCodes
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket

object TerminalHideCompletion : Feature(
    "terminalHideCompletion",
    "Hides the completed title during terminals",
    Categories.DUNGEONS,
    "catacombs",
    subcategory = "F7"
) {
    override fun createRequirements(): List<BasicState<Boolean>?> {
        return super.createRequirements() + listOf(Stages.Terminals.isActiveState)
    }

    private val SETTING_ONLY_SHOW_OWN = addSwitch(
        "onlyShowOwn",
        true,
        "Only shows the completed title by you",
        "Terminal Title Only Own"
    )
    private val terminalTitleRegex = "^(\\w{1,16}) (?:activated a (?:terminal|lever)|completed a device)! \\(\\d+/\\d+\\)$".toRegex()

    override fun initialize() {
        on<PacketReceivedEvent> { event ->
            val text = when (val packet = event.packet) {
                is ClientboundSetSubtitleTextPacket -> packet.text.string.clearCodes()
                is ClientboundSetTitleTextPacket -> packet.text.string.clearCodes()
                else -> null
            } ?: return@on
            val match = terminalTitleRegex.matchEntire(text)?.groupValues?.drop(1) ?: return@on
            val player = minecraft.player ?: return@on
            val name = player.name.string
            if (SETTING_ONLY_SHOW_OWN.get() && match[0] == name) return@on

            event.cancel()
        }
    }
}