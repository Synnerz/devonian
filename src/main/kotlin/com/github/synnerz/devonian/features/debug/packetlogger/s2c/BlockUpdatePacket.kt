package com.github.synnerz.devonian.features.debug.packetlogger.s2c

import com.github.synnerz.devonian.config.json.JsonDataObject
import com.github.synnerz.devonian.features.debug.packetlogger.ISerializer
import com.github.synnerz.devonian.utils.Serializer
import net.minecraft.network.protocol.PacketFlow
import net.minecraft.network.protocol.PacketType
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket
import net.minecraft.network.protocol.game.GamePacketTypes

object BlockUpdatePacket : ISerializer<ClientboundBlockUpdatePacket> {
    override val type: PacketType<ClientboundBlockUpdatePacket> = GamePacketTypes.CLIENTBOUND_BLOCK_UPDATE
    override val flow: PacketFlow = PacketFlow.CLIENTBOUND

    override fun serialize(
        packet: ClientboundBlockUpdatePacket,
        obj: JsonDataObject
    ) {
        obj.set("pos", Serializer.serializeBlockPos(packet.pos))
        obj.set("state", Serializer.serializeBlockState(packet.blockState))
    }
}