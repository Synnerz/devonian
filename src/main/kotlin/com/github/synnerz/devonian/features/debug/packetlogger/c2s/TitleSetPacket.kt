package com.github.synnerz.devonian.features.debug.packetlogger.c2s

import com.github.synnerz.devonian.config.json.JsonDataObject
import com.github.synnerz.devonian.features.debug.packetlogger.ISerializer
import com.github.synnerz.devonian.utils.StringUtils.colorCodes
import net.minecraft.network.protocol.PacketFlow
import net.minecraft.network.protocol.PacketType
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket
import net.minecraft.network.protocol.game.GamePacketTypes

object TitleSetPacket : ISerializer<ClientboundSetTitleTextPacket> {
    override val type: PacketType<ClientboundSetTitleTextPacket> = GamePacketTypes.CLIENTBOUND_SET_TITLE_TEXT
    override val flow: PacketFlow = PacketFlow.CLIENTBOUND

    override fun serialize(packet: ClientboundSetTitleTextPacket, obj: JsonDataObject) {
        obj.set("text", packet.text.colorCodes())
        obj.set("text_", packet.text.string)
    }
}