package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.barrl.Context
import com.github.synnerz.devonian.api.Scheduler
import com.github.synnerz.devonian.api.dungeon.DungeonClass
import com.github.synnerz.devonian.api.dungeon.Dungeons
import com.github.synnerz.devonian.api.events.ChatEvent
import com.github.synnerz.devonian.api.events.EntityEquipmentEvent
import com.github.synnerz.devonian.api.events.RenderWorldEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.hud.texthud.Alert
import net.minecraft.core.component.DataComponents
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.item.Items
import java.awt.Color
import java.util.UUID
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.jvm.optionals.getOrNull

object KeyPickup : Feature(
    "keyPickup",
    "",
    "Dungeons",
    "catacombs"
) {
    private val SETTING_KEY_WIRE_COLOR = addColorPicker(
        "wireColor",
        "",
        "Key Outline Color",
        Color(255, 0, 255, 255).rgb
    )
    private val SETTING_KEY_FILL_COLOR = addColorPicker(
        "fillColor",
        "",
        "Key Fill Color",
        Color(255, 0, 255, 64).rgb
    )
    private val SETTING_KEY_LINE_WIDTH = addSlider(
        "lineWidth",
        "",
        "Key Line Width",
        0.0, 10.0,
        3.0
    )
    private val SETTING_KEY_PICKUP_TITLE = addSwitch(
        "pickupTitle",
        "",
        "Key Pickup Title",
        true
    )
    private val SETTING_KEY_PICKUP_SOUND = addSwitch(
        "pickupSound",
        "",
        "Key Pickup Sound",
        true
    )
    private val SETTING_KEY_PICKUP_TIME = addSlider(
        "pickupTime",
        "The amount of time (in seconds) the title will be in screen",
        "Key Pickup Time",
        0.0, 10.0,
        1.0
    )

    private val pickupSound = SoundEvents.VAULT_OPEN_SHUTTER

    private val witherKeyRegex = "^.+?(\\w+) has obtained Wither Key!$".toRegex()
    private val bloodKeyRegex = "^.+?(\\w+) has obtained Blood Key!$".toRegex()

    private val witherKeyId = UUID.fromString("2865274b-3097-394e-8149-ec629c72d850")
    private val bloodKeyId = UUID.fromString("73f6d1f9-df41-3d1d-b98c-e1442d915885")

    // you never know :)
    private val keys = mutableListOf<ArmorStand>()
    private val idQ = ConcurrentLinkedQueue<Pair<Int, Int>>()

    private fun shortNameFor(name: String) =
        // TODO: handle nicks
        if (name == minecraft.gameProfile.name) "&bYou"
        else Dungeons.playerClasses[name].let {
        if (it == null || it == DungeonClass.Unknown) "&f$name"
        else "${it.colorCode}${it.shortName}"
    }

    override fun initialize() {
        on<ChatEvent> { event ->
            val title = when {
                event.message == "A Wither Key was picked up!" -> "Obtained Wither Key"
                event.message == "A Blood Key was picked up!" -> "Obtained Blood Key"

                witherKeyRegex.matches(event.message) ->
                    witherKeyRegex.matchEntire(event.message).let {
                        "${shortNameFor(it?.groupValues?.getOrNull(1) ?: "")} Picked Up\n&0Wither Key"
                    }

                bloodKeyRegex.matches(event.message) ->
                    bloodKeyRegex.matchEntire(event.message).let {
                        "${shortNameFor(it?.groupValues?.getOrNull(1) ?: "")} Picked Up\n&cBlood Key"
                    }

                else -> null
            } ?: return@on

            Scheduler.scheduleTask {
                if (SETTING_KEY_PICKUP_SOUND.get()) minecraft.player?.playSound(pickupSound, 2f, 1f)
                if (SETTING_KEY_PICKUP_TITLE.get()) Alert.show(title, SETTING_KEY_PICKUP_TIME.get().toInt() * 1000, playSound = false)
            }
        }
        on<EntityEquipmentEvent> { event ->
            if (event.type != EntityType.ARMOR_STAND) return@on
            if (event.slots.size != 1) return@on
            val entry = event.slots.firstOrNull() ?: return@on
            if (entry.first !== EquipmentSlot.HEAD) return@on

            val item = entry.second ?: return@on
            if (item.item !== Items.PLAYER_HEAD) return@on

            val profile = item.get(DataComponents.PROFILE) ?: return@on
            val id = profile.id.getOrNull() ?: return@on

            if (id != witherKeyId && id != bloodKeyId) return@on
            idQ.add(Pair(10, event.entityId))
        }
        on<RenderWorldEvent> { event ->
            val w = minecraft.level ?: return@on

            var len = idQ.size
            while (--len >= 0) {
                val p = idQ.poll() ?: break

                val ent = w.getEntity(p.second) as? ArmorStand
                if (ent == null) {
                    if (p.first > 0) idQ.offer(Pair(p.first - 1, p.second))
                } else keys.add(ent)
            }

            keys.removeIf { ent ->
                if (ent.isDeadOrDying || ent.isRemoved) return@removeIf true

                val pos = ent.getPosition(event.ctx.tickCounter().getGameTimeDeltaPartialTick(false))
                Context.Immediate?.renderBox(
                    pos.x - 0.5,
                    pos.y + 1.2,
                    pos.z - 0.5,
                    1.0,
                    1.0,
                    SETTING_KEY_WIRE_COLOR.getColor(),
                    phase = true,
                    translate = true,
                    // lineWidth = SETTING_KEY_LINE_WIDTH.get()
                )
                Context.Immediate?.renderFilledBox(
                    pos.x - 0.5,
                    pos.y + 1.2,
                    pos.z - 0.5,
                    1.0,
                    1.0,
                    SETTING_KEY_FILL_COLOR.getColor(),
                    phase = false,
                    translate = true
                )
                false
            }
        }
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        keys.clear()
        idQ.clear()
    }
}