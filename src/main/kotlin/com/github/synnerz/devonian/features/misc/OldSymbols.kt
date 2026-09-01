package com.github.synnerz.devonian.features.misc

import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature

object OldSymbols : Feature(
    "oldSymbols",
    "Changes the font rendering to use old symbols over the new ones.",
    category = Categories.VANILLA_TWEAKS,
) {
    @JvmField
    val REPLACEMENTS: IntArray

    init {
        val map = linkedMapOf(
            1 to '⚔', // attack speed
            2 to '๑', // ability damage
            3 to '✎', // intelligence
            4 to '⚡', // mana regen
            5 to 'Ⓟ', // breaking power
            6 to '❄', // cold resistance
            7 to '☠', // crit damage
            8 to '❈', // defense
            9 to '⚓', // double hook chance

            10 to '☡', // fear (the symbol is a guess)
            11 to '⫽', // ferocity
            12 to '☂', // fishing speed
            13 to '❁', // strength
            15 to '▚', // gemstone spread
            16 to '❤', // health
            17 to '❣', // health regen
            18 to '♨', // heat resistance
            19 to '♣', // pet luck

            20 to '☄', // mending
            21 to '⸕', // mining speed
            22 to '▚', // mining spread
            23 to 'ʬ', // overflow mana
            25 to 'ൠ', // bonus pest chance
            26 to '✯', // magic find
            27 to '❍', // pressure resistance
            28 to '✧', // pristine
            29 to '⚶', // respiration

            30 to '❁', // rift damage
            31 to '❤', // hearts (rift)
            32 to 'ф', // rift time
            33 to 'α', // sea creature chance
            34 to '✦', // speed
            35 to '∮', // sweep
            36 to 'Ⓢ', // swing range
            37 to '⛃', // treasure chance
            39 to '❂', // true defense

            40 to '♨', // vitality
            42 to '♔', // trophy chance
            43 to '☀', // overbloom
            44 to '☣', // crit chance
            45 to 'ᛷ', // pull
            46 to '☨', // timber (this stat did not exist before)

            80 to '❁', // damage
            81 to '☘', // farming fortune + others
            83 to '☘', // mining fortune + others
            84 to '☘', // foraging fortune + others
            91 to '☘', // hunting fortune
            103 to '⏣', // subarea

            112 to '✈', // airborne
            113 to '☮', // animal
            114 to '⚓', // aquatic
            115 to '♃', // arcane
            116 to 'Ж', // arthropod
            117 to '⚙', // construct
            118 to '⚂', // cubic
            119 to '♣', // elusive
            120 to '⊙', // ender
            121 to '❆', // frozen
            122 to '❆', // glacial
            123 to '✰', // humanoid
            124 to '♨', // infernal
            125 to '♆', // magmatic
            126 to '✿', // mythological
            127 to 'ൠ', // pest
            128 to '⛨', // shielded
            // 129 to '🦴', // skeletal
            130 to '☽', // spooky
            131 to '⸕', // subterranean
            132 to '༕', // undead
            133 to '☠', // wither
            134 to '⸙', // woodland
            135 to '☋', // critter (did not exist before)
            136 to '♿', // timid (did not exist before)
        )

        val max = map.keys.maxOrNull() ?: -1
        REPLACEMENTS = IntArray(max + 1)
        map.forEach { (k: Int, v: Char) ->
            REPLACEMENTS[k] = v.code
        }

        REPLACEMENTS[129] = "\uD83E\uDDB4".codePointAt(0) // skeletal
    }
}