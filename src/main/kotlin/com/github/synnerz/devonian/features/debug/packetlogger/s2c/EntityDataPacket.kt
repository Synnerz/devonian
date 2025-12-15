package com.github.synnerz.devonian.features.debug.packetlogger.s2c

import com.github.synnerz.devonian.config.json.JsonDataObject
import com.github.synnerz.devonian.features.debug.packetlogger.ISerializer
import com.github.synnerz.devonian.utils.Serializer
import com.google.gson.JsonArray
import net.minecraft.network.protocol.PacketFlow
import net.minecraft.network.protocol.PacketType
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket
import net.minecraft.network.protocol.game.GamePacketTypes

object EntityDataPacket : ISerializer<ClientboundSetEntityDataPacket> {
    override val type: PacketType<ClientboundSetEntityDataPacket> = GamePacketTypes.CLIENTBOUND_SET_ENTITY_DATA
    override val flow: PacketFlow = PacketFlow.CLIENTBOUND

    override fun serialize(
        packet: ClientboundSetEntityDataPacket,
        obj: JsonDataObject
    ) {
        obj.set("entityId", packet.id)
        val arr = JsonArray(packet.packedItems.size)
        packet.packedItems.forEach {
            val obj = JsonDataObject()
            obj.set("id", it.id)
            obj.set("serializer", it.serializer.javaClass.name)
            obj.set("value", Serializer.serialize(it.value))
            arr.add(obj.json)
        }
        obj.set("items", arr)
    }
}