package com.github.synnerz.devonian.api.dungeon

import com.github.synnerz.devonian.api.splits.BranchingSplitStage
import com.github.synnerz.devonian.api.splits.SequentialSplitStage
import com.github.synnerz.devonian.api.splits.SplitStage
import com.github.synnerz.devonian.features.dungeons.BossSplits
import com.github.synnerz.devonian.features.dungeons.clear.RunSplits

object Stages {
    val Root: SequentialSplitStage

    val Clear: SplitStage

    val BloodOpen = SplitStage().withName("&4Blood").withLongTime()
    val FirstWatcherSpawn = SequentialSplitStage(
        arrayOf(
            SplitStage().withName("&cFirst Spawn").withLongTime(),
            SplitStage("[BOSS] The Watcher: Let's see how you can handle this.")
        )
    )
    val WatcherClear = SplitStage().withName("&cWatcher").withLongTime()
    val PortalEnter = SplitStage("[BOSS] The Watcher: You have proven yourself. You may pass.")
        .withName("&dPortal Enter").withLongTime()

    val BossEntry = SplitStage().withName("&9Boss Entry")

    val Boss: SplitStage
    val BossFloor: BranchingSplitStage

    val F1: SequentialSplitStage
    val F2: SequentialSplitStage
    val F3: SequentialSplitStage
    val F4: SequentialSplitStage
    val F5: SequentialSplitStage
    val F6: SequentialSplitStage

    val F7: SequentialSplitStage
    val Maxor: SplitStage
    val Storm: SplitStage
    val Terminals: SequentialSplitStage
    val S1: TerminalSection
    val S2: TerminalSection
    val S3: TerminalSection
    val S4: TerminalSection
    val Goldor: SplitStage
    val Necron: SplitStage
    val WitherKing: SplitStage

    val BossEnd = object : SplitStage("^ +> EXTRA STATS <$".toRegex()) {
        override fun _start() {
            super._start()
            RunSplits.onFloorEnd()
            BossSplits.onFloorEnd()
        }
    }

    init {
        Clear = SplitStage(
            "[NPC] Mort: Here, I found this map when I first entered the dungeon.",
            arrayOf(
                SequentialSplitStage(
                    arrayOf(
                        BloodOpen,
                        SplitStage("The BLOOD DOOR has been opened!", arrayOf(FirstWatcherSpawn, WatcherClear)),
                        PortalEnter
                    )
                ),
                BossEntry,
            )
        )

        F1 = SequentialSplitStage(
            "[BOSS] Bonzo: Gratz for making it this far, but I'm basically unbeatable.",
            arrayOf(
                SplitStage().withName("&cFirst Phase"),
                SplitStage("[BOSS] Bonzo: Oh I'm dead!").withName("&cSecond Phase"),
            )
        )

        F2 = SequentialSplitStage(
            "[BOSS] Scarf: This is where the journey ends for you, Adventurers.",
            arrayOf(
                SplitStage().withName("&7Undeads"),
                SplitStage("[BOSS] Scarf: Those toys are not strong enough I see.").withName("&8Scarf"),
            )
        )

        F3 = SequentialSplitStage(
            "[BOSS] The Professor: I was burdened with terrible news recently...",
            arrayOf(
                SplitStage().withName("&3Guardians"),
                SplitStage("[BOSS] The Professor: Oh? You found my Guardians' one weakness?")
                    .withName("&eHuman :("),
                SplitStage("[BOSS] The Professor: I see. You have forced me to use my ultimate technique.")
                    .withName("&dGuardian :)"),
            )
        )

        F4 = SequentialSplitStage(
            "[BOSS] Thorn: Welcome Adventurers! I am Thorn, the Spirit! And host of the Vegan Trials!",
            arrayOf(SplitStage().withName("&aThorn"))
        )

        F5 = SequentialSplitStage(
            "[BOSS] Livid: Welcome, you've arrived right on time. I am Livid, the Master of Shadows.",
            arrayOf(SplitStage().withName("&fLivid"))
        )

        F6 = SequentialSplitStage(
            "[BOSS] Sadan: So you made it all the way here... Now you wish to defy me? Sadan?!",
            arrayOf(
                SplitStage().withName("&4Terracottas"),
                SplitStage("[BOSS] Sadan: ENOUGH!").withName("&5Giants"),
                SplitStage("[BOSS] Sadan: You did it. I understand now, you have earned my respect.")
                    .withName("&6Sadan"),
            )
        )

        Maxor = SplitStage().withName("&5Maxor")
        Storm = SplitStage("[BOSS] Storm: Pathetic Maxor, just like expected.").withName("&9Storm")
        S1 = TerminalSection(4, 1).also { it.withName("S1") }
        S2 = TerminalSection(5, 2).also { it.withName("S2") }
        S3 = TerminalSection(4, 3).also { it.withName("S3") }
        S4 = TerminalSection(4, 4).also { it.withName("S4") }
        Terminals = SequentialSplitStage(
            "[BOSS] Goldor: Who dares trespass into my domain?",
            arrayOf(S1, S2, S3, S4),
        ).also { it.withName("&eTerminals") }
        Goldor = SplitStage("The Core entrance is opening!").withName("&8Goldor")
        Necron = SplitStage("[BOSS] Necron: You went further than any human before, congratulations.")
            .withName("&4Necron")
        WitherKing = SplitStage("[BOSS] Wither King: You... again?").withName("&0Wither King")

        F7 = SequentialSplitStage(
            "[BOSS] Maxor: WELL! WELL! WELL! LOOK WHO'S HERE!",
            arrayOf(
                Maxor,
                Storm,
                Terminals,
                Goldor,
                Necron,
                WitherKing,
            )
        )

        BossFloor = BranchingSplitStage(
            arrayOf(F1, F2, F3, F4, F5, F6, F7)
        )

        Boss = SplitStage(
            arrayOf(
                BossFloor,
                SplitStage().withName("&4Boss")
            )
        )

        Root = SequentialSplitStage(
            arrayOf(
                SplitStage(arrayOf(SequentialSplitStage(arrayOf(Clear, Boss)))).withName("&bDungeon"),
                BossEnd,
            )
        )
    }
}

