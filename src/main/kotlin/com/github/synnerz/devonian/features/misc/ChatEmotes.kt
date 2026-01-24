package com.github.synnerz.devonian.features.misc

import com.github.synnerz.devonian.features.Feature

object ChatEmotes : Feature(
    "chatEmotes",
    "works same way as /emotes in hypixel does - :java: -> :coffee:",
) {
    private val emoticons = linkedMapOf(
        "(?<=\\s|^)<3(?=\\s|$)".toRegex() to "❤",
        "(?<=\\s|^)o/(?=\\s|$)".toRegex() to "( ﾟ◡ﾟ)/",
        "(?<=\\s|^)h/(?=\\s|$)".toRegex() to "ヽ(^◇^*)/",
    )
    private val emoteRegex = "(?<=\\s|^):(\\w+):(?=\\s|$)".toRegex()
    private val emoteMap = mapOf(
        "star" to "✮",
        "yes" to "✔",
        "no" to "✖",
        "java" to ":coffee:",
        "arrow" to "➜",
        "shrug" to "¯\\_(ツ)_/¯",
        "tableflip" to "(╯°□°）╯︵ ┻━┻",
        "totem" to "☉_☉",
        "typing" to "✎...",
        "maths" to "√(π+x)=L",
        "snail" to "@'-'",
        "thinking" to "(0.o?)",
        "gimme" to "༼つ◕_◕༽つ",
        "wizard" to "('-')⊃━☆ﾟ.*･｡ﾟ",
        "pvp" to "⚔",
        "peace" to "✌",
        "oof" to "OOF",
        "puffer" to "<('O')>",
        "cute" to "(✿◠‿◠)",
        "dj" to "ヽ(⌐■_■)ノ♬",
        "sloth" to "(・⊝・)",
        "dog" to "(ᵔᴥᵔ)",
        "snow" to "☃",
        "cat" to "= ＾● ⋏ ●＾ =",
        "yey" to "ヽ (◕◡◕) ﾉ",
        "dab" to "<o/",
    )

    fun modifyMessage(str: String): String {
        var str = str

        emoticons.forEach { (k, v) ->
            str = str.replace(k, v)
        }

        str = str.replace(emoteRegex) { match ->
            val key = match.groupValues.getOrNull(1)
            key?.let { emoteMap[it] } ?: match.value
        }

        return str
    }
}
