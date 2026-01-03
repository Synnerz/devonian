package com.github.synnerz.devonian.features.debug.packetlogger.c2s

import com.github.synnerz.devonian.config.json.JsonDataObject
import com.github.synnerz.devonian.features.debug.packetlogger.ISerializer
import net.minecraft.network.protocol.PacketFlow
import net.minecraft.network.protocol.PacketType
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket
import net.minecraft.network.protocol.game.GamePacketTypes

object SubTitleSetPacket : ISerializer<ClientboundSetSubtitleTextPacket> {
    override val type: PacketType<ClientboundSetSubtitleTextPacket> = GamePacketTypes.CLIENTBOUND_SET_SUBTITLE_TEXT
    override val flow: PacketFlow = PacketFlow.SERVERBOUND

    override fun serialize(packet: ClientboundSetSubtitleTextPacket, obj: JsonDataObject) {
        obj.set("component", packet.text)
        obj.set("isTerminal", packet.isTerminal)
        obj.set("isSkippable", packet.isSkippable)
    }
}