package com.github.synnerz.devonian.features.dungeons.solvers

import com.github.synnerz.devonian.api.ChatUtils
import com.github.synnerz.devonian.api.WorldUtils
import com.github.synnerz.devonian.api.dungeon.DungeonEvent
import com.github.synnerz.devonian.api.dungeon.Stages
import com.github.synnerz.devonian.api.events.*
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.utils.BasicState
import com.github.synnerz.devonian.utils.render.Render3DImmediate
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.monster.Blaze
import net.minecraft.world.level.block.Blocks
import java.awt.Color
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

object BlazeSolver : Feature(
    "blazeSolver",
    "Highlights the correct blaze to shoot in blaze puzzle.",
    Categories.DUNGEONS,
    "catacombs",
    subcategory = "Solvers",
    searchTags = setOf("higher", "lower", "puzzle"),
) {
    private val SETTING_SEND_MSG = addSwitch(
        "sendMsg",
        false,
        "Sends \"Blaze done\" in party chat",
        "Blaze Done Message"
    )
    private val SETTING_RENDER_PARTICLES = addSwitch(
        "renderParticles",
        false,
        "Stops ALL particles from rendering whenever inside of blaze puzzle",
        "Stop Rendering Particles"
    )
    private val SETTING_RENDER_TRACER = addSwitch(
        "renderTracer",
        true,
        "Renders tracer to the blaze",
        "Tracer"
    )
    private val SETTING_RENDER_TRACER_WIDTH = addSlider(
        "renderTracerLine",
        2.0,
        1.0, 5.0,
        "",
        "Tracer Width"
    )
    private val SETTING_RENDER_TRACER_AMOUNT = addSelection(
        "renderTracerAmount",
        0,
        listOf("1", "2", "all"),
        "The amount of tracers which should be displayed",
        "Tracer Amount"
    )
    private val SETTING_DONT_RENDER_BLAZE = addSwitch(
        "dontRenderBlaze",
        false,
        "Stops blazes from rendering",
    )
    private val SETTING_COLOR_FIRST_OUTLINE = addColorPicker(
        "firstBlazeColorOutline",
        Color(0, 255, 0, 255).rgb,
        "Color for the first correct blaze outline",
        "First Blaze Outline Color",
    )
    private val SETTING_COLOR_FIRST_FILLED = addColorPicker(
        "firstBlazeColorFilled",
        Color(0, 255, 0, 80).rgb,
        "Color for the first correct blaze filled",
        "First Blaze Filled Color",
    )
    private val SETTING_COLOR_SECOND_OUTLINE = addColorPicker(
        "secondBlazeColorOutline",
        Color(255, 165, 0, 255).rgb,
        "Color for the second correct blaze outline",
        "Second Blaze Outline Color",
    )
    private val SETTING_COLOR_SECOND_FILLED = addColorPicker(
        "secondBlazeColorFilled",
        Color(255, 165, 0, 80).rgb,
        "Color for the second correct blaze filled",
        "Second Blaze Filled Color",
    )
    private val SETTING_COLOR_THIRD_OUTLINE = addColorPicker(
        "thirdBlazeColorOutline",
        Color(255, 0, 0, 255).rgb,
        "Color for the third correct blaze outline",
        "Third Blaze Outline Color",
    )
    private val SETTING_COLOR_THIRD_FILLED = addColorPicker(
        "thirdBlazeColorFilled",
        Color(255, 0, 0, 80).rgb,
        "Color for the third correct blaze filled",
        "Third Blaze Filled Color",
    )
    private val SETTING_COLOR_NORMAL_OUTLINE = addColorPicker(
        "normalBlazeColorOutline",
        Color(255, 255, 255, 255).rgb,
        "Color for the normal correct blaze outline",
        "Normal Blaze Outline Color",
    )
    private val SETTING_COLOR_NORMAL_FILLED = addColorPicker(
        "normalBlazeColorFilled",
        Color(255, 255, 255, 80).rgb,
        "Color for the normal correct blaze filled",
        "Normal Blaze Filled Color",
    )
    private val SETTING_EFFICIENT_BLOCK_COLOR_OUTLINE = addColorPicker(
        "efficientBlockColorOutline",
        Color(0, 255, 0, 255).rgb,
        "Color for the \"efficient\" block outline to stand at",
        "\"Efficient\" Blaze Block Outline Color",
    )
    private val SETTING_EFFICIENT_BLOCK_COLOR_FILLED = addColorPicker(
        "efficientBlockColorFilled",
        Color(0, 255, 0, 80).rgb,
        "Color for the \"efficient\" block filled to stand at",
        "\"Efficient\" Blaze Block Filled Color",
    )
    private val SETTING_ETHERWARP_COLOR_OUTLINE = addColorPicker(
        "etherwarpColorOutline",
        Color(255, 255, 0, 255).rgb,
        "Color for the etherwarp spot for \"efficient\" block",
        "Etherwarp Color Outline"
    )
    private val SETTING_ETHERWARP_COLOR_FILLED = addColorPicker(
        "etherwarpColorFilled",
        Color(255, 255, 0, 80).rgb,
        "Color for the etherwarp spot for \"efficient\" block",
        "Etherwarp Color Filled"
    )

    override fun createRequirements(): List<BasicState<Boolean>?> {
        return super.createRequirements() + listOf(Stages.Clear.isActiveState)
    }

    private val blazeHpRegex = "^\\[Lv15] . Blaze [\\d,]+/([\\d,]+)❤$".toRegex()
    private val entityList = ConcurrentHashMap<Int, Int>() // <entityId>: <MaxHP>
    private val etherSpots = mutableListOf<Triple<Int, Int, Int>>()
    var hasPlatform = false
    var inBlaze = false
    val blazes = CopyOnWriteArrayList<BlazeEntity>()
    var lastBlazes = 0
    var startedAt = 0
    var efficientPos: Triple<Int, Int, Int>? = null
    var hasSent = false

    data class BlazeEntity(val entity: Entity, val maxHP: Int)

    override fun initialize() {
        on<DungeonEvent.RoomEnter> { event ->
            val room = event.room
            if (room.name != "Blaze") return@on
            val platformPos = room.fromComp(15, 14) ?: return@on
            val blockState = WorldUtils.getBlockState(platformPos.first, 118, platformPos.second) ?: return@on
            inBlaze = true
            hasPlatform = blockState.block == Blocks.COBBLESTONE

            room.fromComp(20, 11)?.let { etherSpots.add(Triple(it.first, if (hasPlatform) 85 else 35, it.second)) }
            if (hasPlatform)
                room.fromComp(20, 11)?.let { etherSpots.add(Triple(it.first, 68, it.second)) }

            val rcomp = room.fromComp(20, 11) ?: return@on

            efficientPos = Triple(
                rcomp.first,
                if (hasPlatform) 103 else 53,
                rcomp.second
            )
        }

        on<DungeonEvent.RoomLeave> {
            if (inBlaze) blazes.clear()
            if (inBlaze) entityList.clear()
            inBlaze = false
            hasPlatform = false
            startedAt = 0
            lastBlazes = 0
            efficientPos = null
            etherSpots.clear()
        }

        on<ParticleSpawnEvent> { event ->
            if (!inBlaze) return@on

            event.cancel()
        }.setEnabled(SETTING_RENDER_PARTICLES.state)

        on<TickEvent> {
            if (!inBlaze) return@on
            blazes.clear()

            entityList.entries.forEach {
                val entityId = it.key
                val maxHP = it.value
                val entity = minecraft.level?.getEntity(entityId - 1) ?: return@forEach
                blazes.add(BlazeEntity(entity, maxHP))
            }

            if (blazes.size == 9 && startedAt == 0) startedAt = EventBus.serverTicks()
            if (blazes.isEmpty() && startedAt != 0 && lastBlazes == 1 && PuzzleTimers.isEnabled()) {
                val time = (EventBus.serverTicks() - startedAt) * 0.05
                val seconds = "%.2fs".format(time)
                ChatUtils.sendMessage("&bBlaze took&f: &6$seconds", true)
                blazes.clear()
                entityList.clear()
                inBlaze = false
                hasPlatform = false
                startedAt = 0
                lastBlazes = 0
                efficientPos = null
                etherSpots.clear()
                if (!hasSent) {
                    hasSent = true
                    if (SETTING_SEND_MSG.get()) ChatUtils.command("pc Blaze done")
                }
                return@on
            }

            blazes.sortBy { it.maxHP }

            // if it doesn't have platform, reverse the list
            if (!hasPlatform) blazes.reverse()
            lastBlazes = blazes.size
        }

        on<NameChangeEvent> { event ->
            if (event.type !== EntityType.ARMOR_STAND) return@on

            val name = event.name
            val entityId = event.entityId
            val match = blazeHpRegex.matchEntire(name) ?: return@on
            val maxHp = match.groupValues[1].replace(",", "").toInt()

            entityList[entityId] = maxHp
        }

        on<PreExtractRenderEntityEvent> { event ->
            if (!inBlaze) return@on
            val entity = event.entity
            if (entity !is Blaze) return@on

            event.cancel()
        }.setEnabled(SETTING_DONT_RENDER_BLAZE.state)

        on<RenderWorldEvent> {
            if (efficientPos != null) {
                if (etherSpots.isNotEmpty()) {
                    etherSpots.forEach { (x, y, z) ->
                        Render3DImmediate.renderWireframeBox(
                            x.toDouble(), y.toDouble(), z.toDouble(),
                            1.0, 1.0,
                            color = SETTING_ETHERWARP_COLOR_OUTLINE.getColor()
                        )

                        Render3DImmediate.renderFilledBox(
                            x.toDouble(), y.toDouble(), z.toDouble(),
                            1.0, 1.0,
                            color = SETTING_ETHERWARP_COLOR_FILLED.getColor()
                        )
                    }
                }

                Render3DImmediate.renderWireframeBox(
                    efficientPos!!.first.toDouble(), efficientPos!!.second.toDouble(), efficientPos!!.third.toDouble(),
                    1.0, 1.0,
                    color = SETTING_EFFICIENT_BLOCK_COLOR_OUTLINE.getColor()
                )

                Render3DImmediate.renderFilledBox(
                    efficientPos!!.first.toDouble(), efficientPos!!.second.toDouble(), efficientPos!!.third.toDouble(),
                    1.0, 1.0,
                    color = SETTING_EFFICIENT_BLOCK_COLOR_FILLED.getColor()
                )
            }

            val c1 = SETTING_COLOR_FIRST_OUTLINE.getColor()
            val c2 = SETTING_COLOR_SECOND_OUTLINE.getColor()
            val c3 = SETTING_COLOR_THIRD_OUTLINE.getColor()

            // yes i could make this dynamic, but why ?
            // it is pointless if we only need 3 entries
//            val blaze1 = blazes.getOrNull(0)?.entity ?: return@on
//            highlightBlaze(blaze1, c1, SETTING_COLOR_FIRST_FILLED.getColor(), SETTING_RENDER_TRACER_AMOUNT.get() >= 0)
//
//            val blaze2 = blazes.getOrNull(1)?.entity ?: return@on
//            highlightBlaze(blaze2, c2, SETTING_COLOR_SECOND_FILLED.getColor(), SETTING_RENDER_TRACER_AMOUNT.get() >= 1)
//
//            val blaze3 = blazes.getOrNull(2)?.entity
//            if (blaze3 != null) highlightBlaze(blaze3, c3, SETTING_COLOR_THIRD_FILLED.getColor(), SETTING_RENDER_TRACER_AMOUNT.get() == 2)

            var blaze1: Entity? = null
            var blaze2: Entity? = null
            var blaze3: Entity? = null

            for (idx in 0..blazes.lastIndex) {
                val entity = blazes.getOrNull(idx) ?: continue

                if (idx == 0) blaze1 = entity.entity
                if (idx == 1) blaze2 = entity.entity
                if (idx == 2) blaze3 = entity.entity

                val colorOutline = when (idx) {
                    0 -> SETTING_COLOR_FIRST_OUTLINE.getColor()
                    1 -> SETTING_COLOR_SECOND_OUTLINE.getColor()
                    2 -> SETTING_COLOR_THIRD_OUTLINE.getColor()
                    else -> SETTING_COLOR_NORMAL_OUTLINE.getColor()
                }
                val colorFilled = when (idx) {
                    0 -> SETTING_COLOR_FIRST_FILLED.getColor()
                    1 ->SETTING_COLOR_SECOND_FILLED.getColor()
                    2 -> SETTING_COLOR_THIRD_FILLED.getColor()
                    else -> SETTING_COLOR_NORMAL_FILLED.getColor()
                }
                val canTrace = when (idx) {
                    0,
                    1 -> SETTING_RENDER_TRACER_AMOUNT.get() >= idx
                    2 -> SETTING_RENDER_TRACER_AMOUNT.get() == idx
                    else -> false
                }

                highlightBlaze(
                    entity.entity,
                    colorOutline,
                    colorFilled,
                    canTrace
                )
            }

            if (blaze1 == null || blaze2 == null) return@on
            Render3DImmediate.renderLines(c1.alpha and c2.alpha and c3.alpha == 255) {
                val p1 = blaze1.position().add(0.0, 0.8, 0.0)
                val p2 = blaze2.position().add(0.0, 0.8, 0.0)
                val p3 = blaze3?.position()?.add(0.0, 0.8, 0.0)

                submit(p1, p2, c1, c2)
                if (p3 != null) submit(p2, p3, c2, c3)
            }
        }
    }

    private fun highlightBlaze(
        entity: Entity,
        outlineColor: Color = Color.GREEN,
        filledColor: Color = Color(0, 255, 0, 80),
        tracer: Boolean = false,
    ) {
        Render3DImmediate.renderWireframeBox(
            entity.x, entity.y, entity.z,
            0.9,
            entity.bbHeight.toDouble(),
            outlineColor,
            centered = true,
        )
        Render3DImmediate.renderFilledBox(
            entity.x, entity.y, entity.z,
            0.9,
            entity.bbHeight.toDouble(),
            filledColor,
            centered = true,
        )
        if (tracer && SETTING_RENDER_TRACER.get()) Render3DImmediate.renderTracer(
            entity.x, entity.y + 1.0, entity.z,
            outlineColor, lineWidth = SETTING_RENDER_TRACER_WIDTH.get()
        )
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        inBlaze = false
        hasPlatform = false
        startedAt = 0
        lastBlazes = 0
        efficientPos = null
        etherSpots.clear()
        blazes.clear()
        entityList.clear()
        hasSent = false
    }
}