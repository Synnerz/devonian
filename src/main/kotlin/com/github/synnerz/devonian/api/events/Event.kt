package com.github.synnerz.devonian.api.events

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
import net.minecraft.sound.SoundCategory
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

open class CriteriaEvent(val message: String) : CancellableEvent() {
    fun matches(criteria: Regex): List<String>? {
        val matches = criteria.matchEntire(message) ?: return null
        return matches.groupValues.drop(1)
    }
}

open class ChatEvent(message: String, val text: Text) : CriteriaEvent(message)

abstract class ChatChannelEvent(message: String, text: Text, val name: String, val userMessage: String) :
    ChatEvent(message, text) {
    class AllChatEvent(message: String, text: Text, name: String, userMessage: String, val level: Int) :
        ChatChannelEvent(message, text, name, userMessage)

    class PartyChatEvent(message: String, text: Text, name: String, userMessage: String) :
        ChatChannelEvent(message, text, name, userMessage)

    class CoopChatEvent(message: String, text: Text, name: String, userMessage: String) :
        ChatChannelEvent(message, text, name, userMessage)

    class GuildChatEvent(message: String, text: Text, name: String, userMessage: String) :
        ChatChannelEvent(message, text, name, userMessage)

    abstract class PrivateChatEvent(message: String, text: Text, name: String, userMessage: String) :
        ChatChannelEvent(message, text, name, userMessage) {
        class IncomingPrivateChatEvent(message: String, text: Text, name: String, userMessage: String) :
            PrivateChatEvent(message, text, name, userMessage)

        class OutgoingPrivateChatEvent(message: String, text: Text, name: String, userMessage: String) :
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

        fun from(message: String, text: Text): ChatChannelEvent? {
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

class RenderSlotEvent(val slot: Slot, val ctx: DrawContext) : CancellableEvent()

class SoundPlayEvent(
    val sound: String,
    val pitch: Float,
    val volume: Float,
    val category: SoundCategory,
    val x: Double,
    val y: Double,
    val z: Double,
    val seed: Long
) : CancellableEvent()

class PostClientInit(val minecraft: MinecraftClient) : Event()