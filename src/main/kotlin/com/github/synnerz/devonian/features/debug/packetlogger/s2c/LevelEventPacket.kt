package com.github.synnerz.devonian.features.debug.packetlogger.s2c

import com.github.synnerz.devonian.config.json.JsonDataObject
import com.github.synnerz.devonian.features.debug.packetlogger.ISerializer
import com.github.synnerz.devonian.utils.Serializer
import net.minecraft.network.protocol.PacketFlow
import net.minecraft.network.protocol.PacketType
import net.minecraft.network.protocol.game.ClientboundLevelEventPacket
import net.minecraft.network.protocol.game.GamePacketTypes

object LevelEventPacket : ISerializer<ClientboundLevelEventPacket> {
    override val type: PacketType<ClientboundLevelEventPacket> = GamePacketTypes.CLIENTBOUND_LEVEL_EVENT
    override val flow: PacketFlow = PacketFlow.CLIENTBOUND

    override fun serialize(
        packet: ClientboundLevelEventPacket,
        obj: JsonDataObject
    ) {
        obj.set("isGlobal", packet.isGlobalEvent)
        obj.set("type", packet.type)
        obj.set("data", packet.data)
        obj.set("pos", Serializer.serializeBlockPos(packet.pos))
    }
}