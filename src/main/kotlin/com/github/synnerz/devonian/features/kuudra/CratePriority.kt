package com.github.synnerz.devonian.features.kuudra
import com.github.synnerz.devonian.api.events.RenderOverlayEvent
import com.github.synnerz.devonian.api.events.RenderWorldEvent
import com.github.synnerz.devonian.api.events.TickEvent
import com.github.synnerz.devonian.api.events.kuudra.KuudraEvents
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.utils.render.Render2D
import com.github.synnerz.devonian.utils.render.Render3DImmediate
import java.awt.Color
import kotlin.math.sqrt

object CratePriority : Feature(
    "createPriority",
    "tells you which crate to prioritize whenever the one you are standing near does not spawn",
    Categories.KUUDRA,
    "kuudra",
    searchTags = setOf("supply", "kuudra")
) {
    private val safeSpots = mapOf(
        "x" to Triple(-133, 77, -138),
        "tri" to Triple(-67, 77, -122),
        "equals" to Triple(-64, 76, -87),
        "slash" to Triple(-112, 76, -68),
        "shop" to Triple(-85, 78, -128),
        "shop edge" to Triple( -70, 79, -134),
        "equals cannon" to Triple( -68, 78, -104),
        "x cannon" to Triple( -130, 78, -114),
        "square" to Triple(-140, 78, -90),
    )
    private val cratePriorities = mapOf<String, List<CratePriority>>(
        "x" to listOf(CratePriority.X, CratePriority.SQUARE, CratePriority.SQUARE, CratePriority.SHOP, CratePriority.X_CANNON),
        "tri" to listOf(CratePriority.TRI, CratePriority.SQUARE, CratePriority.SQUARE, CratePriority.X_CANNON, CratePriority.SHOP),
        "equals" to listOf(CratePriority.EQUALS, CratePriority.SHOP, CratePriority.SQUARE, CratePriority.X_CANNON, CratePriority.SQUARE),
        "slash" to listOf(CratePriority.SLASH, CratePriority.SQUARE, CratePriority.SHOP, CratePriority.X_CANNON, CratePriority.SQUARE),
        "shop" to listOf(CratePriority.SHOP, CratePriority.SQUARE, CratePriority.SQUARE, CratePriority.X_CANNON, CratePriority.X_CANNON),
        "shop edge" to listOf(CratePriority.SHOP_EDGE, CratePriority.SHOP, CratePriority.SQUARE, CratePriority.SQUARE, CratePriority.X_CANNON, CratePriority.X_CANNON),
        "equals cannon" to listOf(CratePriority.EQUALS_CANNON, CratePriority.EQUALS, CratePriority.SHOP, CratePriority.SQUARE, CratePriority.X_CANNON, CratePriority.SQUARE),
        "x cannon" to listOf(CratePriority.X_CANNON, CratePriority.SHOP, CratePriority.SQUARE, CratePriority.SQUARE, CratePriority.SHOP),
        "square" to listOf(CratePriority.SQUARE, CratePriority.SHOP, CratePriority.X_CANNON, CratePriority.X_CANNON, CratePriority.SHOP),
    )
    private val color = Color(0, 255, 255)
    private val color2 = Color(0, 150, 255, 80)
    private val color3 = Color(0, 255, 255, 255)
    private val availableSpots = mutableSetOf<String>()
    private var currentSpot = ""
    private var spotToGo = ""

    enum class CratePriority(val key: String) {
        X("x"),
        TRI("tri"),
        EQUALS("equals"),
        SLASH("slash"),
        SHOP("shop"),
        SHOP_EDGE("shop edge"),
        EQUALS_CANNON("equals cannon"),
        X_CANNON("x cannon"),
        SQUARE("square"),
    }

    override fun initialize() {
        on<TickEvent> {
            if (!KuudraEvents.inCratePhase()) return@on
            val player = minecraft.player ?: return@on
            val px = player.x
            val py = player.y
            val pz = player.z

            if (currentSpot.isEmpty()) currentSpot = safeSpots.minByOrNull { (k, v) ->
                val dx = v.first - px
                val dy = v.second - py
                val dz = v.third - pz

                sqrt(dx * dx + dy * dy + dz * dz)
            }?.key ?: ""

            if (currentSpot.isEmpty() || spotToGo.isNotEmpty()) return@on

            val isAvailable = availableSpots.contains(currentSpot)
            if (isAvailable) {
                spotToGo = currentSpot
                return@on
            }
            val order = cratePriorities[currentSpot] ?: return@on

            for (idx in 1..order.lastIndex) {
                val name = order.getOrNull(idx) ?: continue
                if (!availableSpots.contains(name.key)) continue

                spotToGo = name.key
            }
        }

        on<KuudraEvents.CrateSpawn> { event ->
            val pos = event.crate.pos() ?: return@on

            val spot = safeSpots.entries.find { (k, v) ->
                val dx = v.first - pos.x
                val dy = v.second - pos.y
                val dz = v.third - pos.z

                sqrt(dx * dx + dy * dy + dz * dz) < 25
            } ?: return@on

            availableSpots.add(spot.key)
        }

        on<KuudraEvents.CrateDespawn> { event ->
            // TODO: if crate pickup failed it will stay despawned
            val pos = event.crate.pos() ?: return@on

            val spot = safeSpots.entries.find { (k, v) ->
                val dx = v.first - pos.x
                val dy = v.second - pos.y
                val dz = v.third - pos.z

                sqrt(dx * dx + dy * dy + dz * dz) < 20
            } ?: return@on

            val player = minecraft.player ?: return@on
            val dx = spot.value.first - player.x
            val dy = spot.value.second - player.y
            val dz = spot.value.third - player.z
            val dist = sqrt(dx * dx + dy * dy + dz * dz)
            if (dist > 30) return@on

            availableSpots.remove(spot.key)
            if (spotToGo == spot.key)
                spotToGo = ""
        }

        on<RenderWorldEvent> {
            if (!KuudraEvents.inCratePhase() || spotToGo.isEmpty()) return@on
            val ( x, y, z ) = safeSpots[spotToGo] ?: return@on

            Render3DImmediate.renderString(
                spotToGo,
                x + 0.5, y + 2.0, z + 0.5,
                phase = true,
            )
            Render3DImmediate.renderWireframeBox(
                x + 0.5, y.toDouble(), z + 0.5,
                1.0, 1.0,
                color,
                phase = true
            )
            Render3DImmediate.renderFilledBox(
                x + 0.5, y.toDouble(), z + 0.5,
                1.0, 1.0,
                color2,
                phase = true
            )
            Render3DImmediate.renderTracer(
                x + 0.5, y + 1.5, z + 0.5,
                color3,
                lineWidth = 2.0
            )
        }

        on<RenderOverlayEvent> {
            if (!KuudraEvents.inCratePhase() || currentSpot.isEmpty()) return@on

            it.ctx.text(
                minecraft.font,
                currentSpot,
                10, 10, -1,
                true,
            )

            Render2D.drawStringNW(
                it.ctx,
                availableSpots.joinToString("\n"),
                100, 10,
            )

            it.ctx.text(
                minecraft.font,
                spotToGo,
                200, 10, -1,
                true,
            )
        }
    }
}