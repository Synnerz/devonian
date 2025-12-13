package com.github.synnerz.devonian.features.debug.packetlogger.s2c

import com.github.synnerz.devonian.config.json.JsonDataObject
import com.github.synnerz.devonian.features.debug.packetlogger.ISerializer
import net.minecraft.network.protocol.PacketFlow
import net.minecraft.network.protocol.PacketType
import net.minecraft.network.protocol.game.ClientboundSoundEntityPacket
import net.minecraft.network.protocol.game.GamePacketTypes

object SoundEntityPacket : ISerializer<ClientboundSoundEntityPacket> {
    override val type: PacketType<ClientboundSoundEntityPacket> = GamePacketTypes.CLIENTBOUND_SOUND_ENTITY
    override val flow: PacketFlow = PacketFlow.CLIENTBOUND

    override fun serialize(
        packet: ClientboundSoundEntityPacket,
        obj: JsonDataObject
    ) {
        obj.set("sound", packet.sound.value().location.toString())
        obj.set("id", packet.id)
        obj.set("volume", packet.volume)
        obj.set("pitch", packet.pitch)
        obj.set("source", packet.source.name)
        obj.set("seed", packet.seed)
    }
}