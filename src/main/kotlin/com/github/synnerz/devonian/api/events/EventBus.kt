package com.github.synnerz.devonian.api.events

import com.github.synnerz.devonian.utils.StringUtils.clearCodes
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents
import net.minecraft.network.protocol.common.ClientboundPingPacket
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket
import net.minecraft.network.protocol.game.ClientboundSoundPacket
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket
import net.minecraft.sounds.SoundSource
import org.lwjgl.glfw.GLFW
import kotlin.jvm.optionals.getOrNull

object EventBus {
    var totalTicks = 0
    private val teamRegex = "^team_(\\d+)$".toRegex()
    val events = hashMapOf<String, MutableList<Any>>()

    init {
        ClientEntityEvents.ENTITY_LOAD.register { entity, _ ->
            post(EntityJoinEvent(entity))
        }
        ClientEntityEvents.ENTITY_UNLOAD.register { entity, _ ->
            post(EntityLeaveEvent(entity))
        }
        ClientTickEvents.START_CLIENT_TICK.register { post(TickEvent(it)) }
        WorldRenderEvents.LAST.register { post(RenderWorldEvent(it)) }
        ClientLifecycleEvents.CLIENT_STARTED.register { post(GameLoadEvent(it)) }
        ClientLifecycleEvents.CLIENT_STOPPING.register { post(GameUnloadEvent(it)) }
        ClientWorldEvents.AFTER_CLIENT_WORLD_CHANGE.register { mc, world ->
            WorldChangeEvent(mc, world).post()
            totalTicks = 0
        }
        ScreenEvents.BEFORE_INIT.register { _, screen, _, _ ->
            ScreenMouseEvents.allowMouseClick(screen).register { _, mx, my, mbtn ->
                val event = GuiClickEvent(mx, my, mbtn, true, screen)
                post(event)
                !event.isCancelled()
            }

            ScreenMouseEvents.allowMouseRelease(screen).register { _, mx, my, mbtn ->
                val event = GuiClickEvent(mx, my, mbtn, false, screen)
                post(event)
                !event.isCancelled()
            }

            ScreenKeyboardEvents.allowKeyPress(screen).register { _, key, scancode, _ ->
                val event = GuiKeyEvent(
                    GLFW.glfwGetKeyName(key, scancode),
                    key,
                    scancode,
                    screen
                )
                post(event)
                !event.isCancelled()
            }
        }
        WorldRenderEvents.BLOCK_OUTLINE.register { worldContext, blockContext ->
            !BlockOutlineEvent(worldContext, blockContext).post()
        }

        on<PacketReceivedEvent> { event ->
            val packet = event.packet

            if (packet is ClientboundSoundPacket) {
                val sound = packet.sound.unwrapKey().getOrNull()?.registry() ?: return@on
                if (onSoundPacket(
                        "${sound.namespace}:${sound.path}",
                        packet.pitch,
                        packet.volume,
                        packet.source,
                        packet.x, packet.y, packet.z,
                        packet.seed
                ))
                    event.ci.cancel()
                return@on
            }

            if (packet is ClientboundPlayerInfoUpdatePacket) {
                val action = packet.actions().firstOrNull() ?: return@on
                if (action === ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER) {
                    packet.entries().forEach {
                        val name = it.displayName ?: return@forEach
                        TabAddEvent(name.string.clearCodes()).post()
                    }
                    return@on
                }
                if (action !== ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME) return@on

                packet.entries().forEach {
                    val name = it.displayName ?: return@forEach
                    TabUpdateEvent(name.string.clearCodes()).post()
                }
                return@on
            }

            if (packet is ClientboundPingPacket) {
                totalTicks++
                ServerTickEvent(totalTicks).post()
                return@on
            }

            if (packet is ClientboundSetPlayerTeamPacket) {
                if (packet.parameters.isEmpty) return@on
                val team = packet.parameters?.get() ?: return@on
                val teamPrefix = team.playerPrefix.string
                val teamSuffix = team.playerSuffix.string
                if (teamPrefix.isEmpty()) return@on
                if (!packet.name.matches(teamRegex)) return@on
                ScoreboardEvent("${teamPrefix}${teamSuffix.trim()}").post()
                return@on
            }

            if (packet !is ClientboundSystemChatPacket) return@on
            if (packet.overlay) return@on

            val content = packet.content ?: return@on
            val message = content.string.clearCodes()

            val specialized = ChatChannelEvent.from(message, content)
            val b1 = ChatEvent(message, content).post()
            val b2 = specialized?.post() ?: false

            if (b1 || b2) event.ci.cancel()
        }

        ClientLifecycleEvents.CLIENT_STARTED.register { client ->
            PostClientInit(client).post()
        }
    }

    fun onSoundPacket(
        soundEvent: String,
        pitch: Float,
        volume: Float,
        category: SoundSource,
        x: Double, y: Double, z: Double,
        seed: Long
    ): Boolean =
        SoundPlayEvent(soundEvent, pitch, volume, category, x, y, z, seed).post()

    fun serverTicks(): Int = totalTicks

    inline fun <reified T : Event> on(noinline cb: (T) -> Unit): EventListener {
        return on<T>(cb, true)
    }

    inline fun <reified T : Event> on(noinline cb: (T) -> Unit, add: Boolean = true): EventListener {
        if (add) events.getOrPut(T::class.java.name) { mutableListOf() }.add(cb)
        return object : EventListener {
            override fun remove() = remove<T>(cb)
            override fun add() = events.getOrPut(T::class.java.name) { mutableListOf() }.add(cb)
        }
    }

    inline fun <reified T : Event> remove(noinline cb: (T) -> Unit) = events[T::class.java.name]?.remove(cb)

    @Suppress("UNCHECKED_CAST")
    fun <T : Event> post(event: T) {
        val listeners = events[event::class.java.name] ?: return
        if (listeners.isEmpty()) return

        for (cb in listeners.toList()) {
            (cb as (T) -> Unit).invoke(event)
        }
    }

    interface EventListener {
        fun remove(): Boolean?
        fun add(): Boolean
    }
}