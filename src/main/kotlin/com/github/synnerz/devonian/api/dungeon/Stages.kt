package com.github.synnerz.devonian.api.dungeon

import com.github.synnerz.devonian.api.ChatUtils
import com.github.synnerz.devonian.api.splits.*
import com.github.synnerz.devonian.commands.DevonianCommand
import com.github.synnerz.devonian.features.dungeons.BossSplits
import com.github.synnerz.devonian.features.dungeons.clear.RunSplits

object Stages {
    val Root: SequentialSplitStage

    val Clear: SplitStage

    val BloodOpen = SplitStage().withName("&4Blood").withLongTime()
    val FirstWatcherSpawn = SplitStage().withName("&cFirst Spawn").withLongTime()
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
    val Maxor = SplitStage().withName("&5Maxor")
    val Storm: SplitStage
    val StormLightning = SplitStage()
    val Terminals: SequentialSplitStage
    val S1 = TerminalSection(4, 1).also { it.withName("&eS1").withLongTime() }
    val S2 = TerminalSection(5, 2).also { it.withName("&eS2").withLongTime() }
    val S3 = TerminalSection(4, 3).also { it.withName("&eS3").withLongTime() }
    val S4 = TerminalSection(4, 4).also { it.withName("&eS4").withLongTime() }
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
                        SplitStage(
                            "The BLOOD DOOR has been opened!",
                            arrayOf(
                                SequentialSplitStage(
                                    arrayOf(
                                        FirstWatcherSpawn,
                                        SplitStage("[BOSS] The Watcher: Let's see how you can handle this.")
                                    )
                                ),
                                WatcherClear
                            )
                        ),
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

        Storm = SequentialSplitStage(
            "[BOSS] Storm: Pathetic Maxor, just like expected.",
            arrayOf(
                StormLightning,
                BranchingSplitStage(
                    arrayOf(
                        SplitStage("[BOSS] Storm: ENERGY HEED MY CALL!"),
                        SplitStage("[BOSS] Storm: THUNDER LET ME BE YOUR CATALYST!"),
                    )
                )
            )
        ).withName("&9Storm").withLongTime()
        Terminals = SequentialSplitStage(
            "[BOSS] Goldor: Who dares trespass into my domain?",
            arrayOf(S1, S2, S3, S4),
        ).also { it.withName("&6Terminals") }
        Goldor = SplitStage("The Core entrance is opening!").withName("&8Goldor")
        Necron = SplitStage("[BOSS] Necron: You went further than any human before, congratulations.")
            .withName("&4Necron")
        WitherKing = object : SplitStage("[BOSS] Necron: All this, for nothing...") {
            override fun getThisSplit(format: TimeUnit.Format, force: TimeUnit?): MutableList<String> {
                if (Dungeons.floor == FloorType.F7) return mutableListOf()
                return super.getThisSplit(format, force)
            }
        }.also {
            it.withName("&0Wither King")
        }

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

    fun initialize() {
        val stageNames = mapOf(
            "clear" to Clear,
            "blood" to WatcherClear,
            "portal" to PortalEnter,
            "f1" to F1,
            "f2" to F2,
            "f3" to F3,
            "f4" to F4,
            "f5" to F5,
            "f6" to F6,
            "f7" to F7,
            "maxor" to Maxor,
            "storm" to Storm,
            "stormlightning" to StormLightning,
            "terminals" to Terminals,
            "s1" to S1,
            "s2" to S2,
            "s3" to S3,
            "s4" to S4,
            "goldor" to Goldor,
            "necron" to Necron,
            "witherking" to WitherKing,
        )

        DevonianCommand.command.subcommand("setsplit") { _, args ->
            val name = args.firstOrNull()?.toString() ?: return@subcommand 0
            val stage = stageNames[name]
            if (stage == null) {
                ChatUtils.sendMessage("§4Unknown split: $name", true)
                return@subcommand 0
            }
            Root.reset()
            val q = ArrayDeque<SplitStage>()
            var c = stage
            while (c != null) {
                q.add(c)
                c = c.parent
            }
            while (q.isNotEmpty()) {
                q.removeLast().start()
            }
            ChatUtils.sendMessage("§aSet split to: $name", true)
            return@subcommand 1
        }.greedyString("split").suggest("split", *stageNames.keys.toTypedArray())
    }
}

class TerminalSection(val terms: Int, val section: Int) : SplitStage() {
    var termsDone = 0
    var deviceDone = false
    var leversDone = 0
    var gateDestroyed = section == 4
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
        gateDestroyed = section == 4
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
            } else if (index == 1 && type != "device") {
                deviceDone = false
            }

            when (type) {
                "terminal" -> termsDone++
                "lever" -> leversDone++
                "device" -> {
                    if (deviceDone && section == 2) Stages.S4.deviceDone = true
                    deviceDone = true
                }
            }

            lastIgn = ign
            lastIndex = index
            lastType = type
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