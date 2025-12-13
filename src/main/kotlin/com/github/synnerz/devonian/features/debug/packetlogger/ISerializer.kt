package com.github.synnerz.devonian.features.debug.packetlogger

import com.github.synnerz.devonian.config.json.JsonDataObject
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.PacketFlow
import net.minecraft.network.protocol.PacketType

interface ISerializer<T : Packet<*>> {
    val type: PacketType<T>
    val flow: PacketFlow

    fun serialize(packet: T, obj: JsonDataObject)

    companion object {
        val EMPTY = object : ISerializer<Packet<*>> {
            override val type: PacketType<Packet<*>>
                get() = throw UnsupportedOperationException()
            override val flow: PacketFlow
                get() = throw UnsupportedOperationException()

            override fun serialize(packet: Packet<*>, obj: JsonDataObject) {}
        }
    }
}