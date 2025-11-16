package com.github.synnerz.devonian.features.dungeons.map

import com.github.synnerz.devonian.api.bufimgrenderer.BufferedImageRenderer
import com.github.synnerz.devonian.api.dungeon.DungeonRoom
import com.github.synnerz.devonian.api.dungeon.mapEnums.*
import com.github.synnerz.devonian.hud.texthud.FontListener
import com.github.synnerz.devonian.hud.texthud.StringParser
import com.github.synnerz.devonian.hud.texthud.TextHud
import com.github.synnerz.devonian.utils.BoundingBox
import com.github.synnerz.devonian.utils.TextRendererImpl
import net.minecraft.util.TriState
import java.awt.Color
import java.awt.Font
import java.awt.image.BufferedImage
import java.util.*
import javax.imageio.ImageIO
import kotlin.math.abs

class DungeonMapBaseRenderer :
    BufferedImageRenderer<DungeonMapRenderData>("dungeonMapBaseRenderer", TriState.FALSE), FontListener {
    val cachedStrings = Collections.synchronizedMap(
        object : LinkedHashMap<String, CachedRenderedString>(30) {
            // max 36 rooms + secret counts, then round down to funny number for funny
            override fun removeEldestEntry(eldest: Map.Entry<String?, CachedRenderedString?>?): Boolean = size > 69
        }
    )
    var cachedW = 0
    var cachedH = 0

    data class CachedRenderedString(
        val img: BufferedImage,
        val xo: Double, val yo: Double,
        val wf: Double, val hf: Double,
        val w: Double, val h: Double
    )

    override fun onFontChange(f: Font) {
        cachedStrings.clear()
    }

    override fun drawImage(img: BufferedImage, param: DungeonMapRenderData): BufferedImage {
        val g = img.createGraphics()

        val w = img.width
        val h = img.height

        val rooms = param.rooms.toSet()
        val doors = param.doors
        val options = param.options
        val colors = options.colors

        fun colorForRoom(room: DungeonRoom) = when (room.type) {
            RoomTypes.ENTRANCE -> colors[DungeonMapColors.RoomEntrance]
            RoomTypes.NORMAL -> when (room.clear) {
                ClearTypes.MOB,
                ClearTypes.OTHER
                    -> colors[DungeonMapColors.RoomNormal]

                ClearTypes.MINIBOSS -> colors[DungeonMapColors.RoomMiniboss]
            }

            RoomTypes.FAIRY -> colors[DungeonMapColors.RoomFairy]
            RoomTypes.BLOOD -> colors[DungeonMapColors.RoomBlood]
            RoomTypes.PUZZLE -> colors[DungeonMapColors.RoomPuzzle]
            RoomTypes.TRAP -> colors[DungeonMapColors.RoomTrap]
            RoomTypes.YELLOW -> colors[DungeonMapColors.RoomYellow]
            RoomTypes.RARE -> colors[DungeonMapColors.RoomRare]
            RoomTypes.UNKNOWN -> colors[DungeonMapColors.RoomUnknown]
        }

        val roomRectOffset = (1.0 - options.roomWidth) * 0.5
        val compToBImgF = 1.0 / options.dungeonSize * w

        fun drawRoom(x: Int, z: Int, w: Int, h: Int) {
            val cx = x + roomRectOffset
            val cz = z + roomRectOffset
            val cw = options.roomWidth + w - 1
            val ch = options.roomWidth + h - 1
            val bx = (compToBImgF * cx + 0.5).toInt()
            val bz = (compToBImgF * cz + 0.5).toInt()
            val bw = (compToBImgF * cw + 0.5).toInt()
            val bh = (compToBImgF * ch + 0.5).toInt()

            g.fillRect(bx, bz, bw, bh)
        }

        fun drawRoomJoined(cx1: Int, cz1: Int, cx2: Int, cz2: Int) {
            if (cx1 > cx2 || cz1 > cz2) return drawRoomJoined(cx2, cz2, cx1, cz1)
            val cx: Double
            val cz: Double
            val cw: Double
            val ch: Double
            if (cx1 == cx2) {
                cx = cx1 + roomRectOffset
                cz = cz1 + roomRectOffset + options.roomWidth
                cw = options.roomWidth
                ch = roomRectOffset * 2.0
            } else {
                cx = cx1 + roomRectOffset + options.roomWidth
                cz = cz1 + roomRectOffset
                cw = roomRectOffset * 2.0
                ch = options.roomWidth
            }
            val bx = (compToBImgF * cx + 0.5).toInt()
            val bz = (compToBImgF * cz + 0.5).toInt()
            val bw = (compToBImgF * cw + 0.5).toInt()
            val bh = (compToBImgF * ch + 0.5).toInt()

            g.fillRect(bx, bz, bw, bh)
        }

        if ((colors[DungeonMapColors.Background]?.alpha ?: 0) > 0) {
            g.paint = colors[DungeonMapColors.Background]
            g.fillRect(0, 0, w, h)
        }

        rooms.forEach { room ->
            var color = colorForRoom(room) ?: colors[DungeonMapColors.RoomNormal] ?: Color(0)
            var shape = room.shape
            if (shape == ShapeTypes.Unknown) return@forEach
            var cells = room.comps.toList()
            var renderRoomInfo = true

            if (!room.explored) {
                if (options.renderUnknownRooms) {
                    if (options.dungeonStarted) color = Color(
                        (color.red * options.unknownRoomsDarkenFactor + 0.5).toInt(),
                        (color.green * options.unknownRoomsDarkenFactor + 0.5).toInt(),
                        (color.blue * options.unknownRoomsDarkenFactor + 0.5).toInt(),
                        color.alpha
                    )
                } else {
                    shape = ShapeTypes.Shape1x1
                    cells = room.comps.filter {
                        val cx = it[2] * 2
                        val cz = it[3] * 2
                        room.doors.any {
                            abs(cx - it.comps[2]) + abs(cz - it.comps[3]) == 1.0 &&
                            it.rooms.any { it.explored }
                        }
                    }
                    renderRoomInfo = false
                }
            }

            if (cells.isEmpty()) return@forEach

            if (shape == ShapeTypes.Shape2x2 && cells.size != 4) shape = ShapeTypes.ShapeL
            if (shape == ShapeTypes.ShapeL && cells.size != 3) shape = when (cells.size) {
                1 -> ShapeTypes.Shape1x1
                2 -> ShapeTypes.Shape1x2
                else ->
                    // something went terribly wrong
                    ShapeTypes.ShapeL
            }

            g.paint = color
            when (shape) {
                ShapeTypes.Unknown -> return@forEach
                ShapeTypes.ShapeL -> {
                    for (i in cells.indices) {
                        val cx = cells[i][2].toInt()
                        val cz = cells[i][3].toInt()
                        drawRoom(cx, cz, 1, 1)
                        for (j in i until cells.size) {
                            val cx2 = cells[j][2].toInt()
                            val cz2 = cells[j][3].toInt()
                            if (abs(cx - cx2) + abs(cz - cz2) == 1) drawRoomJoined(cx, cz, cx2, cz2)
                        }
                    }
                }

                ShapeTypes.Shape1x1 -> drawRoom(cells[0][2].toInt(), cells[0][3].toInt(), 1, 1)
                ShapeTypes.Shape1x2,
                ShapeTypes.Shape1x3,
                ShapeTypes.Shape1x4
                    -> {
                    val roomCorner = cells.minBy { it[2] + it[3] }
                    val cx1 = cells[0][2].toInt()
                    val cx2 = cells[1][2].toInt()
                    drawRoom(
                        roomCorner[2].toInt(), roomCorner[3].toInt(),
                        if (cx1 == cx2) 1 else cells.size,
                        if (cx1 == cx2) cells.size else 1
                    )
                }

                ShapeTypes.Shape2x2 -> {
                    val roomCorner = cells.minBy { it[2] + it[3] }
                    drawRoom(roomCorner[2].toInt(), roomCorner[3].toInt(), 2, 2)
                }
            }

            if (!renderRoomInfo) return@forEach

            val decoration =
                (if (options.checkMark) CHECKMARK[room.checkmark] else null) ?:
                (if (options.puzzleIcon) SPECIAL_ROOMS[room.name] else null)
            val text = mutableListOf<String>()

            if (
                options.roomName &&
                (options.puzzleName || room.type != RoomTypes.PUZZLE)
            ) room.name?.also { name ->
                if (options.colorRoomName) {
                    val colorCode = when (room.checkmark) {
                        CheckmarkTypes.FAILED -> "&c"
                        CheckmarkTypes.GREEN -> "&a"
                        CheckmarkTypes.WHITE,
                        CheckmarkTypes.UNEXPLORED -> "&f"
                        CheckmarkTypes.NONE -> "&7"
                    }
                    text.add("$colorCode$name")
                } else text.add(name)
            }
            if (options.secretCount && room.totalSecrets > 0) {
                val colorCode = when (room.checkmark) {
                    CheckmarkTypes.GREEN -> "&a"
                    CheckmarkTypes.FAILED,
                    CheckmarkTypes.NONE,
                    CheckmarkTypes.WHITE,
                    CheckmarkTypes.UNEXPLORED -> "&f"
                }
                text.add("$colorCode${room.secretsCompleted}/${room.totalSecrets}")
            }

            if (decoration == null && text.isEmpty()) return@forEach

            val center = when (options.roomInfoAlignment) {
                DungeonMapRoomInfoAlignment.TopLeft
                    -> cells.minBy { +it[2] + it[3] }.let { Pair(it[2] + 0.5, it[3] + 0.5) }
                DungeonMapRoomInfoAlignment.TopRight
                    -> cells.minBy { -it[2] + it[3] }.let { Pair(it[2] + 0.5, it[3] + 0.5) }
                DungeonMapRoomInfoAlignment.BottomLeft
                    -> cells.minBy { +it[2] - it[3] }.let { Pair(it[2] + 0.5, it[3] + 0.5) }
                DungeonMapRoomInfoAlignment.BottomRight
                    -> cells.minBy { -it[2] - it[3] }.let { Pair(it[2] + 0.5, it[3] + 0.5) }
                DungeonMapRoomInfoAlignment.Center -> {
                    if (shape == ShapeTypes.ShapeL) {
                        val sorted = cells.sortedBy { it[2] + it[3] * 10.0 }
                        val idx =
                            if (sorted[0][2] < sorted[1][2]) 2
                            else if (sorted[0][2] == sorted[2][2]) 0
                            else 1
                        Pair(sorted[idx][2] + 0.5, sorted[idx][3] + 0.5)
                    } else Pair(
                        cells.sumOf { it[2] } / cells.size + 0.5,
                        cells.sumOf { it[3] } / cells.size + 0.5
                    )
                }
            }

            val decBox = BoundingBox(
                center.first - 0.4,
                center.second - 0.4,
                0.8, 0.8
            )
            val bx = (decBox.x * compToBImgF + 0.5).toInt()
            val by = (decBox.x * compToBImgF + 0.5).toInt()
            val bw = (decBox.w * compToBImgF + 0.5).toInt()
            val bh = (decBox.h * compToBImgF + 0.5).toInt()
            if (bw != cachedW || bh != cachedH) {
                cachedStrings.clear()
                cachedW = bw
                cachedH = bh
            }

            g.paint = Color(-1)
            if (decoration != null) g.drawImage(decoration, bx, by, bw, bh, null)

            text.forEach { str ->
                val rendered = cachedStrings.getOrPut(str) {
                    var fontSize = TextHud.MC_FONT_SIZE
                    var font = TextHud.fontMainBase.deriveFont(Font.PLAIN, fontSize)
                    g.font = font

                    var line = StringParser.processString(str, options.stringShadow, g, font, font, font, fontSize)
                    val (scale, _) = BoundingBox(0.0, 0.0, line.visualWidth.toDouble(), fontSize.toDouble())

                    fontSize *= scale.toFloat()
                    font = TextHud.fontMainBase.deriveFont(Font.PLAIN, fontSize)
                    g.font = font
                    line = StringParser.processString(str, options.stringShadow, g, font, font, font, fontSize)

                    val w = line.visualWidth + (if (options.stringShadow) 0.1 * fontSize else 0.0) + 5.0
                    val h = (fontSize + g.fontMetrics.ascent).toDouble()
                    val img = bimgProvider.create(w.toInt(), h.toInt())

                    TextRendererImpl.drawImage(img, TextRendererImpl.TextRenderParams(
                        TextHud.Align.Left,
                        options.stringShadow,
                        TextHud.Backdrop.None,
                        fontSize,
                        font,
                        listOf(line),
                        line.visualWidth
                    ))

                    CachedRenderedString(
                        img,
                        0.0, 0.0,
                        w * line.visualWidth,
                        h * fontSize,
                        line.visualWidth.toDouble(), fontSize.toDouble()
                    )
                }

                val (scale, box) = BoundingBox(
                    0.0, 0.0,
                    rendered.w, rendered.h
                ).fitInside(decBox)
                if (abs(1.0 - scale) > 0.1) cachedStrings.remove(str)

                g.drawImage(
                    rendered.img,
                    (box.x + rendered.xo + 0.5).toInt(),
                    (box.y + rendered.yo + 0.5).toInt(),
                    (box.w * rendered.wf + 0.5).toInt(),
                    (box.h * rendered.hf + 0.5).toInt(),
                    null
                )
            }
        }

        val doorOffset = (1.0 - options.doorWidth) * 0.5

        fun drawDoor(cx1: Int, cz1: Int, cx2: Int, cz2: Int) {
            if (cx1 > cx2 || cz1 > cz2) return drawDoor(cx2, cz2, cx1, cz1)
            val cx: Double
            val cz: Double
            val cw: Double
            val ch: Double
            if (cx1 == cx2) {
                cx = cx1 + doorOffset
                cz = cz1 + roomRectOffset + options.roomWidth
                cw = options.doorWidth
                ch = roomRectOffset * 2.0
            } else {
                cx = cx1 + roomRectOffset + options.roomWidth
                cz = cz1 + doorOffset
                cw = roomRectOffset * 2.0
                ch = options.doorWidth
            }
            val bx = (compToBImgF * cx + 0.5).toInt()
            val bz = (compToBImgF * cz + 0.5).toInt()
            val bw = (compToBImgF * cw + 0.5).toInt()
            val bh = (compToBImgF * ch + 0.5).toInt()

            g.fillRect(bx, bz, bw, bh)
        }

        doors.forEach { door ->
            val color = when (door.type) {
                DoorTypes.ENTRANCE -> colors[DungeonMapColors.DoorEntrance]
                DoorTypes.WITHER -> colors[DungeonMapColors.DoorWither]
                DoorTypes.BLOOD -> colors[DungeonMapColors.DoorBlood]
                DoorTypes.NORMAL -> {
                    val room = door.rooms.minByOrNull { it.type.prio } ?: return@forEach
                    colorForRoom(room)
                }
            } ?: colors[DungeonMapColors.RoomNormal] ?: return@forEach

            g.paint = color
            drawDoor(
                door.roomComp1.first,
                door.roomComp1.second,
                door.roomComp2.first,
                door.roomComp2.second
            )
        }

        g.dispose()
        return img
    }

    companion object {
        val CHECKMARK = mapOf(
            CheckmarkTypes.NONE to null,
            CheckmarkTypes.WHITE to getImg("whiteCheck.png"),
            CheckmarkTypes.GREEN to getImg("greenCheck.png"),
            CheckmarkTypes.FAILED to getImg("failedRoom.png"),
            CheckmarkTypes.UNEXPLORED to getImg("questionMark.png"),
        )
        val SPECIAL_ROOMS = mapOf(
            "Creeper Beams" to getImg("creeper.png"),
            "Three Weirdos" to getImg("chest.png"),
            "Tic Tac Toe" to getImg("shears.png"),
            "Water Board" to getImg("bucket_water.png"),
            "Teleport Maze" to getImg("endframe_side.png"),
            "Blaze" to getImg("blaze_powder.png"),
            "Boulder" to getImg("planks_oak.png"),
            "Ice Fill" to getImg("ice.png"),
            "Ice Path" to getImg("spawner.png"),
            "Quiz" to getImg("book_normal.png"),
        )

        private fun getImg(path: String): BufferedImage? {
            val stream = this::class.java.getResourceAsStream("/assets/devonian/dungeons/map/$path") ?: return null
            val img = ImageIO.read(stream)
            return img
        }
    }
}