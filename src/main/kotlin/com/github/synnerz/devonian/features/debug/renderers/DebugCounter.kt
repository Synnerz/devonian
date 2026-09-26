package com.github.synnerz.devonian.features.debug.renderers

import com.github.synnerz.devonian.api.events.RenderOverlayEvent
import com.github.synnerz.devonian.api.events.TickEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.hud.texthud.TextHudFeature
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue

object DebugCounter : TextHudFeature(
    "debugCounter",
    "Shows # of times something was called within a time limit.",
    Categories.DEBUG,
    subcategory = "Renderers",
) {
    override fun getEditText(): List<String> = listOf("Name: 1")

    private data class Counter(var ttl: Int, val q: ConcurrentLinkedQueue<Long> = ConcurrentLinkedQueue<Long>())
    private val data = ConcurrentHashMap<String, Counter>()

    private fun getTime(): Long = System.currentTimeMillis()

    fun tick(name: String, ms: Int) {
        if (!isEnabled()) return

        val c = data.getOrPut(name) { Counter(ms) }
        c.ttl = ms
        c.q.add(getTime())
    }

    override fun initialize() {
        on<TickEvent> {
            val time = getTime()

            data.values.removeIf { (ttl, q) ->
                var l = q.size
                while (--l >= 0) {
                    val t = q.peek() ?: break
                    if (time - ttl > t) q.poll()
                    else break
                }

                return@removeIf q.isEmpty()
            }

            setLines(data.entries.map { "${it.key}: ${it.value.q.size}" })
        }

        on<RenderOverlayEvent> { event ->
            draw(event.ctx)
        }
    }
}