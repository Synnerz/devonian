package com.github.synnerz.devonian.api.events

import com.github.synnerz.devonian.Devonian
import com.github.synnerz.devonian.api.ScreenUtils
import com.mojang.blaze3d.vertex.PoseStack
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldExtractionContext
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext
import net.minecraft.client.DeltaTracker
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.client.gui.screens.inventory.ContainerScreen
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.particle.Particle
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState
import net.minecraft.client.renderer.entity.state.EntityRenderState
import net.minecraft.client.renderer.state.CameraRenderState
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundSource
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import org.lwjgl.glfw.GLFW

@Target(AnnotationTarget.CLASS)
annotation class Threaded
@Target(AnnotationTarget.CLASS)
annotation class Ordered

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

class PrePacketSentEvent(
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

class DropItemEvent @JvmOverloads constructor(
    val slot: Slot?,
    val entireStack: Boolean,
    val itemStack: ItemStack = slot?.item ?: ItemStack.EMPTY,
    val willDropInDungeons: Boolean = false,
) : CancellableEvent()

class TickEvent(
    val minecraft: Minecraft
) : Event()

class RenderWorldEvent(
    val ctx: WorldRenderContext
) : Event()

class PreRenderEntityEvent(
    val entityState: EntityRenderState,
    val cameraState: CameraRenderState,
    val matrix: PoseStack,
    val submitter: SubmitNodeCollector,
) : Event()

/*
class PostRenderEntityEvent(
    val entityState: EntityRenderState,
    val cameraState: CameraRenderState,
    val matrix: PoseStack,
    val submitter: SubmitNodeCollector,
) : Event()
 */

class ExtractRenderEntityEvent(
    val entity: Entity,
    val pt: Float,
) : CancellableEvent()

class GuiOpenEvent(
    val screen: Screen
) : CancellableEvent()

class GuiCloseEvent(
    val screen: Screen
) : CancellableEvent()

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

class GuiKeyDownEvent(
    val keyName: String?,
    val key: Int,
    val scanCode: Int,
    val screen: Screen,
    val event: KeyEvent
) : CancellableEvent()

class GuiKeyUpEvent(
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

@Threaded class ActionbarEvent(
    message: String,
    val text: Component
) : CriteriaEvent(message)

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

@Ordered class RenderSlotEvent(val slot: Slot, val ctx: GuiGraphics, val screen: AbstractContainerScreen<*>) : CancellableEvent() {
    fun isInventory(): Boolean = slot.container == Devonian.minecraft.player?.inventory
}

@Ordered class RenderHotbarSlotEvent(val item: ItemStack, val x: Int, val y: Int, val ctx: GuiGraphics) : CancellableEvent()

@Threaded class SoundPlayEvent(
    val sound: String,
    val pitch: Float,
    val volume: Float,
    val category: SoundSource,
    val x: Double,
    val y: Double,
    val z: Double,
    val seed: Long,
    val underlyingEvent: SoundEvent,
) : CancellableEvent()

// while no, yes
@Threaded class PostClientInitEvent(val minecraft: Minecraft) : Event()

@Threaded class NameChangeEvent(
    val entityId: Int,
    val type: EntityType<*>,
    val nameText: Component,
    val name: String
) : Event()

@Threaded class EntityEquipmentEvent(
    val entityId: Int,
    val type: EntityType<*>,
    val spawnPos: Vec3,
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

class UseItemOnEvent(
    val blockHitResult: BlockHitResult,
    val hand: InteractionHand
) : Event()

class UseItemEvent(
    val hand: InteractionHand
) : Event()

class ClientThreadServerTickEvent() : Event()

/*
class PreRenderTileEntityEvent(
    val entityState: BlockEntityRenderState,
    val cameraState: CameraRenderState,
    val matrix: PoseStack,
    val submitter: SubmitNodeCollector,
) : Event()
*/

class PostRenderTileEntityEvent(
    val entityState: BlockEntityRenderState,
    val cameraState: CameraRenderState,
    val matrix: PoseStack,
    val submitter: SubmitNodeCollector,
) : Event()

class SwapItemEvent(
    val slot1: Slot,
    val slot2: Slot,
) : CancellableEvent()

class PickupItemInventoryEvent(
    val slot: Slot,
    val screen: AbstractContainerScreen<*>,
    // is right click
    val isSplitItem: Boolean,
) : CancellableEvent()

class QuickMoveItemEvent(
    val slot: Slot,
    val screen: AbstractContainerScreen<*>
) : CancellableEvent()

class QuickCraftMoveEvent(
    val slot: Slot,
    // is right click
    val isSingleItem: Boolean,
    val screen: AbstractContainerScreen<*>
) : CancellableEvent()

@Ordered class PostRenderSlotsEvent(
    val ctx: GuiGraphics,
    val mouseX: Int,
    val mouseY: Int,
    val container: AbstractContainerScreen<*>,
) : Event()

@Threaded class EntityDataEvent(
    val entityId: Int,
    val type: EntityType<*>,
    val data: List<SynchedEntityData.DataValue<*>>,
) : Event()

class KeyPressEvent(
    val key: Int,
    val scancode: Int,
    val underlying: KeyEvent,
) : Event()

class RenderGuiEvent(
    val screen: Screen,
    val x: Int,
    val y: Int,
    val pticks: Float,
    val ctx: GuiGraphics
) : CancellableEvent()

class PostRenderGuiEvent(
    val screen: Screen,
    val x: Int,
    val y: Int,
    val pticks: Float,
    val ctx: GuiGraphics
) : Event()

class ContainerRenderEvent(
    val screen: ContainerScreen,
    val x: Int,
    val y: Int,
    val pticks: Float,
    val ctx: GuiGraphics
) : CancellableEvent()

@Ordered class TooltipRenderEvent(
    val lore: MutableList<ClientTooltipComponent>,
    val x: Int,
    val y: Int,
) : CancellableEvent() {
    val slot: Slot?
    val item: ItemStack?
    val shift = GLFW.glfwGetKey(Devonian.minecraft.window.handle(), GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS
    init {
        val screen = Devonian.minecraft.screen
        slot = screen?.let { ScreenUtils.cursorSlot(it) }
        item = slot?.item
    }
}

@Threaded class ServerContainerOpenEvent(
    val containerId: Int,
    val title: Component,
    val titleStr: String,
) : Event()

@Threaded class ServerContainerCloseEvent(
    val containerId: Int,
) : Event()

@Threaded class ServerContainerSetContentEvent(
    val containerId: Int,
    val stateId: Int,
    val items: List<ItemStack>,
    val carriedItem: ItemStack, // Item in current cursor
) : Event() {
    inline fun forEach(cb: (Int, ItemStack?) -> Unit) {
        for (idx in items.indices) {
            cb(idx, items.getOrNull(idx))
        }
    }
}

class ClientContainerCloseEvent(
    val containerId: Int
) : CancellableEvent()

class SelectedItemRenderEvent(
    val ctx: GuiGraphics
) : CancellableEvent()

class ItemPickupEvent(
    val entity: ItemEntity,
    val entityId: Int,
) : Event()
