package com.github.synnerz.devonian.events

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.particle.Particle
import net.minecraft.client.render.VertexConsumerProvider
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.client.world.ClientWorld
import net.minecraft.entity.Entity
import net.minecraft.item.ItemStack
import net.minecraft.network.packet.Packet
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

open class Event

class PacketSentEvent(
    val packet: Packet<*>,
    val ci: CallbackInfo
) : Event()

class PacketReceivedEvent(
    val packet: Packet<*>,
    val ci: CallbackInfo
) : Event()

class EntityJoinEvent(
    val entity: Entity
) : Event()

class EntityLeaveEvent(
    val entity: Entity
) : Event()

class DropHandItemEvent(
    val itemStack: ItemStack,
    val entireStack: Boolean,
    val cr: CallbackInfoReturnable<Boolean>
) : Event()

class TickEvent(
    val minecraft: MinecraftClient
) : Event()

class RenderWorldEvent(
    val ctx: WorldRenderContext
) : Event()

class RenderEntityEvent(
    val entity: Entity,
    val matrixStack: MatrixStack,
    val consumer: VertexConsumerProvider,
    val light: Int,
    val ci: CallbackInfo
) : Event()

class GuiOpenEvent(
    val screen: Screen,
    val ci: CallbackInfo
) : Event()

class GuiCloseEvent(
    val ci: CallbackInfo
) : Event()

class ParticleSpawnEvent(
    val particle: Particle,
    val ci: CallbackInfo
) : Event()

class GameLoadEvent(
    val minecraft: MinecraftClient
) : Event()

class GameUnloadEvent(
    val minecraft: MinecraftClient
) : Event()

class WorldChangeEvent(
    val minecraft: MinecraftClient,
    val world: ClientWorld
) : Event()

// TODO: make chat events