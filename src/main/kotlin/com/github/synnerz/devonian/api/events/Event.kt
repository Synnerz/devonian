package com.github.synnerz.devonian.api.events

import com.mojang.blaze3d.vertex.PoseStack
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldExtractionContext
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext
import net.minecraft.client.DeltaTracker
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.particle.Particle
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.entity.state.EntityRenderState
import net.minecraft.client.renderer.state.CameraRenderState
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket
import net.minecraft.sounds.SoundSource
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.ClickType
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.HitResult
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Target(AnnotationTarget.CLASS)
annotation class Threaded

abstract class Event {
    open fun post(): Boolean {
        EventBus.post(this)
        return false
    }
}

abstract class CancellableEvent : Event() {
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

@Threaded class PacketSentEvent(
    val packet: Packet<*>
) : CancellableEvent()

@Threaded class PacketReceivedEvent(
    val packet: Packet<*>
) : CancellableEvent()

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
    val entityState: EntityRenderState,
    val cameraState: CameraRenderState,
    val matrix: PoseStack,
    val submitter: SubmitNodeCollector,
    val ci: CallbackInfo
) : Event()

class PreRenderEntityEvent(
    val entity: Entity
) : CancellableEvent()

class GuiOpenEvent(
    val screen: Screen,
    val ci: CallbackInfo
) : Event()

class GuiCloseEvent(
    val ci: CallbackInfo
) : Event()

class ParticleSpawnEvent(
    val particle: Particle
) : CancellableEvent()

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

@Threaded class AreaEvent(
    val area: String?
) : Event()

@Threaded class SubAreaEvent(
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
    val screen: Screen,
    val event: KeyEvent
) : CancellableEvent()

class BeforeBlockOutlineEvent(
    val renderContext: WorldExtractionContext,
    val hitResult: HitResult?
) : CancellableEvent()

open class CriteriaEvent(val message: String) : CancellableEvent() {
    fun matches(criteria: Regex): List<String>? {
        val matches = criteria.matchEntire(message) ?: return null
        return matches.groupValues.drop(1)
    }
}

@Threaded open class ChatEvent(message: String, val text: Component) : CriteriaEvent(message)

abstract class ChatChannelEvent(message: String, text: Component, val name: String, val userMessage: String) :
    ChatEvent(message, text) {
    @Threaded class AllChatEvent(message: String, text: Component, name: String, userMessage: String, val level: Int) :
        ChatChannelEvent(message, text, name, userMessage)

    @Threaded class PartyChatEvent(message: String, text: Component, name: String, userMessage: String) :
        ChatChannelEvent(message, text, name, userMessage)

    @Threaded class CoopChatEvent(message: String, text: Component, name: String, userMessage: String) :
        ChatChannelEvent(message, text, name, userMessage)

    @Threaded class GuildChatEvent(message: String, text: Component, name: String, userMessage: String) :
        ChatChannelEvent(message, text, name, userMessage)

    abstract class PrivateChatEvent(message: String, text: Component, name: String, userMessage: String) :
        ChatChannelEvent(message, text, name, userMessage) {
        @Threaded class IncomingPrivateChatEvent(message: String, text: Component, name: String, userMessage: String) :
            PrivateChatEvent(message, text, name, userMessage)

        @Threaded class OutgoingPrivateChatEvent(message: String, text: Component, name: String, userMessage: String) :
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

@Threaded class TabAddEvent(message: String) : CriteriaEvent(message)
@Threaded class TabUpdateEvent(message: String) : CriteriaEvent(message)

@Threaded class ServerTickEvent(val ticks: Int) : Event()

@Threaded class ScoreboardEvent(message: String) : CriteriaEvent(message)

class RenderSlotEvent(val slot: Slot, val ctx: GuiGraphics) : CancellableEvent()

@Threaded class SoundPlayEvent(
    val sound: String,
    val pitch: Float,
    val volume: Float,
    val category: SoundSource,
    val x: Double,
    val y: Double,
    val z: Double,
    val seed: Long
) : CancellableEvent()

// while no, yes
@Threaded class PostClientInit(val minecraft: Minecraft) : Event()

@Threaded class NameChangeEvent(
    val entityId: Int,
    val type: EntityType<*>,
    val nameText: Component,
    val name: String
) : Event()

@Threaded class ActionbarEvent(
    val message: String,
    val text: Component
) : Event()

@Threaded class EntityEquipmentEvent(
    val entityId: Int,
    val type: EntityType<*>,
    val slots: List<Pair<EquipmentSlot, ItemStack?>>
) : Event()

class EntityInteractEvent(
    val entity: Entity
) : CancellableEvent()

class BlockUpdateEvent(
    val blockPos: BlockPos,
    val blockState: BlockState
) : Event()

class MultiBlockUpdateEvent(
    val packet: ClientboundSectionBlocksUpdatePacket
) : Event() {
    fun forEach(cb: (BlockPos, BlockState) -> Unit) {
        packet.runUpdates(cb)
    }
}

class BlockPlaceEvent(
    val blockHitResult: BlockHitResult,
    val hand: InteractionHand
) : Event()
