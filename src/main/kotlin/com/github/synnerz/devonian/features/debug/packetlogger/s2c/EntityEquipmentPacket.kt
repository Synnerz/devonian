package com.github.synnerz.devonian.features.debug.packetlogger.s2c

import com.github.synnerz.devonian.config.json.JsonDataObject
import com.github.synnerz.devonian.features.debug.packetlogger.ISerializer
import com.github.synnerz.devonian.utils.Serializer
import com.google.gson.JsonArray
import net.minecraft.network.protocol.PacketFlow
import net.minecraft.network.protocol.PacketType
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket
import net.minecraft.network.protocol.game.GamePacketTypes

object EntityEquipmentPacket : ISerializer<ClientboundSetEquipmentPacket> {
    override val type: PacketType<ClientboundSetEquipmentPacket> = GamePacketTypes.CLIENTBOUND_SET_EQUIPMENT
    override val flow: PacketFlow = PacketFlow.CLIENTBOUND

    override fun serialize(
        packet: ClientboundSetEquipmentPacket,
        obj: JsonDataObject
    ) {
        obj.set("entityId", packet.entity)
        val arr = JsonArray()
        packet.slots.forEach {
            val slot = it.first
            val item = it.second
            val obj = JsonDataObject()
            obj.set("slot", slot.name)
            obj.set("item", Serializer.serializeItem(item))
            arr.add(obj.json)
        }
        obj.set("equipment", arr)
    }
}