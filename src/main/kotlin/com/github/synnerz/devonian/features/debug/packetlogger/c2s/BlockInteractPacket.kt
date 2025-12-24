package com.github.synnerz.devonian.features.debug.packetlogger.c2s

import com.github.synnerz.devonian.config.json.JsonDataObject
import com.github.synnerz.devonian.features.debug.packetlogger.ISerializer
import com.github.synnerz.devonian.utils.Serializer
import net.minecraft.network.protocol.PacketFlow
import net.minecraft.network.protocol.PacketType
import net.minecraft.network.protocol.game.GamePacketTypes
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket

object BlockInteractPacket : ISerializer<ServerboundUseItemOnPacket> {
    override val type: PacketType<ServerboundUseItemOnPacket> = GamePacketTypes.SERVERBOUND_USE_ITEM_ON
    override val flow: PacketFlow = PacketFlow.SERVERBOUND

    override fun serialize(
        packet: ServerboundUseItemOnPacket,
        obj: JsonDataObject
    ) {
        obj.set("hand", packet.hand.asEquipmentSlot().name)
        obj.set("pos", Serializer.serializeBlockPos(packet.hitResult.blockPos))
        obj.set("sequence", packet.sequence)
    }
}