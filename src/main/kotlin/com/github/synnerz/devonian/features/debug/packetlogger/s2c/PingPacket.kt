package com.github.synnerz.devonian.features.debug.packetlogger.s2c

import com.github.synnerz.devonian.config.json.JsonDataObject
import com.github.synnerz.devonian.features.debug.packetlogger.ISerializer
import net.minecraft.network.protocol.PacketFlow
import net.minecraft.network.protocol.PacketType
import net.minecraft.network.protocol.common.ClientboundPingPacket
import net.minecraft.network.protocol.common.CommonPacketTypes

object PingPacket : ISerializer<ClientboundPingPacket> {
    override val type: PacketType<ClientboundPingPacket> = CommonPacketTypes.CLIENTBOUND_PING
    override val flow: PacketFlow = PacketFlow.CLIENTBOUND

    override fun serialize(
        packet: ClientboundPingPacket,
        obj: JsonDataObject
    ) {
        obj.set("id", packet.id)
    }
}