package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.devonian.api.splits.TimerSplit
import com.github.synnerz.devonian.api.splits.TimerSplitData

enum class BossSplitTypes(val ins: TimerSplit) {
    F1(
        TimerSplit(
            TimerSplitData(
                null,
                "^\\[BOSS] Bonzo: Gratz for making it this far, but I'm basically unbeatable\\.$".toRegex(),
                false
            ),
            TimerSplitData(
                "&cFirst Phase&f: &a$1",
                "^\\[BOSS] Bonzo: Oh I'm dead!$".toRegex()
            ),
            TimerSplitData(
                "&cBonzo&f: &a$1",
                "^ +> EXTRA STATS <$".toRegex()
            )
        )
    ),
    F2(
        TimerSplit(
            TimerSplitData(
                null,
                "^\\[BOSS] Scarf: This is where the journey ends for you, Adventurers\\.$".toRegex(),
                false
            ),
            TimerSplitData(
                "&7Undeads&f: &a$1",
                "^\\[BOSS] Scarf: Those toys are not strong enough I see\\.$".toRegex()
            ),
            TimerSplitData(
                "&aScarf&f: &a$1",
                "^ +> EXTRA STATS <$".toRegex()
            )
        )
    ),
    F3(
        TimerSplit(
            TimerSplitData(
                null,
                "^\\[BOSS] The Professor: I was burdened with terrible news recently\\.\\.\\.$".toRegex(),
                false
            ),
            TimerSplitData(
                "&bGuardians&f: &a$1",
                "^\\[BOSS] The Professor: Oh\\? You found my Guardians' one weakness\\?$".toRegex()
            ),
            TimerSplitData(
                "&bPhase One&f: &a$1",
                "^\\[BOSS] The Professor: I see\\. You have forced me to use my ultimate technique\\.$".toRegex()
            ),
            TimerSplitData(
                "&bPhase Two&f: &a$1",
                "^\\[BOSS] The Professor: What\\?! My Guardian power is unbeatable!$".toRegex()
            ),
            TimerSplitData(
                "&aThe Professor&f: &a$1",
                "^ +> EXTRA STATS <$".toRegex()
            )
        )
    ),
    F4(
        TimerSplit(
            TimerSplitData(
                null,
                "^\\[BOSS] Thorn: Welcome Adventurers! I am Thorn, the Spirit! And host of the Vegan Trials!$".toRegex(),
                false
            ),
            TimerSplitData(
                "&aThorn&f: &a$1",
                "^ +> EXTRA STATS <$".toRegex()
            )
        )
    ),
    F5(
        TimerSplit(
            TimerSplitData(
                null,
                "^\\[BOSS] Livid: Welcome, you've arrived right on time\\. I am Livid, the Master of Shadows\\.$".toRegex(),
                false
            ),
            TimerSplitData(
                "&aLivid&f: &a$1",
                "^ +> EXTRA STATS <$".toRegex()
            )
        )
    ),
    F6(
        TimerSplit(
            TimerSplitData(
                null,
                "^\\[BOSS] Sadan: So you made it all the way here\\.\\.\\. Now you wish to defy me\\? Sadan\\?!$".toRegex(),
                false
            ),
            TimerSplitData(
                "&dTerracottas&f: &a$1",
                "^\\[BOSS] Sadan: ENOUGH!$".toRegex()
            ),
            TimerSplitData(
                "&bGiants&f: &a$1",
                "^\\[BOSS] Sadan: You did it\\. I understand now, you have earned my respect\\.$".toRegex()
            ),
            TimerSplitData(
                "&aSadan&f: &a$1",
                "^ +> EXTRA STATS <$".toRegex()
            )
        )
    ),
    F7(
        TimerSplit(
            TimerSplitData(
                null,
                "^\\[BOSS] Maxor: WELL! WELL! WELL! LOOK WHO'S HERE!$".toRegex(),
                false
            ),
            TimerSplitData(
                "&aMaxor&f: &a$1",
                "^\\[BOSS] Storm: Pathetic Maxor, just like expected\\.$".toRegex()
            ),
            TimerSplitData(
                "&bStorm&f: &a$1",
                "^\\[BOSS] Goldor: Who dares trespass into my domain\\?$".toRegex()
            ),
            TimerSplitData(
                "&eTerminals&f: &a$1",
                "^The Core entrance is opening!$".toRegex()
            ),
            TimerSplitData(
                "&7Goldor&f: &a$1",
                "^\\[BOSS] Necron: You went further than any human before, congratulations\\.$".toRegex()
            ),
            TimerSplitData(
                "&4Necron&f: &a$1",
                "^ +> EXTRA STATS <$".toRegex()
            )
        )
    ),
    M7(
        TimerSplit(
            TimerSplitData(
                null,
                "^\\[BOSS] Maxor: WELL! WELL! WELL! LOOK WHO'S HERE!$".toRegex(),
                false
            ),
            TimerSplitData(
                "&aMaxor&f: &a$1",
                "^\\[BOSS] Storm: Pathetic Maxor, just like expected\\.$".toRegex()
            ),
            TimerSplitData(
                "&bStorm&f: &a$1",
                "^\\[BOSS] Goldor: Who dares trespass into my domain\\?$".toRegex()
            ),
            TimerSplitData(
                "&eTerminals&f: &a$1",
                "^The Core entrance is opening!$".toRegex()
            ),
            TimerSplitData(
                "&7Goldor&f: &a$1",
                "^\\[BOSS] Necron: You went further than any human before, congratulations\\.$".toRegex()
            ),
            TimerSplitData(
                "&4Necron&f: &a$1",
                "^\\[BOSS] Necron: All this, for nothing\\.\\.\\.$".toRegex()
            ),
            TimerSplitData(
                "&4Wither King&f: &a$1",
                "^ +> EXTRA STATS <$".toRegex()
            )
        )
    );

    companion object {
        fun byName(name: String) = BossSplitTypes.entries.find { it.name == name.uppercase() }
    }
}