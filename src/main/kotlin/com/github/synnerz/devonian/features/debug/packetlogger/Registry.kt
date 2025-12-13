package com.github.synnerz.devonian.features.debug.packetlogger

import com.github.synnerz.devonian.features.debug.packetlogger.s2c.*
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.PacketFlow

object Registry {
    data class Key(val path: String, val flow: PacketFlow)

    val serializers = mutableMapOf<Key, ISerializer<*>>()

    fun register(serializer: ISerializer<*>) {
        serializers[Key(serializer.type.id.path, serializer.flow)] = serializer
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Packet<*>> get(packet: T): ISerializer<T> = serializers.getOrDefault(
        Key(packet.type().id.path, packet.type().flow),
        ISerializer.EMPTY
    ) as ISerializer<T>

    init {
        register(BlockUpdatePacket)
        register(MultiBlockUpdatePacket)
        register(SoundPacket)
        register(SoundEntityPacket)
        register(LevelEventPacket)
    }
}