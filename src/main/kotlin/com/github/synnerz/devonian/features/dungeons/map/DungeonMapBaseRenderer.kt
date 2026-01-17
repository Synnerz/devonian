package com.github.synnerz.devonian.features.dungeons.map

import com.github.synnerz.devonian.api.bufimgrenderer.BufferedImageRenderer
import com.github.synnerz.devonian.api.dungeon.DungeonRoom
import com.github.synnerz.devonian.api.dungeon.WorldComponentPosition
import com.github.synnerz.devonian.api.dungeon.mapEnums.*
import com.github.synnerz.devonian.hud.texthud.*
import com.github.synnerz.devonian.utils.BoundingBox
import com.github.synnerz.devonian.utils.TextRendererImpl
import java.awt.Color
import java.awt.Font
import java.awt.image.BufferedImage
import java.util.*
import javax.imageio.ImageIO
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max

class DungeonMapBaseRenderer :
    BufferedImageRenderer<DungeonMapRenderData>("dungeonMapBaseRenderer"), FontListener {
    val cachedStrings: MutableMap<CachedStringKey, CachedRenderedString> = Collections.synchronizedMap(
        object : LinkedHashMap<CachedStringKey, CachedRenderedString>(36) {
            override fun removeEldestEntry(eldest: Map.Entry<CachedStringKey?, CachedRenderedString?>?): Boolean = size > 36
        }
    )
    var cachedW = 0
    var cachedH = 0
    var cachedSize = 0

    data class CachedStringKey(val str: String, val shadow: StylizedTextHud.Shadow)

    data class CachedRenderedString(
        val img: BufferedImage,
        val xo: Double, val yo: Double,
        val w: Double, val h: Double,
    )

    override fun onFontChange(f: Font) {
        cachedStrings.clear()
    }

    init {
        BImgTextHudRenderer.registerFontListener(this)
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

        if ((colors[DungeonMapColors.Background]?.alpha ?: 0) > 0) {
            g.paint = colors[DungeonMapColors.Background]
            g.fillRect(0, 0, w, h)
        }

        if (options.border > 0 && (colors[DungeonMapColors.Border]?.alpha ?: 0) > 0) {
            g.paint = colors[DungeonMapColors.Border]
            val lw = options.border
            g.fillRect(0, 0, w, lw)
            g.fillRect(0, lw, lw, h - lw)
            g.fillRect(w - lw, lw, lw, h - lw)
            g.fillRect(lw, h - lw, w - lw - lw, lw)
        }

        fun colorForRoom(room: DungeonRoom): Color? {
            var col =
                if (!room.explored && (!options.renderUnknownRooms || room.name == null))
                    colors[DungeonMapColors.RoomUnknown]
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
        val maxDim = max(options.dungeonWidth, options.dungeonHeight)
        val totalMaxDim = maxDim + options.padding * 2
        val compToBImgFW = w / totalMaxDim
        val compToBImgFH = h / totalMaxDim
        val bImgOX = ((maxDim - options.dungeonWidth) / 2.0 + options.padding) * compToBImgFW
        val bImgOY = ((maxDim - options.dungeonHeight) / 2.0 + options.padding) * compToBImgFH

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
            val bx = (compToBImgFW * cx + bImgOX).toInt()
            val bz = (compToBImgFH * cz + bImgOY).toInt()
            val bw = ceil(compToBImgFW * cw).toInt()
            val bh = ceil(compToBImgFH * ch).toInt()

            g.fillRect(bx, bz, bw + 1, bh + 1)
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
                    val data = colors[DungeonMapColors.DoorNormal]
                    if (data != null) data
                    else {
                        val room = door.rooms.minByOrNull {
                            it.type.prio - (if (!it.explored && !options.renderUnknownRooms) 100 else 0)
                        } ?: return@forEach

                        if (!room.explored && door.holyShitFairyDoorPleaseStopFlashingSobs) colors[DungeonMapColors.DoorWither]
                        else colorForRoom(room)
                    }
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

        fun drawRoom(x: Int, z: Int, w: Int, h: Int) {
            val cx = x + roomRectOffset
            val cz = z + roomRectOffset
            val cw = options.roomWidth + w - 1
            val ch = options.roomWidth + h - 1
            val bx = (compToBImgFW * cx + bImgOX).toInt()
            val bz = (compToBImgFH * cz + bImgOY).toInt()
            val bw = ceil(compToBImgFW * cw).toInt()
            val bh = ceil(compToBImgFH * ch).toInt()

            g.fillRect(bx, bz, bw + 1, bh + 1)
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
            val bx = (compToBImgFW * cx + bImgOX).toInt()
            val bz = (compToBImgFH * cz + bImgOY).toInt()
            val bw = ceil(compToBImgFW * cw).toInt()
            val bh = ceil(compToBImgFH * ch).toInt()

            g.fillRect(bx, bz, bw + 1, bh + 1)
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

        val decW = options.textSize * options.roomWidth
        val bw = ceil(decW * compToBImgFW).toInt()
        val bh = ceil(decW * compToBImgFH).toInt()
        val fontSize = ceil(options.roomWidth * compToBImgFH * 0.25 * options.textSize).toInt()

        if (bw != cachedW || bh != cachedH || fontSize != cachedSize) {
            cachedStrings.clear()
            cachedW = bw
            cachedH = bh
            cachedSize = fontSize
        }

        val textToRender = mutableListOf<TextRenderParam>()
        rooms.forEach { room ->
            if (room == null) return@forEach
            if (room.doors.isEmpty() && room.name == null) return@forEach
            val color = colorForRoom(room) ?: colors[DungeonMapColors.RoomNormal] ?: Color(0, true)
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
                    if (options.renderUnknownRooms && room.checkmark == CheckmarkTypes.UNEXPLORED && room.name != null) null
                    else CHECKMARK[room.checkmark]
                } else null) ?:
                (if (renderRoomInfo && options.puzzleIcon) SPECIAL_ROOMS[room.name] else null)
            val text = mutableListOf<String>()

            if (
                renderRoomInfo &&
                (
                    !options.roomNameNotEFB ||
                    (room.type != RoomTypes.ENTRANCE && room.type != RoomTypes.FAIRY && room.type != RoomTypes.BLOOD)
                ) &&
                (if (room.type == RoomTypes.PUZZLE) options.puzzleName else options.roomName)
            ) room.name?.also { name ->
                val colorCode = if (options.colorRoomName) when (room.type) {
                    RoomTypes.ENTRANCE,
                    RoomTypes.FAIRY,
                        -> "&f"

                    RoomTypes.BLOOD -> if (room.checkmark == CheckmarkTypes.GREEN) "&a" else "&f"

                    else -> when (room.checkmark) {
                        CheckmarkTypes.FAILED -> "&c"
                        CheckmarkTypes.GREEN -> "&a"
                        CheckmarkTypes.WHITE -> "&f"
                        CheckmarkTypes.NONE -> "&7"
                        CheckmarkTypes.UNEXPLORED ->
                            if (room.explored || options.dungeonStarted) "&7"
                            else "&f"
                    }
                } else ""
                name.replace("\u200B", "- ").split(" ").forEach { text.add("$colorCode$it")}
            }
            if (renderRoomInfo && options.secretCount && room.totalSecrets > 0) {
                val colorCode = when (room.checkmark) {
                    CheckmarkTypes.FAILED -> "&c"
                    CheckmarkTypes.GREEN -> "&a"
                    CheckmarkTypes.WHITE -> "&f"
                    CheckmarkTypes.NONE -> "&7"
                    CheckmarkTypes.UNEXPLORED ->
                        if (room.explored || options.dungeonStarted) "&7"
                        else "&f"
                }
                text.add("$colorCode${
                    if (room.checkmark == CheckmarkTypes.GREEN) max(room.totalSecrets, room.secretsCompleted)
                    else if (room.secretsCompleted < 0) "?"
                    else room.secretsCompleted
                }/${room.totalSecrets}")
            }

            if (decoration == null && text.isEmpty()) return@forEach

            if (decoration != null && !(options.roomNoFairyCheck && room.type == RoomTypes.FAIRY)) {
                val decW = options.iconSize * options.roomWidth
                val center = getCenterOf(cells, shape, options.iconAlignment)
                val decBox = BoundingBox(
                    center.first - decW * 0.5,
                    center.second - decW * 0.5,
                    decW, decW
                )
                val bx = (decBox.x * compToBImgFW + bImgOX).toInt()
                val by = (decBox.y * compToBImgFH + bImgOY).toInt()
                val bw = ceil(decBox.w * compToBImgFW).toInt()
                val bh = ceil(decBox.h * compToBImgFH).toInt()
                g.drawImage(decoration, bx, by, bw, bh, null)
            }

            if (text.isNotEmpty()) {
                val center = getCenterOf(cells, shape, options.textAlignment)
                val decBox = BoundingBox(
                    center.first - decW * 0.5,
                    center.second - decW * 0.5,
                    decW, decW
                )

                val str = text.joinToString("\n")
                val key = CachedStringKey(str, options.stringShadow)

                textToRender.add(TextRenderParam(decBox, key, text))
            }
        }

        if (textToRender.isNotEmpty()) {
            val fontSizeF = fontSize.toFloat()
            val font = BImgTextHudRenderer.fontMainBase.deriveFont(Font.PLAIN, fontSizeF)
            g.font = font
            g.paint = Color(-1)
            val ascent = g.fontMetrics.ascent

            textToRender.forEach { (decBox, key, text) ->
                if (text.isEmpty()) return@forEach

                val rendered = cachedStrings.getOrPut(CachedStringKey(key.str, key.shadow)) {
                    val lines = text.map { StringParser.processString(
                        it,
                        g,
                        font, font, font,
                        fontSizeF
                    ) }
                    val width = lines.maxOf { it.width }

                    val w = ceil(width + options.stringShadow.getSizeIncrease(fontSizeF) + 5.0f).toInt()
                    val h = fontSize * lines.size + ascent
                    val img = bimgProvider.create(w, h)

                    TextRendererImpl.drawImage(img, TextRenderer.RenderParams(
                        StylizedTextHud.TextRenderParams(
                            StylizedTextHud.Align.Center,
                            options.stringShadow,
                            StylizedTextHud.Backdrop.None,
                            fontSizeF,
                        ),
                        lines,
                        width,
                        lines.maxOfOrNull { it.descent } ?: 0f,
                        lines.maxOfOrNull { it.ascent } ?: 0f,
                        true,
                    ))

                    CachedRenderedString(
                        img,
                        0.0, -ascent / 2.0,
                        width.toDouble(),
                        fontSize * lines.size.toDouble(),
                    )
                }

                val box = BoundingBox(
                    0.0, 0.0,
                    rendered.w, rendered.h
                ).centerInside(BoundingBox(
                    decBox.x * compToBImgFW + bImgOX,
                    decBox.y * compToBImgFH + bImgOY,
                    decBox.w * compToBImgFW,
                    decBox.h * compToBImgFH
                ))

                val bx = (box.x + rendered.xo).toInt()
                val bz = (box.y + rendered.yo).toInt()
                g.drawImage(
                    rendered.img,
                    bx, bz,
                    null
                )
            }
        }

        g.dispose()
        return img
    }

    companion object {
        val CHECKMARK = mapOf(
            CheckmarkTypes.NONE to null,
            CheckmarkTypes.WHITE to getImg("checks/whiteCheck.png")!!,
            CheckmarkTypes.GREEN to getImg("checks/greenCheck.png")!!,
            CheckmarkTypes.FAILED to getImg("checks/failedRoom.png")!!,
            CheckmarkTypes.UNEXPLORED to getImg("checks/questionMark.png")!!,
        )
        val SPECIAL_ROOMS = mapOf(
            "Creeper Beams" to getImg("puzzles/creeper.png")!!,
            "Three Weirdos" to getImg("puzzles/chest.png")!!,
            "Tic Tac Toe" to getImg("puzzles/shears.png")!!,
            "Water Board" to getImg("puzzles/bucket_water.png")!!,
            "Teleport Maze" to getImg("puzzles/endframe_side.png")!!,
            "Blaze" to getImg("puzzles/blaze_powder.png")!!,
            "Boulder" to getImg("puzzles/planks_oak.png")!!,
            "Ice Fill" to getImg("puzzles/ice.png")!!,
            "Ice Path" to getImg("puzzles/spawner.png")!!,
            "Quiz" to getImg("puzzles/book_normal.png")!!,
        )

        private fun getImg(path: String): BufferedImage? {
            val stream = this::class.java.getResourceAsStream("/assets/devonian/dungeons/map/$path") ?: return null
            val img = ImageIO.read(stream)
            return img
        }
    }
}