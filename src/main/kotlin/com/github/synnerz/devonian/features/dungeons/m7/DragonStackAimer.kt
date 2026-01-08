package com.github.synnerz.devonian.features.dungeons.m7

import com.github.synnerz.barrl.Context
import com.github.synnerz.devonian.api.Ping
import com.github.synnerz.devonian.api.dungeon.DungeonClass
import com.github.synnerz.devonian.api.dungeon.Dungeons
import com.github.synnerz.devonian.api.dungeon.Stages
import com.github.synnerz.devonian.api.events.*
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.dungeons.solvers.BlazeSolver
import com.github.synnerz.devonian.hud.texthud.TextHudFeature
import com.github.synnerz.devonian.utils.BasicState
import com.github.synnerz.devonian.utils.StringUtils
import com.github.synnerz.devonian.utils.math.Projectile
import net.minecraft.world.phys.Vec3
import org.joml.Vector3f
import java.awt.Color
import java.util.*
import kotlin.math.cos
import kotlin.math.sin

object DragonStackAimer : TextHudFeature(
    "dragonStackAimer",
    "",
    Categories.DUNGEONS,
    "catacombs",
    subcategory = "Solvers",
) {
    override fun createRequirements(): List<BasicState<Boolean>?> {
        return super.createRequirements() + listOf(
            Stages.WitherKing.isActiveState,
            Dungeons.selfClass.map { activeClasses.contains(it) },
        )
    }

    private val activeClasses = EnumSet.of(DungeonClass.Unknown, DungeonClass.Archer, DungeonClass.Berserk)

    private val SETTING_PING = addSwitch(
        "ping",
        true,
        "",
        "Account for Ping",
    )
    private val SETTING_HUD = addSwitch(
        "hud",
        true,
        "timer for when to start running",
        "HUD Display",
    )
    private val SETTING_AIM_COLOR = addColorPicker(
        "aimColor",
        Color(0, 255, 255).rgb,
        "these names are terrible so: color of the box you should look at",
        "Aim Color",
    )
    private val SETTING_TRACER_COLOR = addColorPicker(
        "tracerColor",
        Color(0, 255, 255).rgb,
        "color of the tracer pointing to the box you should look at",
        "Aim Tracer Color",
    )

    private var spawned: M7Dragon? = null
    private var isHigh = false
    private var ticks = 0
    private var done = false
    private var prevBest = 0
    private var data: Projectile.ProjectileData? = null

    override fun initialize() {
        on<ChatEvent> { event ->
            if (event.message == "[BOSS] Wither King: Incredible. You did what I couldn't do myself.") done = true
        }

        on<M7Events.DragonSpawned2> { event ->
            if (done) return@on
            spawned = event.dragon
            isHigh = event.isHigh
            ticks = EventBus.serverTicks() + 100
        }

        on<ServerTickEvent> {
            val drag = spawned ?: return@on
            var ttl = (ticks - EventBus.serverTicks()).toDouble()
            if (ttl < -30.0) {
                data = null
                return@on
            }
            if (SETTING_PING.get()) ttl -= Ping.getMedianPing() / 50.0

            val (t, d) = Projectile.aim(
                ttl,
                drag.path,
                prevBest,
                0.001, -0.05, 3.0, 0.99, false,
                0.0, if (isHigh) 8.0 else 0.0, 0.0,
            ) ?: return@on

            prevBest = t
            data = d
        }

        on<RenderOverlayEvent> { event ->
            val remaining = ticks - EventBus.serverTicks()
            if (remaining <= 0) return@on

            val d = data ?: return@on

            val ttl = ((remaining - d.ticks) * 50 - (if (SETTING_PING.get()) Ping.getMedianPing() else 0.0)).toInt()
            if (ttl < -1000) return@on
            if (ttl < 0) setLine("&bNOW")
            else setLine("${StringUtils.colorForNumber(ttl, 5000)}${ttl}")

            draw(event.ctx)
        }.setEnabled(SETTING_HUD.state)

        on<RenderWorldEvent> { event ->
            if (ticks <= 0) return@on

            val d = data ?: return@on

            val x = 50.0 * sin(d.phi) * cos(d.theta)
            val y = 50.0 * cos(d.phi)
            val z = 50.0 * sin(d.phi) * sin(d.theta)
            val w = 1.0

            Context.Immediate?.renderFilledBox(
                x - w * 0.5,
                y - w * 0.5,
                z - w * 0.5,
                w, w,
                SETTING_AIM_COLOR.getColor(),
                phase = true,
                translate = false,
            )
            val cam = event.ctx.worldState().cameraRenderState.pos
            val look = event.ctx.worldState().cameraRenderState.orientation.transform(Vector3f(0f, 0f, -1f))
            BlazeSolver.renderLine(
                cam.add(x, y, z),
                cam.add(Vec3(look)),
                SETTING_TRACER_COLOR.getColor(),
                ctx = event.ctx,
            )
        }
    }

    override fun getEditText(): List<String> = listOf((5000L - (System.currentTimeMillis() % 5000L)).toString())

    override fun onWorldChange(event: WorldChangeEvent) {
        spawned = null
        isHigh = false
        ticks = 0
        done = false
        prevBest = 0
        data = null
    }
}