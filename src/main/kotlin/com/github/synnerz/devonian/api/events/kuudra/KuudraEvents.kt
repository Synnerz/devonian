package com.github.synnerz.devonian.api.events.kuudra

import com.github.synnerz.devonian.api.Location
import com.github.synnerz.devonian.api.events.ChatEvent
import com.github.synnerz.devonian.api.events.EntityJoinEvent
import com.github.synnerz.devonian.api.events.Event
import com.github.synnerz.devonian.api.events.EventBus
import com.github.synnerz.devonian.api.events.RenderTickEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import net.minecraft.world.entity.monster.Giant
import java.lang.ref.WeakReference
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

object KuudraEvents {
    // TODO: kuudra state, stages, phases
    private val supplyStartRegex = "^\\[NPC] Elle: Head over to the main platform, I will join you when I get a bite!$".toRegex()
    private val supplyEndRegex = "^(?:\\[[^]]+] )?\\w{1,16} recovered one of Elle's supplies! \\(6/6\\)".toRegex()
    private val startFuelRegex = "^\\[NPC] Elle: We need to find the fuel for the Ballista, it must have fallen in the lava with the rest of my supplies!$".toRegex()
    private val endFuelRegex = "^\\[NPC] Elle: POW! SURELY THAT'S IT! I don't think he has any more in him!$".toRegex()
    private val giantEntities = mutableListOf<CrateGiant>()
    var inSupplyPhase = false
    var inFuelPhase = false

    class SupplyPhaseEnter : Event
    class SupplyPhaseLeave : Event
    class FuelPhaseEnter : Event
    class FuelPhaseLeave : Event
    class CrateSpawn(val crate: CrateGiant) : Event {
        fun entity() = crate.entity.get()
    }
    class CrateDespawn(val crate: CrateGiant) : Event {
        fun entity() = crate.entity.get()
    }

    data class CratePos(val x: Double, val y: Double, val z: Double)
    data class CrateGiant(
        val entity: WeakReference<Giant>,
    ) {
        fun pos(): CratePos?
            = entity.get()?.let {
                val rot = it.yHeadRot
                CratePos(
                    it.x + 5 * cos((rot + 130) * (PI / 180)),
                    it.y + 5.0,
                    it.z + 5 * sin((rot + 130) * (PI / 180)),
                )
        }
    }

    fun initialize() {
        EventBus.on<ChatEvent> { event ->
            event.matches(supplyStartRegex)?.let {
                inSupplyPhase = true
                SupplyPhaseEnter().post()
                return@on
            }
            event.matches(supplyEndRegex)?.let {
                inSupplyPhase = false
                SupplyPhaseLeave().post()
                return@on
            }
            event.matches(startFuelRegex)?.let {
                inFuelPhase = true
                FuelPhaseEnter().post()
                return@on
            }
            if (event.matches(endFuelRegex) == null) return@on

            inFuelPhase = false
            FuelPhaseLeave().post()
        }.setEnabled(Location.stateInArea("kuudra"))

        EventBus.on<EntityJoinEvent> { event ->
            if (!inCratePhase()) return@on
            val entity = event.entity
            if (entity !is Giant || entity.y > 67) return@on

            val crate = CrateGiant(WeakReference(entity))
            giantEntities.add(crate)
            CrateSpawn(crate).post()
        }.setEnabled(Location.stateInArea("kuudra"))

        EventBus.on<RenderTickEvent> {
            if (giantEntities.isEmpty()) return@on

            giantEntities.removeIf { crate ->
                val despawn =
                    crate.entity.get() == null ||
                    crate.entity.get()!!.isDeadOrDying ||
                    crate.entity.get()!!.isRemoved

                if (despawn)
                    CrateDespawn(crate).post()

                despawn
            }
        }.setEnabled(Location.stateInArea("kuudra"))

        EventBus.on<WorldChangeEvent> {
            clear()
        }
    }

    fun supplies() = giantEntities

    // this will become state don't worry about it (:
    fun inCratePhase() = inSupplyPhase || inFuelPhase

    fun clear() {
        giantEntities.clear()
        inSupplyPhase = false
        inFuelPhase = false
    }
}