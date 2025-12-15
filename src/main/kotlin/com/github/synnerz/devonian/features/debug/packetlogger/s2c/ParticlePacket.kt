package com.github.synnerz.devonian.features.debug.packetlogger.s2c

import com.github.synnerz.devonian.config.json.JsonDataObject
import com.github.synnerz.devonian.features.debug.packetlogger.ISerializer
import com.github.synnerz.devonian.utils.Serializer
import net.minecraft.core.particles.BlockParticleOption
import net.minecraft.core.particles.ColorParticleOption
import net.minecraft.core.particles.DustColorTransitionOptions
import net.minecraft.core.particles.DustParticleOptions
import net.minecraft.core.particles.ItemParticleOption
import net.minecraft.core.particles.PowerParticleOption
import net.minecraft.core.particles.ScalableParticleOptionsBase
import net.minecraft.core.particles.SculkChargeParticleOptions
import net.minecraft.core.particles.SpellParticleOption
import net.minecraft.core.particles.TrailParticleOption
import net.minecraft.core.particles.VibrationParticleOption
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.protocol.PacketFlow
import net.minecraft.network.protocol.PacketType
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket
import net.minecraft.network.protocol.game.GamePacketTypes

object ParticlePacket : ISerializer<ClientboundLevelParticlesPacket> {
    override val type: PacketType<ClientboundLevelParticlesPacket> = GamePacketTypes.CLIENTBOUND_LEVEL_PARTICLES
    override val flow: PacketFlow = PacketFlow.CLIENTBOUND

    override fun serialize(
        packet: ClientboundLevelParticlesPacket,
        obj: JsonDataObject
    ) {
        val particle = packet.particle
        val type = JsonDataObject()
        type.set("name", BuiltInRegistries.PARTICLE_TYPE.getKey(particle.type).toString())
        type.set("class", particle::class.java.name)
        when (particle) {
            is BlockParticleOption -> type.set("block", Serializer.serializeBlockState(particle.state))
            is ColorParticleOption -> {
                val color = JsonDataObject()
                color.set("r", particle.red)
                color.set("g", particle.green)
                color.set("b", particle.blue)
                color.set("a", particle.alpha)
                type.set("color", color)
            }
            is DustColorTransitionOptions -> {
                type.set("scale", particle.scale)
                type.set("fromColor", Serializer.serializeVector3f(particle.fromColor))
                type.set("toColor", Serializer.serializeVector3f(particle.toColor))
            }
            is DustParticleOptions -> {
                type.set("scale", particle.scale)
                type.set("color", Serializer.serializeVector3f(particle.color))
            }
            is ItemParticleOption -> type.set("item", Serializer.serializeItem(particle.item))
            is PowerParticleOption -> type.set("power", particle.power)
            is ScalableParticleOptionsBase -> type.set("scale", particle.scale)
            is SculkChargeParticleOptions -> type.set("roll", particle.roll)
            is SpellParticleOption -> {
                val color = JsonDataObject()
                color.set("r", particle.red)
                color.set("g", particle.green)
                color.set("b", particle.blue)
                type.set("color", color)
                type.set("power", particle.power)
            }
            is TrailParticleOption -> {
                type.set("target", Serializer.serializeVec(particle.target))
                type.set("color", "0x" + particle.color.toString(16).uppercase().padStart(8, '0'))
                type.set("duration", particle.duration)
            }
            is VibrationParticleOption -> {
                // type.set("destination", "fuck you")
                type.set("arriveTime", particle.arrivalInTicks)
            }
        }
        obj.set("type", type)
        obj.set("count", packet.count)
        obj.set("x", packet.x)
        obj.set("y", packet.y)
        obj.set("z", packet.z)
        obj.set("xDist", packet.xDist)
        obj.set("yDist", packet.yDist)
        obj.set("zDist", packet.zDist)
        obj.set("maxSpeed", packet.maxSpeed)
        obj.set("bypassLimit", packet.isOverrideLimiter)
        obj.set("alwaysShow", packet.alwaysShow())
    }
}