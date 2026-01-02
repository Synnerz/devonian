package com.github.synnerz.devonian.features.debug.packetlogger.s2c

import com.github.synnerz.devonian.config.json.JsonDataObject
import com.github.synnerz.devonian.features.debug.packetlogger.ISerializer
import net.minecraft.network.protocol.PacketFlow
import net.minecraft.network.protocol.PacketType
import net.minecraft.network.protocol.game.ClientboundTakeItemEntityPacket
import net.minecraft.network.protocol.game.GamePacketTypes

object TakeItemPacket : ISerializer<ClientboundTakeItemEntityPacket> {
    override val type: PacketType<ClientboundTakeItemEntityPacket> = GamePacketTypes.CLIENTBOUND_TAKE_ITEM_ENTITY
    override val flow: PacketFlow = PacketFlow.CLIENTBOUND

    override fun serialize(
        packet: ClientboundTakeItemEntityPacket,
        obj: JsonDataObject
    ) {
        obj.set("playerId", packet.playerId)
        obj.set("itemId", packet.itemId)
        obj.set("amount", packet.amount)
    }
}