package com.github.synnerz.devonian.features.debug.packetlogger.s2c

import com.github.synnerz.devonian.config.json.JsonDataObject
import com.github.synnerz.devonian.features.debug.packetlogger.ISerializer
import com.github.synnerz.devonian.utils.Serializer
import net.minecraft.network.protocol.PacketFlow
import net.minecraft.network.protocol.PacketType
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket
import net.minecraft.network.protocol.game.GamePacketTypes
import net.minecraft.world.entity.EntityType

object SpawnEntityPacket : ISerializer<ClientboundAddEntityPacket> {
    override val type: PacketType<ClientboundAddEntityPacket> = GamePacketTypes.CLIENTBOUND_ADD_ENTITY
    override val flow: PacketFlow = PacketFlow.CLIENTBOUND

    override fun serialize(
        packet: ClientboundAddEntityPacket,
        obj: JsonDataObject
    ) {
        obj.set("type", EntityType.getKey(packet.type).toString())
        obj.set("id", packet.id)
        obj.set("uuid", packet.uuid.toString())
        obj.set("data", packet.data)
        obj.set("x", packet.x)
        obj.set("y", packet.y)
        obj.set("z", packet.z)
        obj.set("xRot", packet.xRot)
        obj.set("yRot", packet.yRot)
        obj.set("yRotHead", packet.yHeadRot)
        obj.set("velo", Serializer.serializeVec(packet.movement))
    }
}