package com.github.synnerz.devonian.api

import com.github.synnerz.devonian.Devonian
import com.github.synnerz.devonian.api.events.EventBus
import com.github.synnerz.devonian.api.events.ServerTickEvent
import kotlinx.atomicfu.atomic
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.world.entity.Entity
import java.util.concurrent.PriorityBlockingQueue

object Scheduler {
    private val tasks = PriorityBlockingQueue<Task>(10, Comparator.comparingInt { it.delay })
    private var tick = atomic(0)
    private val tasksServer = PriorityBlockingQueue<Task>(10, Comparator.comparingInt { it.delay })
    private var tickServer = atomic(0)

    data class Task(var delay: Int, val cb: () -> Unit)

    init {
        ClientTickEvents.START_CLIENT_TICK.register {
            val curr = tick.incrementAndGet()
            while (tasks.isNotEmpty() && tasks.peek().delay <= curr) {
                val task = tasks.poll() ?: return@register
                Devonian.minecraft.execute(task.cb)
            }
        }
        EventBus.on<ServerTickEvent> {
            val curr = tickServer.incrementAndGet()
            while (tasksServer.isNotEmpty() && tasksServer.peek().delay <= curr) {
                val task = tasksServer.poll() ?: return@on
                scheduleTask(cb = task.cb)
            }
        }
    }

    @JvmOverloads
    fun scheduleTask(delay: Int = 1, cb: () -> Unit) {
        tasks.add(Task(tick.value + delay, cb))
    }

    @JvmOverloads
    fun scheduleStandName(entity: Entity, cb: () -> Unit, depth: Int = 0) {
        // TODO: this entire method shouldn't even be needed
        //  just listen for the name change packet.
        if (depth > 10) return
        scheduleTask(2) {
            if (entity.name.string !== "Armor Stand") {
                cb()
                return@scheduleTask
            }

            scheduleStandName(entity, cb, depth + 1)
        }
    }

    @JvmOverloads
    fun scheduleServerTask(delay: Int = 1, cb: () -> Unit) {
        tasksServer.add(Task(tickServer.value + delay, cb))
    }
}