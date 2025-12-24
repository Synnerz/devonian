package com.github.synnerz.devonian.features.debug.packetlogger.s2c

import com.github.synnerz.devonian.config.json.JsonDataObject
import com.github.synnerz.devonian.features.debug.packetlogger.ISerializer
import net.minecraft.network.protocol.PacketFlow
import net.minecraft.network.protocol.PacketType
import net.minecraft.network.protocol.game.ClientboundAnimatePacket
import net.minecraft.network.protocol.game.GamePacketTypes

object AnimatePacket : ISerializer<ClientboundAnimatePacket> {
    override val type: PacketType<ClientboundAnimatePacket> = GamePacketTypes.CLIENTBOUND_ANIMATE
    override val flow: PacketFlow = PacketFlow.CLIENTBOUND

    override fun serialize(
        packet: ClientboundAnimatePacket,
        obj: JsonDataObject
    ) {
        obj.set("entityId", packet.id)
        obj.set("action", packet.action)
    }
}