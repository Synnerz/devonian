package com.github.synnerz.devonian.features.kuudra

import com.github.synnerz.devonian.api.events.ChatEvent
import com.github.synnerz.devonian.api.events.EntityJoinEvent
import com.github.synnerz.devonian.api.events.RenderWorldEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.utils.render.Render3DImmediate
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.monster.Giant
import java.awt.Color
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

object CratesWaypoints : Feature(
    "cratesWaypoints",
    "Adds waypoints to the crates that you need to pickup",
    Categories.KUUDRA,
    "kuudra",
    searchTags = setOf("supply", "kuudra")
) {
    private val SETTING_COLOR = addColorPicker(
        "color",
        Color(211, 0, 255).rgb,
        "The color of the waypoint",
        "Color"
    )
    private val supplyStartRegex = "^\\[NPC] Elle: Head over to the main platform, I will join you when I get a bite!$".toRegex()
    private val supplyEndRegex = "^(?:\\[[^]]+] )?\\w{1,16} recovered one of Elle's supplies! \\(6/6\\)".toRegex()
    private val startFuelRegex = "^\\[NPC] Elle: We need to find the fuel for the Ballista, it must have fallen in the lava with the rest of my supplies!$".toRegex()
    private val endFuelRegex = "^\\[NPC] Elle: POW! SURELY THAT'S IT! I don't think he has any more in him!$".toRegex()
    private val zombies = mutableListOf<LivingEntity>()
    private var canRender = false

    override fun initialize() {
        on<ChatEvent> { event ->
            event.matches(supplyStartRegex)?.let {
                canRender = true
                return@on
            }
            event.matches(supplyEndRegex)?.let {
                canRender = false
                return@on
            }
            event.matches(startFuelRegex)?.let {
                canRender = true
            }

            if (event.matches(endFuelRegex) == null) return@on
            canRender = false
        }

        on<EntityJoinEvent> { event ->
            val entity = event.entity
            if (entity !is Giant || entity.y > 67) return@on

            zombies.add(entity)
        }

        on<RenderWorldEvent> {
            if (!canRender || zombies.isEmpty()) return@on

            zombies.removeIf {
                val rot = it.yHeadRot

                Render3DImmediate.renderWaypoint(
                    it.x + 5 * cos((rot + 130) * (PI / 180)),
                    it.y + 5.0,
                    it.z + 5 * sin((rot + 130) * (PI / 180)),
                    SETTING_COLOR.getColor(),
                    phase = true
                )

                it.isDeadOrDying || it.isRemoved
            }
        }
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        canRender = false
        zombies.clear()
    }
}