package com.github.synnerz.devonian.features.dungeons.map

import com.github.synnerz.devonian.api.bufimgrenderer.BufferedImageRenderer
import com.github.synnerz.devonian.api.dungeon.DungeonRoom
import com.github.synnerz.devonian.api.dungeon.WorldComponentPosition
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
import kotlin.math.ceil

class DungeonMapBaseRenderer :
    BufferedImageRenderer<DungeonMapRenderData>("dungeonMapBaseRenderer", TriState.FALSE), FontListener {
    val cachedStrings = Collections.synchronizedMap(
        object : LinkedHashMap<CachedStringKey, CachedRenderedString>(30) {
            override fun removeEldestEntry(eldest: Map.Entry<CachedStringKey?, CachedRenderedString?>?): Boolean = size > 36
        }
    )
    var cachedW = 0
    var cachedH = 0

    data class CachedStringKey(val str: String, val size: Int)

    data class CachedRenderedString(
        val img: BufferedImage,
        val xo: Double, val yo: Double,
        val wr: Double, val hr: Double,
        val w: Double, val h: Double
    )

    override fun onFontChange(f: Font) {
        cachedStrings.clear()
    }

    data class TextRenderParam(val box: BoundingBox, val key: CachedStringKey, val text: List<String>)

    override fun drawImage(img: BufferedImage, param: DungeonMapRenderData): BufferedImage {
        val g = img.createGraphics()

        val w = img.width
        val h = img.height

        val rooms = param.rooms.toSet()
        val doors = param.doors
        val options = param.options
        val colors = options.colors

        fun colorForRoom(room: DungeonRoom): Color? {
            var col = if (!options.renderUnknownRooms && !room.explored) colors[DungeonMapColors.RoomUnknown]
            else when (room.type) {
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

            if (col != null && options.dungeonStarted && !room.explored) col = Color(
                (col.red * options.unknownRoomsDarkenFactor + 0.5).toInt(),
                (col.green * options.unknownRoomsDarkenFactor + 0.5).toInt(),
                (col.blue * options.unknownRoomsDarkenFactor + 0.5).toInt(),
                col.alpha
            )

            return col
        }

        val roomRectOffset = (1.0 - options.roomWidth) * 0.5
        val compToBImgF = 1.0 / options.dungeonSize * w

        fun drawRoom(x: Int, z: Int, w: Int, h: Int) {
            val cx = x + roomRectOffset
            val cz = z + roomRectOffset
            val cw = options.roomWidth + w - 1
            val ch = options.roomWidth + h - 1
            val bx = (compToBImgF * cx).toInt()
            val bz = (compToBImgF * cz).toInt()
            val bw = ceil(compToBImgF * cw).toInt()
            val bh = ceil(compToBImgF * ch).toInt()

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
            val bx = (compToBImgF * cx).toInt()
            val bz = (compToBImgF * cz).toInt()
            val bw = ceil(compToBImgF * cw).toInt()
            val bh = ceil(compToBImgF * ch).toInt()

            g.fillRect(bx, bz, bw, bh)
        }

        fun getCenterOf(
            cells: List<WorldComponentPosition>,
            shape: ShapeTypes,
            alignment: DungeonMapRoomInfoAlignment
        ) = when (alignment) {
            DungeonMapRoomInfoAlignment.TopLeft
                -> cells.minBy { +it.cx + it.cz }.let { Pair(it.cx / 2 + 0.5, it.cz / 2 + 0.5) }
            DungeonMapRoomInfoAlignment.TopRight
                -> cells.minBy { -it.cx + it.cz }.let { Pair(it.cx / 2 + 0.5, it.cz / 2 + 0.5) }
            DungeonMapRoomInfoAlignment.BottomLeft
                -> cells.minBy { +it.cx - it.cz }.let { Pair(it.cx / 2 + 0.5, it.cz / 2 + 0.5) }
            DungeonMapRoomInfoAlignment.BottomRight
                -> cells.minBy { -it.cx - it.cz }.let { Pair(it.cx / 2 + 0.5, it.cz / 2 + 0.5) }
            DungeonMapRoomInfoAlignment.Center -> {
                if (shape == ShapeTypes.ShapeL) {
                    val sorted = cells.sortedBy { it.cx + it.cz * 11 }
                    val idx =
                        if (sorted[0].cx > sorted[1].cx) 2
                        else if (sorted[0].cx == sorted[2].cx) 0
                        else 1
                    Pair(sorted[idx].cx / 2 + 0.5, sorted[idx].cz / 2 + 0.5)
                } else Pair(
                    cells.sumOf { it.cx / 2.0 } / cells.size + 0.5,
                    cells.sumOf { it.cz / 2.0 } / cells.size + 0.5
                )
            }
        }

        if ((colors[DungeonMapColors.Background]?.alpha ?: 0) > 0) {
            g.paint = colors[DungeonMapColors.Background]
            g.fillRect(0, 0, w, h)
        }

        val textToRender = mutableListOf<TextRenderParam>()
        rooms.forEach { room ->
            if (room == null) return@forEach
            if (room.doors.isEmpty() && room.name == null) return@forEach
            val color = colorForRoom(room) ?: colors[DungeonMapColors.RoomNormal] ?: Color(0)
            var shape = room.shape
            if (shape == ShapeTypes.Unknown) return@forEach
            var cells = room.comps.toList()
            var renderRoomInfo = true

            if (!room.explored && !options.renderUnknownRooms) {
                shape = ShapeTypes.Shape1x1
                cells = room.comps.filter {
                    val cx = it.cx
                    val cz = it.cz
                    room.doors.any {
                        abs(cx - it.comp.cx) + abs(cz - it.comp.cz) == 1 &&
                        it.rooms.any { it.explored }
                    }
                }
                renderRoomInfo = false
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
                        val cx = cells[i].cx / 2
                        val cz = cells[i].cz / 2
                        drawRoom(cx, cz, 1, 1)
                        for (j in i until cells.size) {
                            val cx2 = cells[j].cx / 2
                            val cz2 = cells[j].cz / 2
                            if (abs(cx - cx2) + abs(cz - cz2) == 1) drawRoomJoined(cx, cz, cx2, cz2)
                        }
                    }
                }

                ShapeTypes.Shape1x1 -> drawRoom(cells[0].cx / 2, cells[0].cz / 2, 1, 1)
                ShapeTypes.Shape1x2,
                ShapeTypes.Shape1x3,
                ShapeTypes.Shape1x4
                    -> {
                    val roomCorner = cells.minBy { it.cx + it.cz }
                    val cx1 = cells[0].cx
                    val cx2 = cells[1].cx
                    drawRoom(
                        roomCorner.cx / 2, roomCorner.cz / 2,
                        if (cx1 == cx2) 1 else cells.size,
                        if (cx1 == cx2) cells.size else 1
                    )
                }

                ShapeTypes.Shape2x2 -> {
                    val roomCorner = cells.minBy { it.cx + it.cz }
                    drawRoom(roomCorner.cx / 2, roomCorner.cz / 2, 2, 2)
                }
            }

            val decoration =
                (if (options.checkMark) {
                    if (options.renderUnknownRooms && room.checkmark == CheckmarkTypes.UNEXPLORED) null
                    else CHECKMARK[room.checkmark]
                } else null) ?:
                (if (renderRoomInfo && options.puzzleIcon) SPECIAL_ROOMS[room.name] else null)
            val text = mutableListOf<String>()

            if (
                renderRoomInfo &&
                options.roomName &&
                (options.puzzleName || room.type != RoomTypes.PUZZLE)
            ) room.name?.also { name ->
                if (options.colorRoomName) {
                    val colorCode = when (room.type) {
                        RoomTypes.ENTRANCE,
                        RoomTypes.FAIRY,
                        RoomTypes.BLOOD
                            -> "&f"

                        else -> when (room.checkmark) {
                            CheckmarkTypes.FAILED -> "&c"
                            CheckmarkTypes.GREEN -> "&a"
                            CheckmarkTypes.WHITE,
                            CheckmarkTypes.UNEXPLORED -> "&f"

                            CheckmarkTypes.NONE -> "&7"
                        }
                    }
                    name.replace("\u200B", "- ").split(" ").forEach { text.add("$colorCode$it")}
                } else text.add(name)
            }
            if (renderRoomInfo && options.secretCount && room.totalSecrets > 0) {
                val colorCode = when (room.checkmark) {
                    CheckmarkTypes.FAILED -> "&c"
                    CheckmarkTypes.GREEN -> "&a"
                    CheckmarkTypes.WHITE,
                    CheckmarkTypes.UNEXPLORED -> "&f"
                    CheckmarkTypes.NONE -> "&7"
                }
                text.add("$colorCode${if (room.checkmark == CheckmarkTypes.GREEN) room.totalSecrets else room.secretsCompleted}/${room.totalSecrets}")
            }

            if (decoration == null && text.isEmpty()) return@forEach

            if (decoration != null) {
                val decW = options.iconSize * options.roomWidth
                val center = getCenterOf(cells, shape, options.iconAlignment)
                val decBox = BoundingBox(
                    center.first - decW * 0.5,
                    center.second - decW * 0.5,
                    decW, decW
                )
                val bx = (decBox.x * compToBImgF).toInt()
                val by = (decBox.y * compToBImgF).toInt()
                val bw = ceil(decBox.w * compToBImgF).toInt()
                val bh = ceil(decBox.h * compToBImgF).toInt()
                g.drawImage(decoration, bx, by, bw, bh, null)
            }

            if (text.isNotEmpty()) {
                val decW = options.textSize * options.roomWidth
                val center = getCenterOf(cells, shape, options.textAlignment)
                val decBox = BoundingBox(
                    center.first - decW * 0.5,
                    center.second - decW * 0.5,
                    decW, decW
                )
                val bw = ceil(decBox.w * compToBImgF).toInt()
                val bh = ceil(decBox.h * compToBImgF).toInt()

                if (bw != cachedW || bh != cachedH) {
                    cachedStrings.clear()
                    cachedW = bw
                    cachedH = bh
                }

                val str = text.joinToString("\n")

                var fontSize = TextHud.MC_FONT_SIZE
                val font = TextHud.fontMainBase.deriveFont(Font.PLAIN, fontSize)
                g.font = font

                val lines = text.map { StringParser.processString(
                    it,
                    options.stringShadow,
                    g,
                    font, font, font,
                    fontSize
                ) }
                val visualWidth = lines.maxOf { it.visualWidth }
                val (fontScale, _) = BoundingBox(
                    0.0, 0.0,
                    visualWidth.toDouble(), fontSize.toDouble() * lines.size
                ).fitInside(decBox)

                fontSize = ceil(fontSize * (fontScale * compToBImgF).toFloat())

                val key = CachedStringKey(str, fontSize.toInt())
                textToRender.add(TextRenderParam(decBox, key, text))
            }
        }

        if (textToRender.isNotEmpty()) {
            val fontSize = textToRender.minOf { it.key.size }.toFloat()
            val font = TextHud.fontMainBase.deriveFont(Font.PLAIN, fontSize)
            g.font = font
            g.paint = Color(-1)

            textToRender.forEach { (decBox, key, text) ->
                val rendered = cachedStrings.getOrPut(key) {
                    val lines = text.map { StringParser.processString(
                        it,
                        options.stringShadow,
                        g,
                        font, font, font,
                        fontSize
                    ) }
                    val visualWidth = lines.maxOf { it.visualWidth }

                    val w = visualWidth + (if (options.stringShadow) 0.1 * fontSize else 0.0) + 5.0
                    val h = (fontSize * lines.size + g.fontMetrics.ascent).toDouble()
                    val img = bimgProvider.create(w.toInt(), h.toInt())

                    TextRendererImpl.drawImage(img, TextRendererImpl.TextRenderParams(
                        TextHud.Align.Center,
                        options.stringShadow,
                        TextHud.Backdrop.None,
                        fontSize,
                        font,
                        lines,
                        visualWidth
                    ))

                    CachedRenderedString(
                        img,
                        0.0, 0.0,
                        w,
                        h,
                        visualWidth.toDouble(),
                        fontSize * lines.size.toDouble()
                    )
                }

                val box = BoundingBox(
                    0.0, 0.0,
                    rendered.w, rendered.h
                ).centerInside(BoundingBox(
                    decBox.x * compToBImgF,
                    decBox.y * compToBImgF,
                    decBox.w * compToBImgF,
                    decBox.h * compToBImgF
                ))

                val bx = (box.x + rendered.xo).toInt()
                val bz = (box.y + rendered.yo).toInt()
                val bw = ceil(rendered.wr).toInt()
                val bh = ceil(rendered.hr).toInt()
                g.drawImage(
                    rendered.img,
                    bx, bz,
                    bw, bh,
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
            val bx = (compToBImgF * cx).toInt()
            val bz = (compToBImgF * cz).toInt()
            val bw = ceil(compToBImgF * cw).toInt()
            val bh = ceil(compToBImgF * ch).toInt()

            g.fillRect(bx, bz, bw, bh)
        }

        doors.forEach { door ->
            if (door == null) return@forEach
            if (!options.renderUnknownRooms && door.rooms.all { !it.explored }) return@forEach
            val color = when (door.type) {
                DoorTypes.ENTRANCE -> colors[DungeonMapColors.DoorEntrance]
                DoorTypes.WITHER -> colors[DungeonMapColors.DoorWither]
                DoorTypes.BLOOD -> colors[DungeonMapColors.DoorBlood]
                DoorTypes.NORMAL -> {
                    if (!door.opened) return@forEach
                    val room = door.rooms.minByOrNull {
                        it.type.prio - (if (!it.explored && !options.renderUnknownRooms) 100 else 0)
                    } ?: return@forEach

                    if (room.type == RoomTypes.FAIRY && !room.explored) colors[DungeonMapColors.DoorWither]
                    else colorForRoom(room)
                }
            } ?: colors[DungeonMapColors.RoomNormal] ?: return@forEach

            g.paint = color
            drawDoor(
                door.roomComp1.x,
                door.roomComp1.z,
                door.roomComp2.x,
                door.roomComp2.z,
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