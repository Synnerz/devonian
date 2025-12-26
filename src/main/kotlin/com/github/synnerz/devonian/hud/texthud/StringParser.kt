package com.github.synnerz.devonian.hud.texthud

import java.awt.*
import java.awt.font.TextAttribute
import java.awt.font.TextLayout
import java.text.AttributedCharacterIterator
import java.text.AttributedString
import java.util.stream.Collectors

object StringParser {
    fun isColorCode(c: Char): Boolean {
        return (c in '0'..'9') || (c in 'a'..'f')
    }

    private fun addAttribute(
        str: AttributedString?,
        a: AttributedCharacterIterator.Attribute,
        v: Any?,
        s: Int,
        e: Int
    ) {
        if (s >= e) return
        str?.addAttribute(a, v, s, e)
    }

    private fun setFontAttrBackup(att: AttributedString, c: Char, f: Font, i: Int) {
        var f = f
        if (!f.canDisplay(c)) {
            val n = getBackupFont(c)
            if (n != null) f = n
        }
        addAttribute(att, TextAttribute.FONT, f, i, i + 1)
    }

    private val backupFontMap: MutableMap<Char, Font?> = HashMap()
    private fun getBackupFont(c: Char): Font? {
        // null :sob:
        if (!backupFontMap.containsKey(c)) backupFontMap[c] =
            findBackupFont(c)
        return backupFontMap[c]
    }

    private fun findBackupFont(c: Char): Font? {
        for (f in GraphicsEnvironment.getLocalGraphicsEnvironment().allFonts) {
            if (f.canDisplay(c)) return f
        }
        return null
    }

    private fun setFontAttr(att: AttributedString?, str: CharArray, f1: Font, f2: Font, s: Int, e: Int) {
        var s = s
        if (att == null) return
        if (s >= e) return
        if (s >= str.size) return
        if (e > str.size) return

        var i = f1.canDisplayUpTo(str, s, e)
        while (s < str.size && i >= 0) {
            addAttribute(att, TextAttribute.FONT, f1, s, i)
            setFontAttrBackup(att, str[i], f2, i)
            s = i + 1
            i = f1.canDisplayUpTo(str, s, e)
        }
        addAttribute(att, TextAttribute.FONT, f1, s, e)
    }

    val COLORS = mapOf(
        '0' to Color(0),
        '1' to Color(170),
        '2' to Color(43520),
        '3' to Color(43690),
        '4' to Color(11141120),
        '5' to Color(11141290),
        '6' to Color(16755200),
        '7' to Color(11184810),
        '8' to Color(5592405),
        '9' to Color(5592575),
        'a' to Color(5635925),
        'b' to Color(5636095),
        'c' to Color(16733525),
        'd' to Color(16733695),
        'e' to Color(16777045),
        'f' to Color(16777215),
    )

    private fun obfuscate(c: Char): Char {
        var n = (Math.random() * 142).toInt()
        n += 33 + (if (n >= 94) 1 else 0)
        return n.toChar()
    }

    fun processString(
        str: String,
        g: Graphics2D,
        f1: Font,
        f2: Font,
        f3: Font,
        fontSize: Float,
    ): LayoutLineData {
        var str = str
        str = "&f$str&r"
        val sb = StringBuilder()
        val o: MutableList<ObfData> = ArrayList()
        val atts: MutableList<AttrData> = ArrayList()
        var cAtts: MutableList<AttrData> = ArrayList()
        var obfS = -1

        var i = 0
        while (i < str.length) {
            val c = str[i]
            if ((c == '&' || c == '§') && i < str.length - 1) {
                val k = str[i + 1]
                if (k == '\u200B') {
                    sb.append(if (obfS >= 0) ' ' else c)
                    i++
                    i++
                    continue
                }
                if (isColorCode(k)) {
                    cAtts = cAtts
                        .stream()
                        .filter { v: AttrData ->
                            if (!isColorCode(v.t)) return@filter true
                            atts.add(AttrData(v.t, v.s, sb.length))
                            false
                        }
                        .collect(Collectors.toList())
                    cAtts.add(AttrData(k, sb.length, 0))
                    i++
                    i++
                    continue
                }
                if (k == 'k') {
                    obfS = sb.length
                    i++
                    i++
                    continue
                }
                if (k == 'l' || k == 'o' || k == 'm' || k == 'n') {
                    cAtts.add(AttrData(k, sb.length, 0))
                    i++
                    i++
                    continue
                }
                if (k == 'r') {
                    cAtts.forEach { v -> atts.add(AttrData(v.t, v.s, sb.length)) }
                    cAtts.clear()
                    cAtts.add(AttrData('f', sb.length, 0))
                    if (obfS >= 0) o.add(ObfData(obfS, sb.length))
                    obfS = -1
                    i++
                    i++
                    continue
                }
            }
            sb.append(if (obfS >= 0) obfuscate(c) else c)
            i++
        }

        val s = sb.toString()
        val ca = s.toCharArray()
        val attStr = AttributedString(s)

        addAttribute(attStr, TextAttribute.SIZE, fontSize, 0, s.length)
        var end = 0
        for (v in o) {
            setFontAttr(attStr, ca, f1, f3, end, v.s)
            addAttribute(attStr, TextAttribute.FONT, f2, v.s, v.e)
            end = v.e
        }
        setFontAttr(attStr, ca, f1, f3, end, s.length)

        atts.forEach { v ->
            if (isColorCode(v.t)) {
                return@forEach
            } else if (v.t == 'l') {
                addAttribute(attStr, TextAttribute.WEIGHT, TextAttribute.WEIGHT_BOLD, v.s, v.e)
            } else if (v.t == 'o') {
                addAttribute(attStr, TextAttribute.POSTURE, TextAttribute.POSTURE_OBLIQUE, v.s, v.e)
            } else if (v.t == 'm') {
                addAttribute(attStr, TextAttribute.STRIKETHROUGH, TextAttribute.STRIKETHROUGH_ON, v.s, v.e)
            } else if (v.t == 'n') {
                addAttribute(attStr, TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_LOW_ONE_PIXEL, v.s, v.e)
            } else throw IllegalStateException("unknown attribute: " + v.t)
        }

        val tyl = TextLayout(attStr.iterator, g.fontRenderContext)

        return LayoutLineData(
            tyl.visibleAdvance,
            tyl.ascent,
            tyl.descent,
            o.isNotEmpty(),
            atts,
            attStr,
            null,
        )
    }

    class LayoutLineData(
        width: Float,
        ascent: Float,
        descent: Float,
        hasObfuText: Boolean,
        val atts: List<AttrData>,
        val attStr: AttributedString,
        var shapes: List<Pair<Color, Shape>>?,
    ) : StylizedTextHud.LineData(
        width,
        ascent,
        descent,
        hasObfuText,
    )

    class AttrData(val t: Char, val s: Int, val e: Int)

    class ObfData internal constructor(val s: Int, val e: Int)
}