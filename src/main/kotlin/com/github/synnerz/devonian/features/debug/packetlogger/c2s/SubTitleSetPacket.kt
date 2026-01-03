package com.github.synnerz.devonian.features.debug.packetlogger.c2s

import com.github.synnerz.devonian.config.json.JsonDataObject
import com.github.synnerz.devonian.features.debug.packetlogger.ISerializer
import com.github.synnerz.devonian.utils.StringUtils.colorCodes
import net.minecraft.network.protocol.PacketFlow
import net.minecraft.network.protocol.PacketType
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket
import net.minecraft.network.protocol.game.GamePacketTypes

object SubTitleSetPacket : ISerializer<ClientboundSetSubtitleTextPacket> {
    override val type: PacketType<ClientboundSetSubtitleTextPacket> = GamePacketTypes.CLIENTBOUND_SET_SUBTITLE_TEXT
    override val flow: PacketFlow = PacketFlow.CLIENTBOUND

    override fun serialize(packet: ClientboundSetSubtitleTextPacket, obj: JsonDataObject) {
        obj.set("text", packet.text.colorCodes())
        obj.set("text_", packet.text.string)
    }
}