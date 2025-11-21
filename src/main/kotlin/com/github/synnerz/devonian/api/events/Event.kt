package com.github.synnerz.devonian.api.events

import com.mojang.blaze3d.vertex.PoseStack
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext
import net.minecraft.client.DeltaTracker
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.particle.Particle
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.Packet
import net.minecraft.sounds.SoundSource
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.ClickType
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.HitResult
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
    val minecraft: Minecraft
) : Event()

class RenderWorldEvent(
    val ctx: WorldRenderContext
) : Event()

class RenderEntityEvent(
    val entity: Entity,
    val matrixStack: PoseStack,
    val consumer: MultiBufferSource,
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
    val minecraft: Minecraft
) : Event()

class GameUnloadEvent(
    val minecraft: Minecraft
) : Event()

class WorldChangeEvent(
    val minecraft: Minecraft,
    val world: ClientLevel
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
    val actionType: ClickType,
    val handler: AbstractContainerMenu
) : CancellableEvent()

class GuiKeyEvent(
    val keyName: String?,
    val key: Int,
    val scanCode: Int,
    val screen: Screen
) : CancellableEvent()

class BeforeBlockOutlineEvent(
    val renderContext: WorldRenderContext,
    val hitResult: HitResult?
) : CancellableEvent()

open class CriteriaEvent(val message: String) : CancellableEvent() {
    fun matches(criteria: Regex): List<String>? {
        val matches = criteria.matchEntire(message) ?: return null
        return matches.groupValues.drop(1)
    }
}

open class ChatEvent(message: String, val text: Component) : CriteriaEvent(message)

abstract class ChatChannelEvent(message: String, text: Component, val name: String, val userMessage: String) :
    ChatEvent(message, text) {
    class AllChatEvent(message: String, text: Component, name: String, userMessage: String, val level: Int) :
        ChatChannelEvent(message, text, name, userMessage)

    class PartyChatEvent(message: String, text: Component, name: String, userMessage: String) :
        ChatChannelEvent(message, text, name, userMessage)

    class CoopChatEvent(message: String, text: Component, name: String, userMessage: String) :
        ChatChannelEvent(message, text, name, userMessage)

    class GuildChatEvent(message: String, text: Component, name: String, userMessage: String) :
        ChatChannelEvent(message, text, name, userMessage)

    abstract class PrivateChatEvent(message: String, text: Component, name: String, userMessage: String) :
        ChatChannelEvent(message, text, name, userMessage) {
        class IncomingPrivateChatEvent(message: String, text: Component, name: String, userMessage: String) :
            PrivateChatEvent(message, text, name, userMessage)

        class OutgoingPrivateChatEvent(message: String, text: Component, name: String, userMessage: String) :
            PrivateChatEvent(message, text, name, userMessage)
    }

    companion object {
        private val allChatRegex =
            "^(?:\\[(?<level>\\d+)] .? ?)?(?:\\[[^]]+] )?(?<name>\\w{1,16}): (?<msg>.+)\$".toRegex()
        private val partyChatRegex = "^Party > (?:\\[[^]]+] )?(?<name>\\w{1,16}): (?<msg>.+)\$".toRegex()
        private val coopChatRegex = "^Co-op > (?:\\[[^]]+] )?(?<name>\\w{1,16}): (?<msg>.+)\$".toRegex()
        private val guildChatRegex = "^Guild > (?:\\[[^]]+] )?(?<name>\\w{1,16}): (?<msg>.+)\$".toRegex()
        private val incomingPMRegex = "^From (?:\\[[^]]+] )?(?<name>\\w{1,16}): (?<msg>.+)\$".toRegex()
        private val outgoingPMRegex = "^To (?:\\[[^]]+] )?(?<name>\\w{1,16}): (?<msg>.+)\$".toRegex()

        fun from(message: String, text: Component): ChatChannelEvent? {
            allChatRegex.matchEntire(message)?.let {
                return AllChatEvent(
                    message, text,
                    it.groups["name"]?.value ?: "",
                    it.groups["msg"]?.value ?: "",
                    (it.groups["level"]?.value?.toInt()) ?: 0,
                )
            }

            partyChatRegex.matchEntire(message)?.let {
                return PartyChatEvent(
                    message, text,
                    it.groups["name"]?.value ?: "",
                    it.groups["msg"]?.value ?: "",
                )
            }

            coopChatRegex.matchEntire(message)?.let {
                return CoopChatEvent(
                    message, text,
                    it.groups["name"]?.value ?: "",
                    it.groups["msg"]?.value ?: "",
                )
            }

            guildChatRegex.matchEntire(message)?.let {
                return GuildChatEvent(
                    message, text,
                    it.groups["name"]?.value ?: "",
                    it.groups["msg"]?.value ?: "",
                )
            }

            incomingPMRegex.matchEntire(message)?.let {
                return PrivateChatEvent.IncomingPrivateChatEvent(
                    message, text,
                    it.groups["name"]?.value ?: "",
                    it.groups["msg"]?.value ?: "",
                )
            }

            outgoingPMRegex.matchEntire(message)?.let {
                return PrivateChatEvent.OutgoingPrivateChatEvent(
                    message, text,
                    it.groups["name"]?.value ?: "",
                    it.groups["msg"]?.value ?: "",
                )
            }

            return null
        }
    }
}

class EntityDeathEvent(
    val entity: Entity,
    val world: ClientLevel
) : Event()

class RenderOverlayEvent(
    val ctx: GuiGraphics,
    val tickCounter: DeltaTracker
) : Event()

class RenderTickEvent : Event()

class TabAddEvent(message: String) : CriteriaEvent(message)
class TabUpdateEvent(message: String) : CriteriaEvent(message)

class ServerTickEvent(val ticks: Int) : Event()

class ScoreboardEvent(message: String) : CriteriaEvent(message)

class RenderSlotEvent(val slot: Slot, val ctx: GuiGraphics) : CancellableEvent()

class SoundPlayEvent(
    val sound: String,
    val pitch: Float,
    val volume: Float,
    val category: SoundSource,
    val x: Double,
    val y: Double,
    val z: Double,
    val seed: Long
) : CancellableEvent()

class PostClientInit(val minecraft: Minecraft) : Event()

class PacketNameChangeEvent(
    val entityId: Int,
    val type: EntityType<*>,
    val nameText: Component,
    val name: String
) : Event()