package com.github.synnerz.devonian.features.debug.packetlogger.s2c

import com.github.synnerz.devonian.config.json.JsonDataObject
import com.github.synnerz.devonian.features.debug.packetlogger.ISerializer
import net.minecraft.network.protocol.PacketFlow
import net.minecraft.network.protocol.PacketType
import net.minecraft.network.protocol.game.ClientboundBundlePacket
import net.minecraft.network.protocol.game.GamePacketTypes

object BundlePacket : ISerializer<ClientboundBundlePacket> {
    override val type: PacketType<ClientboundBundlePacket> = GamePacketTypes.CLIENTBOUND_BUNDLE
    override val flow: PacketFlow = PacketFlow.CLIENTBOUND

    override fun serialize(
        packet: ClientboundBundlePacket,
        obj: JsonDataObject
    ) {
        val packets = packet.subPackets().toList()
        obj.set("count", packets.size)
        obj.set("types", '[' + packets.joinToString(", ") {
            it.type().flow.id()[0] + it.type().id.path
        } + ']')
    }
}