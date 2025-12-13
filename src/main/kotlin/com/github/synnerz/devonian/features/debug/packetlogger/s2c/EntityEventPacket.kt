package com.github.synnerz.devonian.features.debug.packetlogger.s2c

import com.github.synnerz.devonian.config.json.JsonDataObject
import com.github.synnerz.devonian.features.debug.packetlogger.ISerializer
import com.github.synnerz.devonian.mixin.accessor.ClientboundEntityEventPacketAccessor
import net.minecraft.network.protocol.PacketFlow
import net.minecraft.network.protocol.PacketType
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket
import net.minecraft.network.protocol.game.GamePacketTypes

object EntityEventPacket : ISerializer<ClientboundEntityEventPacket> {
    override val type: PacketType<ClientboundEntityEventPacket> = GamePacketTypes.CLIENTBOUND_ENTITY_EVENT
    override val flow: PacketFlow = PacketFlow.CLIENTBOUND

    override fun serialize(
        packet: ClientboundEntityEventPacket,
        obj: JsonDataObject
    ) {
        obj.set("entityId", (packet as ClientboundEntityEventPacketAccessor).entityId)
        obj.set("eventId", packet.eventId)
    }
}