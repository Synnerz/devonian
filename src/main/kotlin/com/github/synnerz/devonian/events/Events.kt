package com.github.synnerz.devonian.events

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents
import net.fabricmc.fabric.api.event.Event
import net.fabricmc.fabric.api.event.EventFactory
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

object Events {
    private val tasks = mutableListOf<Task>()
    class Task(var delay: Int, val cb: () -> Unit)

    // Interfaces
    fun interface PacketCallback {
        fun receive(packet: Packet<*>?, cb: CallbackInfo)
    }

    fun interface EntityCallback {
        fun onEntity(entity: Entity, cb: CallbackInfo)
    }

    fun interface DropItemCallback {
        fun onDrop(itemStack: ItemStack, entireStack: Boolean, cb: CallbackInfoReturnable<Boolean>)
    }

    fun interface EntityRenderCallback {
        fun onRender(entity: Entity, matrixStack: MatrixStack, consumer: VertexConsumerProvider, light: Int, ci: CallbackInfo)
    }

    fun interface GuiCallback {
        fun trigger(screen: Screen?, ci: CallbackInfo)
    }

    fun interface ParticleCallback {
        fun trigger(particle: Particle, ci: CallbackInfo)
    }

    // Init block for event listeners
    init {
        ClientTickEvents.START_CLIENT_TICK.register {
            synchronized(tasks) {
                tasks.removeAll {
                    if (it.delay-- > 0) return@removeAll false

                    MinecraftClient.getInstance().submit(it.cb)

                    return@removeAll true
                }
            }
        }
    }

    class CancellableEvent(var cancelled: Boolean = false) {
        fun cancel() {
            this.cancelled = true
        }
    }

    // Event fields
    @JvmField
    val PACKET_SENT = bake<PacketCallback> { listeners ->
        PacketCallback { packet, cb -> listeners.forEach { it.receive(packet, cb) } }
    }

    @JvmField
    val PACKET_RECEIVED = bake<PacketCallback> { listeners ->
        PacketCallback { packet, cb -> listeners.forEach { it.receive(packet, cb) } }
    }

    @JvmField
    val DROP_HAND_ITEM = bake<DropItemCallback> { listeners ->
        DropItemCallback { itemStack, entireStack, cb -> listeners.forEach { it.onDrop(itemStack, entireStack, cb) } }
    }

    @JvmField
    val PRE_ENTITY_RENDER = bake<EntityRenderCallback> { listeners ->
        EntityRenderCallback { entity, matrixStack, consumer, light, ci -> listeners.forEach { it.onRender(entity, matrixStack, consumer, light, ci) } }
    }

    @JvmField
    val GUI_OPEN = bake<GuiCallback> { listeners ->
        GuiCallback { screen, ci -> listeners.forEach { it.trigger(screen, ci) } }
    }

    @JvmField
    val GUI_CLOSE = bake<GuiCallback> { listeners ->
        GuiCallback { screen, ci -> listeners.forEach { it.trigger(screen, ci) } }
    }

    @JvmField
    val PARTICLE_SPAWN = bake<ParticleCallback> { listeners ->
        ParticleCallback { particle, ci -> listeners.forEach { it.trigger(particle, ci) } }
    }

    // Event hooks
    fun onPacketSent(cb: (Packet<*>?, CallbackInfo) -> Unit) = PACKET_SENT.register(cb)

    fun onPacketReceived(cb: (Packet<*>?, CallbackInfo) -> Unit) = PACKET_RECEIVED.register(cb)

    fun onChat(cb: (args: List<Any>, event: CancellableEvent) -> Unit) {
        ClientReceiveMessageEvents.ALLOW_CHAT.register { message, _, _, _, _ ->
            val event = CancellableEvent()

            message.string?.let { cb(listOf(it), event) }

            return@register !event.cancelled
        }
    }

    fun onChat(criteria: Regex, cb: (args: List<Any>, event: CancellableEvent) -> Unit) {
        ClientReceiveMessageEvents.ALLOW_CHAT.register { message, _, _, _, _ ->
            val event = CancellableEvent()

            val str = message.string ?: return@register !event.cancelled

            val matches = criteria.findAll(str)
            cb(matches.flatMap { it.groupValues.drop(1) }.toList(), event)

            return@register !event.cancelled
        }
    }

    fun onEntityAdd(cb: (Entity, ClientWorld) -> Unit) {
        ClientEntityEvents.ENTITY_LOAD.register(cb)
    }

    fun onEntityRemove(cb: (Entity, ClientWorld) -> Unit) {
        ClientEntityEvents.ENTITY_UNLOAD.register(cb)
    }

    fun onDropHandItem(cb: (ItemStack, Boolean, CallbackInfoReturnable<Boolean>) -> Unit) {
        DROP_HAND_ITEM.register(cb)
    }

    fun scheduleTask(delay: Int, cb: () -> Unit) {
        synchronized(tasks) {
            tasks.add(Task(delay, cb))
        }
    }

    @JvmOverloads
    fun scheduleStandName(entity: Entity, cb: () -> Unit, depth: Int = 0) {
        if (depth > 10) return
        scheduleTask(2) {
            if (entity.name.string !== "Armor Stand") {
                cb()
                return@scheduleTask
            }

            scheduleStandName(entity, cb, depth + 1)
        }
    }

    fun onTick(cb: (MinecraftClient) -> Unit) {
        ClientTickEvents.START_CLIENT_TICK.register(cb)
    }

    fun onPostRenderWorld(cb: (WorldRenderContext) -> Unit) {
        WorldRenderEvents.LAST.register(cb)
    }

    fun onGameLoad(cb: (MinecraftClient) -> Unit) {
        ClientLifecycleEvents.CLIENT_STARTED.register(cb)
    }

    fun onGameUnload(cb: (MinecraftClient) -> Unit) {
        ClientLifecycleEvents.CLIENT_STOPPING.register(cb)
    }

    fun onWorldChange(cb: (MinecraftClient, ClientWorld) -> Unit) {
        ClientWorldEvents.AFTER_CLIENT_WORLD_CHANGE.register(cb)
    }

    fun onPreRenderEntity(cb: (Entity, MatrixStack, VertexConsumerProvider, Int, CallbackInfo) -> Unit) {
        PRE_ENTITY_RENDER.register(cb)
    }

    fun onGuiOpen(cb: (Screen?, CallbackInfo) -> Unit) {
        GUI_OPEN.register(cb)
    }

    fun onGuiClose(cb: (Screen?, CallbackInfo) -> Unit) {
        GUI_CLOSE.register(cb)
    }

    fun onParticleSpawn(cb: (Particle, CallbackInfo) -> Unit) {
        PARTICLE_SPAWN.register(cb)
    }

    // Baker
    private inline fun <reified T> bake(noinline v: (Array<T>) -> T): Event<T> =
        EventFactory.createArrayBacked(T::class.java, v)
}