package com.github.synnerz.devonian.features.debug.packetlogger.s2c

import com.github.synnerz.devonian.config.json.JsonDataObject
import com.github.synnerz.devonian.features.debug.packetlogger.ISerializer
import com.github.synnerz.devonian.utils.StringUtils.colorCodes
import net.minecraft.network.protocol.PacketFlow
import net.minecraft.network.protocol.PacketType
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket
import net.minecraft.network.protocol.game.GamePacketTypes

object SystemChatPacket : ISerializer<ClientboundSystemChatPacket> {
    override val type: PacketType<ClientboundSystemChatPacket> = GamePacketTypes.CLIENTBOUND_SYSTEM_CHAT
    override val flow: PacketFlow = PacketFlow.CLIENTBOUND

    override fun serialize(
        packet: ClientboundSystemChatPacket,
        obj: JsonDataObject
    ) {
        obj.set("overlay", packet.overlay)
        obj.set("content", packet.content.colorCodes())
        obj.set("content_", packet.content.string)
    }
}