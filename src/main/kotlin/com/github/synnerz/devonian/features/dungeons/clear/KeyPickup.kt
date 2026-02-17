package com.github.synnerz.devonian.features.dungeons.clear

import com.github.synnerz.devonian.api.ItemUtils
import com.github.synnerz.devonian.api.dungeon.DungeonClass
import com.github.synnerz.devonian.api.dungeon.Dungeons
import com.github.synnerz.devonian.api.dungeon.Stages
import com.github.synnerz.devonian.api.events.*
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.hud.texthud.Alert
import com.github.synnerz.devonian.utils.BasicState
import com.github.synnerz.devonian.utils.render.Render3DImmediate
import net.minecraft.core.component.DataComponents
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.item.Items
import java.awt.Color
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

object KeyPickup : Feature(
    "keyPickup",
    "Highlights wither/blood keys & alerts when they are picked up.",
    Categories.DUNGEONS,
    "catacombs",
    subcategory = "Highlights",
    searchTags = setOf("highlight", "alert", "dropped", "wither", "blood"),
    subcategories = listOf("Alerts"),
) {
    override fun createRequirements(): List<BasicState<Boolean>?> {
        return super.createRequirements() + listOf(Stages.Clear.isActiveState)
    }

    private val SETTING_KEY_WIRE_COLOR = addColorPicker(
        "wireColor",
        Color(255, 0, 255, 255).rgb,
        "",
        "Key Outline Color",
    )
    private val SETTING_KEY_FILL_COLOR = addColorPicker(
        "fillColor",
        Color(255, 0, 255, 64).rgb,
        "",
        "Key Fill Color",
    )
    private val SETTING_KEY_LINE_WIDTH = addSlider(
        "lineWidth",
        3.0,
        0.0, 10.0,
        "",
        "Key Line Width",
    )
    private val SETTING_KEY_PICKUP_TITLE = addSwitch(
        "pickupTitle",
        true,
        "Shows an alert whenever a key is picked up.",
        "Key Pickup Title",
        subcategory = "Alerts",
    )
    private val SETTING_KEY_PICKUP_SOUND = addSwitch(
        "pickupSound",
        true,
        "Plays the vault open sound whenever the key is picked up.",
        "Key Pickup Sound",
        subcategory = "Alerts",
    )
    private val SETTING_KEY_PICKUP_TIME = addSlider(
        "pickupTime",
        1.0,
        0.0, 10.0,
        "The amount of time (in seconds) the title will be in screen.",
        "Key Pickup Time",
        subcategory = "Alerts",
    )

    private val pickupSound = SoundEvents.VAULT_OPEN_SHUTTER

    private val witherKeyRegex = "^.*?(\\w+) has obtained Wither Key!$".toRegex()
    private val bloodKeyRegex = "^.*?(\\w+) has obtained Blood Key!$".toRegex()

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

            if (SETTING_KEY_PICKUP_SOUND.get()) minecraft.player?.playSound(pickupSound, 2f, 1f)
            if (SETTING_KEY_PICKUP_TITLE.get()) Alert.show(title, SETTING_KEY_PICKUP_TIME.get().toInt() * 1000, playSound = false)
        }
        on<EntityEquipmentEvent> { event ->
            if (event.type != EntityType.ARMOR_STAND) return@on
            if (event.slots.size != 1) return@on
            val entry = event.slots.firstOrNull() ?: return@on
            if (entry.first !== EquipmentSlot.HEAD) return@on

            val item = entry.second ?: return@on
            if (item.item !== Items.PLAYER_HEAD) return@on

            val profile = item.get(DataComponents.PROFILE) ?: return@on
            val id = profile.partialProfile().id ?: return@on

            if (
                id != witherKeyId &&
                (id != bloodKeyId || ItemUtils.skyblockId(item) != null)
            ) return@on
            idQ.add(Pair(10, event.entityId))
        }

        on<TickEvent> {
            val w = minecraft.level ?: return@on

            var len = idQ.size
            while (--len >= 0) {
                val p = idQ.poll() ?: break

                val ent = w.getEntity(p.second) as? ArmorStand
                if (ent == null) {
                    if (p.first > 0) idQ.offer(Pair(p.first - 1, p.second))
                } else keys.add(ent)
            }
        }

        on<RenderWorldEvent> {
            keys.removeIf { ent ->
                if (ent.isDeadOrDying || ent.isRemoved) return@removeIf true

                val pos = ent.getPosition(minecraft.deltaTracker.getGameTimeDeltaPartialTick(false))
                Render3DImmediate.renderWireframeBox(
                    pos.x,
                    pos.y + 1.2,
                    pos.z,
                    1.0, 1.0,
                    SETTING_KEY_WIRE_COLOR.getColor(),
                    phase = true,
                    lineWidth = SETTING_KEY_LINE_WIDTH.get(),
                    centered = true,
                )
                Render3DImmediate.renderFilledBox(
                    pos.x,
                    pos.y + 1.2,
                    pos.z,
                    1.0, 1.0,
                    SETTING_KEY_FILL_COLOR.getColor(),
                    phase = false,
                    centered = true,
                )
                return@removeIf false
            }
        }
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        keys.clear()
        idQ.clear()
    }
}