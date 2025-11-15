package com.github.synnerz.devonian.hud.texthud

import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.GraphicsEnvironment
import java.awt.font.TextAttribute
import java.awt.font.TextLayout
import java.text.AttributedCharacterIterator
import java.text.AttributedString
import java.util.function.Consumer
import java.util.stream.Collectors

object StringParser {
    private fun isColorCode(c: Char): Boolean {
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

    private val COLORS1: MutableMap<Char, Color> = HashMap()
    private val COLORS2: MutableMap<Char, Color> = HashMap()

    init {
        COLORS1['0'] = Color(0)
        COLORS1['1'] = Color(170)
        COLORS1['2'] = Color(43520)
        COLORS1['3'] = Color(43690)
        COLORS1['4'] = Color(11141120)
        COLORS1['5'] = Color(11141290)
        COLORS1['6'] = Color(16755200)
        COLORS1['7'] = Color(11184810)
        COLORS1['8'] = Color(5592405)
        COLORS1['9'] = Color(5592575)
        COLORS1['a'] = Color(5635925)
        COLORS1['b'] = Color(5636095)
        COLORS1['c'] = Color(16733525)
        COLORS1['d'] = Color(16733695)
        COLORS1['e'] = Color(16777045)
        COLORS1['f'] = Color(16777215)
        COLORS2['0'] = Color(0)
        COLORS2['1'] = Color(42)
        COLORS2['2'] = Color(10752)
        COLORS2['3'] = Color(10794)
        COLORS2['4'] = Color(2752512)
        COLORS2['5'] = Color(2752554)
        COLORS2['6'] = Color(4139520)
        COLORS2['7'] = Color(2763306)
        COLORS2['8'] = Color(1381653)
        COLORS2['9'] = Color(1381695)
        COLORS2['a'] = Color(1392405)
        COLORS2['b'] = Color(1392447)
        COLORS2['c'] = Color(4134165)
        COLORS2['d'] = Color(4134207)
        COLORS2['e'] = Color(4144917)
        COLORS2['f'] = Color(4144959)
    }

    private fun obfuscate(c: Char): Char {
        var n = (Math.random() * 142).toInt()
        n += 33 + (if (n >= 94) 1 else 0)
        return n.toChar()
    }

    fun processString(
        str: String,
        shadow: Boolean,
        g: Graphics2D,
        f1: Font,
        f2: Font,
        f3: Font,
        fontSize: Float
    ): LineData {
        var str = str
        str = "$str&r"
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
                    cAtts.forEach(Consumer { v: AttrData -> atts.add(AttrData(v.t, v.s, sb.length)) })
                    cAtts.clear()
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
        val a = AttributedString(s)
        val b = if (shadow) AttributedString(s) else null

        addAttribute(a, TextAttribute.SIZE, fontSize, 0, s.length)
        addAttribute(b, TextAttribute.SIZE, fontSize, 0, s.length)
        var end = 0
        for (v in o) {
            setFontAttr(a, ca, f1, f3, end, v.s)
            setFontAttr(b, ca, f1, f3, end, v.s)
            addAttribute(a, TextAttribute.FONT, f2, v.s, v.e)
            addAttribute(b, TextAttribute.FONT, f2, v.s, v.e)
            end = v.e
        }
        setFontAttr(a, ca, f1, f3, end, s.length)
        setFontAttr(b, ca, f1, f3, end, s.length)

        atts.forEach(Consumer { v: AttrData ->
            if (isColorCode(v.t)) {
                addAttribute(a, TextAttribute.FOREGROUND, COLORS1[v.t], v.s, v.e)
                addAttribute(b, TextAttribute.FOREGROUND, COLORS2[v.t], v.s, v.e)
            } else if (v.t == 'l') {
                addAttribute(b, TextAttribute.WEIGHT, TextAttribute.WEIGHT_BOLD, v.s, v.e)
                addAttribute(a, TextAttribute.WEIGHT, TextAttribute.WEIGHT_BOLD, v.s, v.e)
            } else if (v.t == 'o') {
                addAttribute(b, TextAttribute.POSTURE, TextAttribute.POSTURE_OBLIQUE, v.s, v.e)
                addAttribute(a, TextAttribute.POSTURE, TextAttribute.POSTURE_OBLIQUE, v.s, v.e)
            } else if (v.t == 'm') {
                addAttribute(b, TextAttribute.STRIKETHROUGH, TextAttribute.STRIKETHROUGH_ON, v.s, v.e)
                addAttribute(a, TextAttribute.STRIKETHROUGH, TextAttribute.STRIKETHROUGH_ON, v.s, v.e)
            } else if (v.t == 'n') {
                addAttribute(b, TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_LOW_ONE_PIXEL, v.s, v.e)
                addAttribute(a, TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_LOW_ONE_PIXEL, v.s, v.e)
            } else throw RuntimeException("unknown attribute: " + v.t)
        })

        val tylA = TextLayout(a.iterator, g.fontRenderContext)
        val tylB = if (b == null) null else TextLayout(b.iterator, g.fontRenderContext)

        return LineData(
            o.toTypedArray<ObfData>(),
            tylA.advance,
            tylA.visibleAdvance,
            tylA,
            tylB
        )
    }

    class LineData internal constructor(
        val obfData: Array<ObfData>,
        val width: Float,
        val visualWidth: Float,
        val layout: TextLayout,
        val layoutShadow: TextLayout?
    )

    internal class AttrData(val t: Char, val s: Int, val e: Int)

    class ObfData internal constructor(val s: Int, val e: Int)
}