package com.github.synnerz.devonian.utils.render.impl

import com.github.synnerz.devonian.utils.render.Render3DTypes
import net.minecraft.client.renderer.rendertype.RenderType


data class BatchedRenderType(val name: String, val type: RenderType, val batchId: Int, val phase: Boolean) {
    enum class Primitive(val types: Array<RenderType>) {
        LINES(
            arrayOf(
                Render3DTypes.LINES_TRANSLUCENT,
                Render3DTypes.LINES_TRANSLUCENT_ESP,
                Render3DTypes.LINES_OPAQUE,
                Render3DTypes.LINES_OPAQUE_ESP,
            )
        ),
        TRIS(
            arrayOf(
                Render3DTypes.TRIANGLES_TRANSLUCENT,
                Render3DTypes.TRIANGLES_TRANSLUCENT_ESP,
                Render3DTypes.TRIANGLES_OPAQUE,
                Render3DTypes.TRIANGLES_OPAQUE_ESP,
            )
        ),
        QUADS(
            arrayOf(
                Render3DTypes.QUADS_TRANSLUCENT,
                Render3DTypes.QUADS_TRANSLUCENT_ESP,
                Render3DTypes.QUADS_OPAQUE,
                Render3DTypes.QUADS_OPAQUE_ESP,
            )
        ),
        BEACON(
            arrayOf(
                Render3DTypes.BEACON_BEAM_TRANSLUCENT,
                Render3DTypes.BEACON_BEAM_TRANSLUCENT_ESP,
                Render3DTypes.BEACON_BEAM_OPAQUE,
                Render3DTypes.BEACON_BEAM_OPAQUE_ESP,
            )
        );
    }

    companion object {
        fun get(opaque: Boolean, phase: Boolean, primitive: Primitive): BatchedRenderType {
            val type = primitive.types[(if (opaque) 2 else 0) + (if (phase) 1 else 0)]

            return BatchedRenderType(
                "${primitive.name} ${if (opaque) "O" else "t"}${if (phase) "P" else "p"}",
                type,
                (if (opaque) 0 else 1) + (primitive.ordinal shl 1),
                phase,
            )
        }

        val MAX_ID = 1 or ((Primitive.entries.size - 1) shl 1)
    }
}