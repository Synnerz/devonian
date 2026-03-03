package com.github.synnerz.devonian.api

import com.github.synnerz.devonian.Devonian
import com.github.synnerz.devonian.api.events.EventBus
import com.github.synnerz.devonian.api.events.PacketReceivedEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.mixin.accessor.LocalPlayerAccessor
import com.github.synnerz.devonian.utils.BlockTypes
import com.github.synnerz.devonian.utils.math.DDA
import net.minecraft.client.multiplayer.ClientChunkCache
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.core.BlockPos
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.protocol.game.ClientboundForgetLevelChunkPacket
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.chunk.LevelChunk
import kotlin.math.abs
import kotlin.math.floor

object WorldUtils {
    val world: ClientLevel? get() = Devonian.minecraft.level
    val chunkManager: ClientChunkCache? get() = world?.chunkSource

    private val knownChunks = mutableSetOf<Int>()

    private fun zigzag(v: Int): Int =
        if (v >= 0) 2 * v
        else -2 * v - 1
    private fun szudzik(x: Int, z: Int): Int {
        val x = zigzag(x)
        val z = zigzag(z)
        return if(x >= z) x * x + x + z
            else z * z + x
    }

    fun initialize() {
        EventBus.on<PacketReceivedEvent> { event ->
            when (val packet = event.packet) {
                is ClientboundLevelChunkWithLightPacket -> {
                    Scheduler.scheduleAfterPacket {
                        knownChunks.add(szudzik(packet.x, packet.z))
                    }
                }
                is ClientboundForgetLevelChunkPacket -> {
                    Scheduler.scheduleAfterPacket {
                        knownChunks.remove(szudzik(packet.pos.x, packet.pos.z))
                    }
                }
            }
        }

        EventBus.on<WorldChangeEvent> {
            knownChunks.clear()
        }
    }

    fun isChunkLoaded(x: Double, z: Double): Boolean = isChunkLoaded(floor(x).toInt(), floor(z).toInt())

    fun isChunkLoaded(x: Int, z: Int): Boolean {
        val player = Devonian.minecraft.player as? LocalPlayerAccessor ?: return false
        val cx = x shr 4
        val cz = z shr 4
        val pcx = player.lastXClient.toInt() shr 4
        val pcz = player.lastZClient.toInt() shr 4
        return abs(cx - pcx) <= 7 && abs(cz - pcz) <= 7 &&
            knownChunks.contains(szudzik(cx, cz)) &&
            chunkManager?.getChunk(cx, cz, false)?.let { it.javaClass === LevelChunk::class.java } ?: false
    }

    fun fromBlockTypeOrNull(x: Double, y: Double, z: Double, blockType: Block): BlockState? =
        fromBlockTypeOrNull(floor(x).toInt(), floor(y).toInt(), floor(z).toInt(), blockType)

    fun fromBlockTypeOrNull(x: Int, y: Int, z: Int, blockType: Block): BlockState? {
        val blockState = getBlockState(x, y, z) ?: return null
        if (blockState.block != blockType) return null

        return blockState
    }

    fun getBlockState(x: Double, y: Double, z: Double): BlockState? =
        getBlockState(floor(x).toInt(), floor(y).toInt(), floor(z).toInt())

    fun getBlockState(x: Int, y: Int, z: Int): BlockState? =
        world?.getBlockState(BlockPos(x, y, z))

    fun getBlockId(block: Block): Int = BuiltInRegistries.BLOCK.indexOf(block)

    fun getBlockIdAt(x: Double, y: Double, z: Double): Int? {
        val blockState = getBlockState(x, y, z) ?: return null
        val block = blockState.block

        return getBlockId(block)
    }

    fun registryName(block: Block): String {
        val registry = BuiltInRegistries.BLOCK.getKey(block)
        return "${registry.namespace}:${registry.path}"
    }

    fun raycast(
        x: Double, y: Double, z: Double,
        dx: Double, dy: Double, dz: Double,
        firstBlock: Boolean,
    ): BlockPos? {
        val w = world ?: return null

        for (bp in DDA(x, y, z, dx, dy, dz)) {
            val bs = w.getBlockState(bp)
            if (firstBlock && !bs.isAir) return bp
            if (!BlockTypes.AirLike.contains(bs.block)) return bp
        }

        return null
    }
}