class TerminalSection(val terms: Int, val section: Int) : SplitStage() {
    var termsDone = 0
    var deviceDone = false
    var leversDone = 0
    var gateDestroyed = false
    var lastIgn = ""
    var lastIndex = 0
    var lastType = ""
    var ignoreFirst = false
    private val taskRegex = "^(\\w+) (?:activated|completed) a (terminal|lever|device)! \\((\\d)/\\d\\)$".toRegex()

    override fun reset() {
        super.reset()

        termsDone = 0
        deviceDone = false
        leversDone = 0
        gateDestroyed = false
        lastIgn = ""
        lastIndex = 0
        lastType = ""
        ignoreFirst = false
    }

    override fun onChat(msg: String) {
        if (!isActive()) return
        if (ignoreFirst) {
            ignoreFirst = false
            return
        }
        if (msg == "The gate has been destroyed!") gateDestroyed = true
        else {
            val match = taskRegex.matchEntire(msg) ?: return
            val ign = match.groupValues.getOrNull(1) ?: return
            val type = match.groupValues.getOrNull(2) ?: return
            val index = match.groupValues.getOrNull(3)?.toIntOrNull() ?: return
            if (index == lastIndex) {
                if (ign == lastIgn) return
                if (type == "device") {
                    when (section) {
                        1 -> {
                            if (!Stages.S4.deviceDone) Stages.S4.deviceDone = true
                            else if (!Stages.S2.deviceDone) Stages.S2.deviceDone = true
                            else Stages.S3.deviceDone = true
                        }
                        2 -> {
                            if (!Stages.S3.deviceDone) Stages.S3.deviceDone = true
                            else Stages.S4.deviceDone = true
                        }
                        3 -> Stages.S4.deviceDone = true
                    }
                    return
                } else if (lastType == "device") deviceDone = false
            } else if (index == 2 && lastIndex == 0) {
                deviceDone = true
            }
            when (type) {
                "terminal" -> termsDone++
                "lever" -> leversDone++
                "device" -> deviceDone = true
            }
        }

        if (
            termsDone >= terms &&
            leversDone >= 2 &&
            deviceDone &&
            gateDestroyed
        ) {
            stop()
            if (section < 4) {
                (parent!!.children[section] as TerminalSection).ignoreFirst = true
                parent!!.children[section].start()
            }
        }
    }
}