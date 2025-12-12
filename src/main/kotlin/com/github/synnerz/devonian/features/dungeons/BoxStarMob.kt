package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.barrl.Context
import com.github.synnerz.devonian.api.events.*
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import java.awt.Color
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

object BoxStarMob : Feature(
    "boxStarMob",
    "Renders a box surrounding the star mobs in dungeons to complete a room.",
    Categories.DUNGEONS,
    "catacombs",
    subcategory = "World",
) {
    private val SETTING_MOB_COLOR = addColorPicker(
        "mobColor",
        Color(0, 255, 255, 255).rgb,
        "",
        "Starred Mob Color",
    )
    private val SETTING_CHONK_COLOR = addColorPicker(
        "chonkColor",
        Color(255, 0, 128, 255).rgb,
        "withermancers, commanders, lords, and super archers",
        "Starred Chunky Mob Color",
    )
    private val SETTING_FEL_COLOR = addColorPicker(
        "felColor",
        Color(0, 255, 128, 255).rgb,
        "",
        "Starred Fels Color",
    )
    private val SETTING_MINI_COLOR = addColorPicker(
        "miniColor",
        Color(235, 1, 165, 255).rgb,
        "due to the way the detection works (so it's faster) it actually doesn't check if it's starred",
        "Miniboss Color",
    )
    private val SETTING_SA_COLOR = addColorPicker(
        "saColor",
        Color(255, 0, 0, 255).rgb,
        "see mini color",
        "Shadow Assassin Color",
    )
    private val SETTING_SM_COLOR = addColorPicker(
        "smColor",
        Color(255, 128, 0, 255).rgb,
        "",
        "Skeleton Master Color",
    )
    private val SETTING_LINE_WIDTH = addSlider(
        "lineWidth",
        3.0,
        0.0, 10.0,
        "",
        "Starred Mobs Line Width",
    )
    private val SETTING_PHASE = addSwitch(
        "phase",
        false,
        "",
        "Starred Mobs Esp",
        cheeto = true,
        isHidden = true,
    )

    private val starred = mutableListOf<Pair<LivingEntity, MobData>>()
    private val starredIdQ = ConcurrentLinkedQueue<Pair<Int, MobData>>()
    private val playerMobMap = mutableMapOf<UUID, MobData>()
    private var lastStand: Int = 0

    private fun getMobDataFromArmorStand(name: String): MobData {
        if (name.contains("Shadow Assassin")) return MobData(2.0, SETTING_SA_COLOR.getColor())

        if (name.contains("Fels")) return MobData(3.0, SETTING_FEL_COLOR.getColor())

        if (name.contains("Skeleton Master")) return MobData(2.0, SETTING_SM_COLOR.getColor())

        if (name.contains("Withermancer")) return MobData(3.0, SETTING_CHONK_COLOR.getColor())
        if (
        // Zombie Lord, Skeleton Lord
            name.contains("Lord") ||
            name.contains("Zombie Commander") ||
            name.contains("Super Archer")
        ) return MobData(2.0, SETTING_CHONK_COLOR.getColor())

        if (
        // Lost Adventurer, Frozen Adventurer
            name.contains("Adventurer") ||
            name.contains("Angry Archaeologist") ||
            name.contains("King Midas")
        ) return MobData(2.0, SETTING_MINI_COLOR.getColor())

        return MobData(2.0, SETTING_MOB_COLOR.getColor())
    }

    private fun getMobDataFromName(name: String): MobData? = when (name) {
        "Shadow Assassin" -> MobData(2.0, SETTING_SA_COLOR.getColor())
        "Lost Adventurer",
        "Diamond Guy",
        "King Midas"
            -> MobData(2.0, SETTING_MINI_COLOR.getColor())

        else -> null
    }

    private data class MobData(val height: Double, val color: Color)

    override fun initialize() {
        on<NameChangeEvent> { event ->
            if (event.entityId != lastStand) return@on
            lastStand = 0

            if (!event.name.contains('✯')) return@on

            starredIdQ.add(
                Pair(
                    event.entityId - (if (event.name.contains("Withermancer")) 3 else 1),
                    getMobDataFromArmorStand(event.name)
                )
            )
        }
        on<PacketReceivedEvent> { event ->
            when (val packet = event.packet) {
                is ClientboundAddEntityPacket -> {
                    when (packet.type) {
                        EntityType.ARMOR_STAND -> {
                            lastStand = packet.id
                        }

                        EntityType.PLAYER -> {
                            val uuid = packet.uuid ?: return@on
                            val data = playerMobMap[uuid] ?: return@on
                            starredIdQ.add(Pair(packet.id, data))
                        }
                    }
                }

                is ClientboundPlayerInfoUpdatePacket -> {
                    if (!packet.actions().contains(ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER)) return@on

                    val entry = packet.entries().getOrNull(0) ?: return@on
                    val name = entry.profile?.name ?: return@on

                    val data = getMobDataFromName(name) ?: return@on
                    playerMobMap[entry.profileId] = data
                }
            }
        }

        on<TickEvent> {
            val w = minecraft.level ?: return@on

            var len = starredIdQ.size
            while (--len >= 0) {
                val p = starredIdQ.poll() ?: break
                val (id, data) = p

                val ent = w.getEntity(id) as? LivingEntity
                if (ent == null) starredIdQ.offer(p)
                else starred.add(Pair(ent, data))
            }
        }

        on<RenderWorldEvent> {
            starred.removeIf { (ent, data) ->
                if (ent.isDeadOrDying || ent.isRemoved) return@removeIf true

                val pos = ent.getPosition(minecraft.deltaTracker.getGameTimeDeltaPartialTick(false))
                Context.Immediate?.renderBox(
                    pos.x - 0.4,
                    pos.y,
                    pos.z - 0.4,
                    0.8,
                    data.height,
                    data.color,
                    phase = SETTING_PHASE.get(),
                    translate = true,
                    lineWidth = SETTING_LINE_WIDTH.get()
                )
                false
            }
        }
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        starred.clear()
        starredIdQ.clear()
        playerMobMap.clear()
        lastStand = 0
    }
}