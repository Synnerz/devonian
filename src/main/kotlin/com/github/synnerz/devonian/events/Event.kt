package com.github.synnerz.devonian.events

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext.BlockOutlineContext
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.particle.Particle
import net.minecraft.client.render.RenderTickCounter
import net.minecraft.client.render.VertexConsumerProvider
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.client.world.ClientWorld
import net.minecraft.entity.Entity
import net.minecraft.item.ItemStack
import net.minecraft.network.packet.Packet
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.slot.Slot
import net.minecraft.screen.slot.SlotActionType
import net.minecraft.text.Text
import net.minecraft.util.math.BlockPos
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

open class Event {
    open fun post(): Boolean {
        EventBus.post(this)
        return false
    }
}

open class CancellableEvent : Event() {
    private var shouldCancel = false

    fun cancel() {
        shouldCancel = true
    }

    fun isCancelled() = shouldCancel

    override fun post(): Boolean {
        EventBus.post(this)
        return isCancelled()
    }
}

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

class DropItemEvent(
    val itemStack: ItemStack,
    val entireStack: Boolean
) : CancellableEvent()

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

class AreaEvent(
    val area: String?
) : Event()

class SubAreaEvent(
    val subarea: String?
) : Event()

class BlockInteractEvent(
    val itemStack: ItemStack,
    val pos: BlockPos
) : CancellableEvent()

class GuiClickEvent(
    val mx: Double,
    val my: Double,
    val mbtn: Int,
    val state: Boolean,
    val screen: Screen
) : CancellableEvent()

class GuiSlotClickEvent(
    val slot: Slot?,
    val slotId: Int,
    val mbtn: Int,
    val actionType: SlotActionType,
    val handler: ScreenHandler
) : CancellableEvent()

class GuiKeyEvent(
    val keyName: String?,
    val key: Int,
    val scanCode: Int,
    val screen: Screen
) : CancellableEvent()

class BlockOutlineEvent(
    val renderContext: WorldRenderContext,
    val blockContext: BlockOutlineContext
) : CancellableEvent()

open class CriteriaEvent(val message: String): CancellableEvent() {
    fun matches(criteria: Regex): List<String>? {
        val matches = criteria.matchEntire(message) ?: return null
        return matches.groupValues.drop(1)
    }
}

class ChatEvent(message: String, val text: Text) : CriteriaEvent(message)

class EntityDeathEvent(
    val entity: Entity,
    val world: ClientWorld
) : Event()

class RenderOverlayEvent(
    val ctx: DrawContext,
    val tickCounter: RenderTickCounter
) : Event()

class RenderTickEvent : Event()

class TabAddEvent(message: String) : CriteriaEvent(message)
class TabUpdateEvent(message: String) : CriteriaEvent(message)

class ServerTickEvent(val ticks: Int) : Event()

class ScoreboardEvent(message: String) : CriteriaEvent(message)
