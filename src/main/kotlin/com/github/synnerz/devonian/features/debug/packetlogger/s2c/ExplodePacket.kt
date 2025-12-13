package com.github.synnerz.devonian.features.debug.packetlogger.s2c

import com.github.synnerz.devonian.config.json.JsonDataObject
import com.github.synnerz.devonian.features.debug.packetlogger.ISerializer
import com.github.synnerz.devonian.utils.Serializer
import com.google.gson.JsonArray
import net.minecraft.network.protocol.PacketFlow
import net.minecraft.network.protocol.PacketType
import net.minecraft.network.protocol.game.ClientboundExplodePacket
import net.minecraft.network.protocol.game.GamePacketTypes

object ExplodePacket : ISerializer<ClientboundExplodePacket> {
    override val type: PacketType<ClientboundExplodePacket> = GamePacketTypes.CLIENTBOUND_EXPLODE
    override val flow: PacketFlow = PacketFlow.CLIENTBOUND

    override fun serialize(
        packet: ClientboundExplodePacket,
        obj: JsonDataObject
    ) {
        obj.set("center", Serializer.serializeVec(packet.center))
        obj.set("radius", packet.radius)
        obj.set("blockCount", packet.blockCount)
        if (packet.playerKnockback.isPresent) obj.set("playerKnockback", Serializer.serializeVec(packet.playerKnockback.get()))
        obj.set("explosionParticles", packet.explosionParticle)
        obj.set("sound", packet.explosionSound.value().location.toString())
        val arr = JsonArray()
        packet.blockParticles.unwrap().forEach {
            val ent = JsonDataObject()
            ent.set("value", it.value)
            ent.set("weight", it.weight)
            arr.add(ent.json)
        }
        obj.set("blockParticles", arr)
    }
}