package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.barrl.Context
import com.github.synnerz.devonian.api.Scheduler
import com.github.synnerz.devonian.api.events.*
import com.github.synnerz.devonian.features.Feature
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket
import net.minecraft.world.level.block.Blocks
import java.awt.Color

object LividSolver : Feature(
    "lividSolver",
    "Highlights the correct livid in F5/M5",
    "Dungeons",
    "catacombs"
) {
    private val lividStartRegex = "^\\[BOSS] Livid: Welcome, you've arrived right on time\\. I am Livid, the Master of Shadows\\.$".toRegex()
    private val lividSpawnedRegex = "^\\[BOSS] Livid: I respect you for making it to here, but I'll be your undoing\\.$".toRegex()
    private val mapBlocks = mapOf(
        Blocks.WHITE_WOOL to "Vendetta",
        Blocks.PINK_WOOL to "Crossed",
        Blocks.YELLOW_WOOL to "Arcade",
        Blocks.LIME_WOOL to "Smile",
        Blocks.GRAY_WOOL to "Doctor",
        Blocks.PURPLE_WOOL to "Purple",
        Blocks.GREEN_WOOL to "Frog",
        Blocks.BLUE_WOOL to "Scream",
        Blocks.RED_WOOL to "Hockey"
    )
    private val SETTING_BOX_COLOR = addColorPicker(
        "boxColor",
        "",
        "Livid Box Color",
        Color(0, 255, 255).rgb
    )
    var inBoss = false
    var currentLivid: String? = null
    var lividId = -1

    override fun initialize() {
        on<ChatEvent> { event ->
            if (event.matches(lividSpawnedRegex) != null) {
                // Fallback to red if there's no block update packet found
                Scheduler.scheduleServerTask(2) {
                    if (lividId != -1) return@scheduleServerTask
                    currentLivid = "Hockey"
                }
                return@on
            }
            event.matches(lividStartRegex) ?: return@on
            inBoss = true
        }

        on<PacketReceivedEvent> { event ->
            if (!inBoss) return@on
            val packet = event.packet
            if (packet !is ClientboundSectionBlocksUpdatePacket) return@on

            packet.runUpdates { t, u ->
                if (t.x != 5 || t.y != 108 || t.z != 43) return@runUpdates
                if (mapBlocks[u.block] != null) currentLivid = mapBlocks[u.block]
            }
        }

        on<EntityJoinEvent> { event ->
            Scheduler.scheduleServerTask {
                val entity = event.entity
                val name = entity.name.string ?: return@scheduleServerTask
                if (!name.contains("$currentLivid Livid")) return@scheduleServerTask

                lividId = entity.id
            }
        }

        on<RenderEntityEvent> { event ->
            val entity = event.entity
            val matrixStack = event.matrixStack
            if (entity.id != lividId) return@on

            val cam = minecraft.gameRenderer.mainCamera.position.reverse()
            val width = entity.bbWidth.toDouble()
            val halfWidth = width / 2.0

            matrixStack.pushPose()
            matrixStack.translate(cam.x, cam.y, cam.z)

            Context.Immediate?.renderBox(
                entity.x - halfWidth, entity.y, entity.z - halfWidth,
                width, entity.bbHeight.toDouble(),
                SETTING_BOX_COLOR.getColor(), translate = false
            )

            matrixStack.popPose()
        }
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        inBoss = false
        currentLivid = null
        lividId = -1
    }
}