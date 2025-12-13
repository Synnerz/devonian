package com.github.synnerz.devonian.features.debug.packetlogger.s2c

import com.github.synnerz.devonian.config.json.JsonDataObject
import com.github.synnerz.devonian.features.debug.packetlogger.ISerializer
import com.github.synnerz.devonian.utils.Serializer
import com.google.gson.JsonArray
import net.minecraft.network.protocol.PacketFlow
import net.minecraft.network.protocol.PacketType
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket
import net.minecraft.network.protocol.game.GamePacketTypes

object MultiBlockUpdatePacket : ISerializer<ClientboundSectionBlocksUpdatePacket> {
    override val type: PacketType<ClientboundSectionBlocksUpdatePacket> = GamePacketTypes.CLIENTBOUND_SECTION_BLOCKS_UPDATE
    override val flow: PacketFlow = PacketFlow.CLIENTBOUND

    override fun serialize(
        packet: ClientboundSectionBlocksUpdatePacket,
        obj: JsonDataObject
    ) {
        val arr = JsonArray()
        packet.runUpdates { pos, state ->
            val v = JsonDataObject()
            v.set("pos", Serializer.serializeBlockPos(pos))
            v.set("state", Serializer.serializeBlockState(state))
            arr.add(v.json)
        }
        obj.set("updates", arr)
    }
}