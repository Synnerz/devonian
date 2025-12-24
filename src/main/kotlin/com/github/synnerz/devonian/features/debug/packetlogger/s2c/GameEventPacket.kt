package com.github.synnerz.devonian.features.debug.packetlogger.s2c

import com.github.synnerz.devonian.config.json.JsonDataObject
import com.github.synnerz.devonian.features.debug.packetlogger.ISerializer
import com.github.synnerz.devonian.mixin.accessor.`ClientboundGameEventPacket$TypeAccessor`
import net.minecraft.network.protocol.PacketFlow
import net.minecraft.network.protocol.PacketType
import net.minecraft.network.protocol.game.ClientboundGameEventPacket
import net.minecraft.network.protocol.game.GamePacketTypes

object GameEventPacket : ISerializer<ClientboundGameEventPacket> {
    override val type: PacketType<ClientboundGameEventPacket> = GamePacketTypes.CLIENTBOUND_GAME_EVENT
    override val flow: PacketFlow = PacketFlow.CLIENTBOUND

    override fun serialize(
        packet: ClientboundGameEventPacket,
        obj: JsonDataObject
    ) {
        obj.set("event", (packet.event as `ClientboundGameEventPacket$TypeAccessor`).id)
        obj.set("param", packet.param)
    }
}