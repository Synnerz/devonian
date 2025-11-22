package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.barrl.Context
import com.github.synnerz.devonian.api.ItemUtils
import com.github.synnerz.devonian.api.dungeon.Dungeons
import com.github.synnerz.devonian.api.events.*
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.utils.math.MathUtils
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.phys.AABB
import java.awt.Color
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.PI

object BoxIcedMobs : Feature(
    "boxIcedMobs",
    "Box mobs that are ice sprayed",
    "Dungeons",
    "catacombs"
) {
    private val SETTING_WIRE_COLOR = addColorPicker(
        "wireColor",
        "",
        "Iced Mob Outline Color",
        Color(173, 216, 230, 255).rgb
    )
    private val SETTING_FILL_COLOR = addColorPicker(
        "fillColor",
        "",
        "Iced Mob Fill Color",
        Color(173, 188, 230, 80).rgb
    )
    private val SETTING_LINE_WIDTH = addSlider(
        "lineWidth",
        "",
        "Iced Mob Line Width",
        0.0, 10.0,
        3.0
    )

    private val itemCandidates = ConcurrentLinkedQueue<ItemCandidate>()
    private var lastHeldSpray = 0

    // lmao
    private val frozenMobs = linkedMapOf<FrozenMob, FrozenMob>()

    // pulled out of my ass
    private const val NEAR = 0.01
    private const val FAR = 8.0
    private const val FOV = 90.0 * PI / 180.0
    private const val ASPECT = 1.0

    private data class ItemCandidate(var ticks: Int, val id: Int)

    // firstFrozen in case I ever want to track when the mob starts moving
    private class FrozenMob(var ticks: Int, val ent: LivingEntity, var firstFrozen: Int) {
        override fun hashCode(): Int = ent.hashCode()
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as FrozenMob

            return ent == other.ent
        }
    }

    private fun isSpray(item: ItemStack?): Boolean {
        if (item == null) return false
        val id = ItemUtils.skyblockId(item) ?: return false
        return id == "ICE_SPRAY_WAND" || id == "STARRED_ICE_SPRAY_WAND"
    }

    override fun initialize() {
        on<TickEvent> {
            val world = minecraft.level ?: return@on
            val player = minecraft.player ?: return@on
            val held = player.mainHandItem
            val tick = EventBus.serverTicks()
            if (isSpray(held)) lastHeldSpray = tick

            while (frozenMobs.isNotEmpty()) {
                // ??
                val entry = frozenMobs.firstEntry() ?: break
                if (tick >= entry.value.ticks) frozenMobs.pollFirstEntry()
                else break
            }

            var foundIce = false

            var len = itemCandidates.size
            while (--len >= 0) {
                val c = itemCandidates.poll() ?: break
                val ent = world.getEntity(c.id) as? ItemEntity
                if (ent == null) {
                    if (--c.ticks >= 0) itemCandidates.offer(c)
                } else {
                    if (ent.item.item != Items.ICE) continue
                    foundIce = true
                    itemCandidates.clear()
                    break
                }
            }

            if (!foundIce) return@on

            val icers = Dungeons.players.values.mapNotNullTo(mutableListOf()) {
                if (isSpray(it.entity?.mainHandItem)) it.entity
                else null
            }
            if (icers.isEmpty() && tick - lastHeldSpray < 5) icers.add(player)

            // must have been the wind
            if (icers.isEmpty()) return@on

            icers.forEach { player ->
                val x = player.x
                val y = player.eyeY
                val z = player.z
                val look = player.lookAngle
                val hull = AABB(
                    x - FAR, y - FAR, z - FAR,
                    x + FAR, y + FAR, z + FAR,
                )
                val planes = arrayOf(
                    // left
                    MathUtils.rotate(look.x, look.y, look.z, (+FOV - PI) * 0.5, 0.0, 0.0)
                        .let { (nx, ny, nz) -> doubleArrayOf(nx, ny, nz, -(nx * x + ny * y + nz * z)) },
                    // right
                    MathUtils.rotate(look.x, look.y, look.z, (-FOV + PI) * 0.5, 0.0, 0.0)
                    .let { (nx, ny, nz) -> doubleArrayOf(nx, ny, nz, -(nx * x + ny * y + nz * z)) },
                    // bottom
                    MathUtils.rotate(look.x, look.y, look.z, 0.0, (+FOV * ASPECT - PI) * 0.5, 0.0)
                        .let { (nx, ny, nz) -> doubleArrayOf(nx, ny, nz, -(nx * x + ny * y + nz * z)) },
                    // top
                    MathUtils.rotate(look.x, look.y, look.z, 0.0, (-FOV * ASPECT + PI) * 0.5, 0.0)
                        .let { (nx, ny, nz) -> doubleArrayOf(nx, ny, nz, -(nx * x + ny * y + nz * z)) },
                    // near
                    doubleArrayOf(
                        look.x,
                        look.y,
                        look.z,
                        -(look.x * (x + look.x * NEAR) + look.y * (y + look.y * NEAR) + look.z * (z + look.z * NEAR))
                    ),
                    // far
                    doubleArrayOf(
                        -look.x,
                        -look.y,
                        -look.z,
                        look.x * (x + look.x * FAR) + look.y * (y + look.y * FAR) + look.z * (z + look.z * FAR)
                    ),
                )

                world.entitiesForRendering().forEach { ent ->
                    if (ent is ArmorStand) return@forEach
                    if (ent !is LivingEntity) return@forEach
                    val bb = ent.boundingBox
                    if (!bb.intersects(hull)) return@forEach

                    if (
                        planes.any { bb.minX * it[0] + bb.minY * it[1] + bb.minZ * it[2] + it[3] < 0.0 } &&
                        planes.any { bb.minX * it[0] + bb.minY * it[1] + bb.maxZ * it[2] + it[3] < 0.0 } &&
                        planes.any { bb.minX * it[0] + bb.maxY * it[1] + bb.minZ * it[2] + it[3] < 0.0 } &&
                        planes.any { bb.minX * it[0] + bb.maxY * it[1] + bb.maxZ * it[2] + it[3] < 0.0 } &&
                        planes.any { bb.maxX * it[0] + bb.minY * it[1] + bb.minZ * it[2] + it[3] < 0.0 } &&
                        planes.any { bb.maxX * it[0] + bb.minY * it[1] + bb.maxZ * it[2] + it[3] < 0.0 } &&
                        planes.any { bb.maxX * it[0] + bb.maxY * it[1] + bb.minZ * it[2] + it[3] < 0.0 } &&
                        planes.any { bb.maxX * it[0] + bb.maxY * it[1] + bb.maxZ * it[2] + it[3] < 0.0 }
                    ) return@forEach
                    if (Dungeons.players.any { it.value.entity == ent }) return@forEach

                    val data = FrozenMob(tick + 100, ent, tick)
                    frozenMobs.computeIfPresent(data) { k, _ ->
                        data.firstFrozen = k.firstFrozen
                        null
                    }
                    frozenMobs[data] = data
                }
            }
        }

        on<PacketReceivedEvent> { event ->
            val packet = event.packet as? ClientboundAddEntityPacket ?: return@on
            if (packet.type != EntityType.ITEM) return@on
            itemCandidates.offer(ItemCandidate(5, packet.id))
        }

        on<RenderWorldEvent> { event ->
            frozenMobs.forEach {
                val ent = it.value.ent
                if (ent.isDeadOrDying || ent.isRemoved) return@forEach

                val pos = ent.getPosition(event.ctx.tickCounter().getGameTimeDeltaPartialTick(false))
                val w = ent.bbWidth + 0.2
                val h = ent.bbHeight + 0.2
                Context.Immediate?.renderBox(
                    pos.x - w * 0.5,
                    pos.y,
                    pos.z - w * 0.5,
                    w,
                    h,
                    SETTING_WIRE_COLOR.getColor(),
                    translate = true,
                    lineWidth = SETTING_LINE_WIDTH.get()
                )
                Context.Immediate?.renderFilledBox(
                    pos.x - w * 0.5,
                    pos.y,
                    pos.z - w * 0.5,
                    w,
                    h,
                    SETTING_FILL_COLOR.getColor(),
                    translate = true
                )
            }
        }
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        itemCandidates.clear()
        lastHeldSpray = 0
        frozenMobs.clear()
    }
}