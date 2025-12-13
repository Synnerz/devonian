package com.github.synnerz.devonian.features.debug.packetlogger.s2c

import com.github.synnerz.devonian.config.json.JsonDataObject
import com.github.synnerz.devonian.features.debug.packetlogger.ISerializer
import net.minecraft.network.protocol.PacketFlow
import net.minecraft.network.protocol.PacketType
import net.minecraft.network.protocol.game.ClientboundSoundPacket
import net.minecraft.network.protocol.game.GamePacketTypes

object SoundPacket : ISerializer<ClientboundSoundPacket> {
    override val type: PacketType<ClientboundSoundPacket> = GamePacketTypes.CLIENTBOUND_SOUND
    override val flow: PacketFlow = PacketFlow.CLIENTBOUND

    override fun serialize(
        packet: ClientboundSoundPacket,
        obj: JsonDataObject
    ) {
        obj.set("sound", packet.sound.value().location.toString())
        obj.set("volume", packet.volume)
        obj.set("pitch", packet.pitch)
        obj.set("source", packet.source.name)
        obj.set("seed", packet.seed)
        obj.set("x", packet.x)
        obj.set("y", packet.y)
        obj.set("z", packet.z)
    }
}