package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.devonian.api.dungeon.Stages
import com.github.synnerz.devonian.api.events.NameChangeEvent
import com.github.synnerz.devonian.api.events.PacketReceivedEvent
import com.github.synnerz.devonian.api.events.RenderWorldEvent
import com.github.synnerz.devonian.api.events.TickEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.utils.BasicState
import com.github.synnerz.devonian.utils.render.Render3DImmediate
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import java.awt.Color
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CopyOnWriteArrayList

object SpiritBearHighlight : Feature(
    "spiritBearHighlight",
    "Highlights the spirit bear whenever spawned",
    Categories.DUNGEONS,
    "catacombs",
    subcategory = "HUD",
    searchTags = setOf("f4", "m4"),
) {
    override fun createRequirements(): List<BasicState<Boolean>?> {
        return super.createRequirements() + listOf(Stages.F4.isActiveState)
    }

    private val SETTING_OUTLINE_COLOR = addColorPicker(
        "outlineColor",
        Color(255, 0, 255, 255).rgb,
        "Color of the outline",
        "Outline Color"
    )
    private val SETTING_FILLED_COLOR = addColorPicker(
        "fillColor",
        Color(255, 0, 255, 64).rgb,
        "",
        "Filled Color",
    )
    private val SETTING_OUTLINE_WIDTH = addSlider(
        "outlineWidth",
        2.0,
        0.0, 10.0,
        "",
        "Outline Width"
    )
    private val nameRegex = "^\uE071 Spirit Bear [\\dkMB]+.$".toRegex()
    private val entities = ConcurrentLinkedQueue<Int>()
    private val bears = mutableListOf<LivingEntity>()
    private val previousStands = CopyOnWriteArrayList<Int>()

    override fun initialize() {
        on<PacketReceivedEvent> { event ->
            val packet = event.packet
            if (packet !is ClientboundAddEntityPacket) return@on
            if (packet.type != EntityType.ARMOR_STAND) return@on

            previousStands.add(packet.id)
        }

        on<NameChangeEvent> { event ->
            if (!previousStands.contains(event.entityId)) return@on
            val name = event.name
            if (!nameRegex.matches(name)) return@on

            previousStands.remove(event.entityId)
            entities.add(event.entityId - 1)
        }

        on<TickEvent> {
            val world = minecraft.level ?: return@on
            var length = entities.size

            while (--length >= 0) {
                val id = entities.poll() ?: break
                val entity = world.getEntity(id) as? LivingEntity

                if (entity == null) entities.offer(id)
                else bears.add(entity)
            }
        }

        on<RenderWorldEvent> {
            bears.removeIf { entity ->
                if (entity.isDeadOrDying || entity.isRemoved) return@removeIf true

                val pos = entity.getPosition(minecraft.deltaTracker.getGameTimeDeltaPartialTick(false))
                Render3DImmediate.renderWireframeBox(
                    pos.x,
                    pos.y,
                    pos.z,
                    0.8, 2.0,
                    SETTING_OUTLINE_COLOR.getColor(),
                    lineWidth = SETTING_OUTLINE_WIDTH.get(),
                    centered = true,
                )
                Render3DImmediate.renderFilledBox(
                    pos.x,
                    pos.y,
                    pos.z,
                    0.8, 2.0,
                    SETTING_FILLED_COLOR.getColor(),
                    centered = true,
                )

                false
            }
        }
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        entities.clear()
        bears.clear()
        previousStands.clear()
    }
}