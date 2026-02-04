package com.github.synnerz.devonian.features.misc

import com.github.synnerz.devonian.Devonian
import com.github.synnerz.devonian.api.ChatUtils
import com.github.synnerz.devonian.api.Location
import com.github.synnerz.devonian.api.Scheduler
import com.github.synnerz.devonian.api.events.ActionbarEvent
import com.github.synnerz.devonian.api.events.RenderOverlayEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.config.Config
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.hud.texthud.TextHudFeature
import com.github.synnerz.devonian.utils.BasicState

object ActionbarParser : Feature(
    "actionbarParser",
    "Allows for moving/hiding elements from the actionbar (above your hotbar). " +
    "This is a global toggle. None of the features below will work when this is disabled.",
    subcategory = "Actionbar",
) {
    override fun createRequirements(): List<BasicState<Boolean>?> {
        return super.createRequirements() + listOf(Location.stateInSkyblock)
    }

    private val splitReg = "\\s{2,}".toRegex()
    // term laser makes it "§a7,952§a❈ Defense§a§l  T3!"
    private val cringe = listOf("§r", "§a§l", "§6")

    private var updateCount = 0

    override fun initialize() {
        Stats.entries.forEach { it.initialize() }

        on<ActionbarEvent> { event ->
            val str = event.text.string

            val pieces = str.split(splitReg)

            var shouldOverride = false
            val override = buildString {
                pieces.forEach { stat ->
                    var stat = stat
                    cringe.find { stat.endsWith(it) }?.let {
                        stat = stat.dropLast(it.length)
                    }

                    val parsed = Stats.entries.find {
                        (
                            it.endsWith?.let { stat.endsWith(it) } ?:
                            it.endsWithL?.any { stat.endsWith(it) } ?:
                            true
                        ) && (it.startsWith?.let { stat.startsWith(it) } ?: true)
                    }
                    if (parsed == null) {
                        if (Devonian.isDev) println("ActionbarParser: unknown stat '$stat'")
                        append(" ".repeat(5))
                        append(stat)
                    } else {
                        parsed.updated = updateCount + 1

                        if (parsed.custom.isEnabled()) Scheduler.scheduleTask {
                            parsed.custom.setLine(parsed.modifyStringHud(stat))
                        }

                        if (parsed.hide.isEnabled()) shouldOverride = true
                        else {
                            append(parsed.pad)
                            append(parsed.modifyStringVanilla(stat))
                        }
                    }
                }
            }

            updateCount++

            if (!shouldOverride) return@on
            event.cancel()
            ChatUtils.sendActionbar(override)
        }

        on<RenderOverlayEvent> { event ->
            Stats.entries.forEach {
                if (!it.custom.isEnabled()) return@forEach
                if (!it.shouldShow()) return@forEach
                it.custom.draw(event.ctx)
            }
        }
    }

    private const val desc = "Requires 'Actionbar Parser' to be enabled."
    private val customTags = setOf("actionbar")
    private val hideTags = setOf<String>()
    enum class Stats(
        val endsWith: String?,
        val endsWithL: List<String>?,
        val custom: TextHudFeature,
        val hide: Feature,
        val pad: String = " ".repeat(5),
        val startsWith: String? = null,
    ) {
        Health(
            "❤",
            null,
            object : TextHudFeature(
                "customHealthHud",
                "$desc Allows you to move the health that appears in the actionbar (above your hotbar).",
                subcategory = "Actionbar",
                searchTags = customTags,
            ) {
                override fun getEditText(): List<String> = listOf("§616,460/13,607❤")
            },
            object : Feature(
                "hideHealthActionbar",
                "$desc Hides the health that appears in the actionbar (above your hotbar).",
                subcategory = "Actionbar",
                searchTags = hideTags,
            ) {},
            "",
        ),
        Defense(
            "❈ Defense",
            null,
            object : TextHudFeature(
                "customDefenseHud",
                "$desc Allows you to move the defense that appears in the actionbar (above your hotbar).",
                subcategory = "Actionbar",
                searchTags = customTags,
            ) {
                override fun getEditText(): List<String> = listOf("§a7,952§a❈")
            },
            object : Feature(
                "hideDefenseActionbar",
                "$desc Hides the defense that appears in the actionbar (above your hotbar).",
                subcategory = "Actionbar",
                searchTags = hideTags,
            ) {},
        ) {
            val SETTING_ALWAYS_SHOW = custom.addSwitch(
                "alwaysShow",
                true,
                "Render the hud even when defense is not listed in the actionbar.",
                "Always Show",
            ).also {
                Config.categories[custom.category]!![it.subcategory]!!.also { arr ->
                    arr.add(arr.size - 2, arr.removeLast())
                }
            }
            var ignore = 0

            override fun initialize() {
                on<WorldChangeEvent> {
                    ignore = updateCount
                }
            }

            override fun shouldShow(): Boolean =
                if (SETTING_ALWAYS_SHOW.get()) ignore != updateCount
                else super.shouldShow()

            override fun modifyStringHud(str: String): String = str.dropLast(" Defense".length)
        },
        Mana(
            null,
            listOf("✎", "ʬ", " Mana"),
            object : TextHudFeature(
                "customManaHud",
                "$desc Allows you to move the mana that appears in the actionbar (above your hotbar).",
                subcategory = "Actionbar",
                searchTags = customTags,
            ) {
                override fun getEditText(): List<String> = listOf("§b469/1,154✎ §3600ʬ")
            },
            object : Feature(
                "hideManaActionbar",
                "$desc Hides the mana that appears in the actionbar (above your hotbar).",
                subcategory = "Actionbar",
                searchTags = hideTags,
            ) {},
        ) {
            override fun modifyStringHud(str: String): String {
                return if (str.endsWith(" Mana")) str.dropLast(" Mana".length)
                    else str
            }
        },
        ManaUse(
            ")",
            null,
            object : TextHudFeature(
                "customManaUseHud",
                "$desc Allows you to move the mana used '18 Mana (Instant Transmission)' that appears in the actionbar (above your hotbar).",
                subcategory = "Actionbar",
                searchTags = customTags,
            ) {
                override fun getEditText(): List<String> = listOf("§b-18 Mana (§6Instant Transmission§b)")
            },
            object : Feature(
                "hideManaUseActionbar",
                "$desc Hides the mana used '18 Mana (Instant Transmission)' that appears in the actionbar (above your hotbar).",
                subcategory = "Actionbar",
                searchTags = hideTags,
            ) {},
            startsWith = "§b",
        ),
        TrueDefense(
            "❈ True Defense",
            null,
            object : TextHudFeature(
                "customTrueDefenseHud",
                "$desc Allows you to move the true defense that appears in the actionbar (above your hotbar).",
                subcategory = "Actionbar",
                searchTags = customTags,
            ) {
                override fun getEditText(): List<String> = listOf("§f69§f❂")
            },
            object : Feature(
                "hideTrueDefenseActionbar",
                "$desc Hides the true defense that appears in the actionbar (above your hotbar).",
                subcategory = "Actionbar",
                searchTags = hideTags,
            ) {},
        ) {
            val SETTING_ALWAYS_SHOW = custom.addSwitch(
                "alwaysShow",
                true,
                "Render the hud even when true defense is not listed in the actionbar.",
                "Always Show",
            ).also {
                Config.categories[custom.category]!![it.subcategory]!!.also { arr ->
                    arr.add(arr.size - 2, arr.removeLast())
                }
            }
            var ignore = 0

            override fun initialize() {
                on<WorldChangeEvent> {
                    ignore = updateCount
                }
            }

            override fun shouldShow(): Boolean =
                if (SETTING_ALWAYS_SHOW.get()) ignore != updateCount
                else super.shouldShow()

            override fun modifyStringHud(str: String): String = str.dropLast(" True Defense".length)
        },
        SkillXP(
            ")",
            null,
            object : TextHudFeature(
                "customStatXpHud",
                "$desc Allows you to move the stat xp '+163.7 Combat (141,560,940/0)' that appears in the actionbar (above your hotbar).",
                subcategory = "Actionbar",
                searchTags = customTags,
            ) {
                override fun getEditText(): List<String> = listOf("§3+163.7 Combat (141,560,940/0)")
            },
            object : Feature(
                "hideStatXpActionbar",
                "$desc Hides the stat xp '+163.7 Combat (141,560,940/0)' that appears in the actionbar (above your hotbar).",
                subcategory = "Actionbar",
                searchTags = hideTags,
            ) {},
            startsWith = "§3",
        ),
        Secrets(
            " Secrets",
            null,
            object : TextHudFeature(
                "secretsHud",
                "$desc Allows you to move the secret count that appears in the actionbar (above your hotbar).",
                displayName = "Custom Secret Count Hud",
                subcategory = "Actionbar",
                searchTags = customTags,
            ) {
                override fun getEditText(): List<String> = listOf("§70/4 Secrets")
            },
            object : Feature(
                "hideSecretCountActionbar",
                "$desc Hides the secret count that appears in the actionbar (above your hotbar).",
                subcategory = "Actionbar",
                searchTags = hideTags,
            ) {},
        ) {
            override fun modifyStringHud(str: String): String = str.dropLast(" Secrets".length)
        },
        TermLaser(
            null,
            listOf("T1", "T2", "T3!"),
            object : TextHudFeature(
                "customTermLaserHud",
                "$desc Allows you to move the term laser that appears in the actionbar (above your hotbar).",
                subcategory = "Actionbar",
                searchTags = customTags,
            ) {
                override fun getEditText(): List<String> = listOf("§a§lT3!")
            },
            object : Feature(
                "hideTermLaserActionbar",
                "$desc Hides the term laser that appears in the actionbar (above your hotbar).",
                subcategory = "Actionbar",
                searchTags = hideTags,
            ) {},
            " ".repeat(2),
        ) {
            fun modifyString(str: String): String {
                return when (str) {
                    "T1",
                    "T2" -> "§6"
                    "T3!" -> "§a§l"
                    else -> ""
                } + str
            }

            override fun modifyStringHud(str: String): String = modifyString(str)
            override fun modifyStringVanilla(str: String): String = modifyString(str) + "§r"
        },
        DrillFuel(
            " Drill Fuel",
            null,
            object : TextHudFeature(
                "customDrillFuelHud",
                "$desc Allows you to move the drill fuel that appears in the actionbar (above your hotbar).",
                subcategory = "Actionbar",
                searchTags = customTags,
            ) {
                override fun getEditText(): List<String> = listOf("§22,609/3k Drill Fuel")
            },
            object : Feature(
                "hideDrillFuelActionbar",
                "$desc Hides the drill fuel that appears in the actionbar (above your hotbar).",
                subcategory = "Actionbar",
                searchTags = hideTags,
            ) {},
        ) {
            override fun modifyStringHud(str: String): String = str.dropLast(" Drill Fuel".length)
        },
        ArmorStacks(
            null,
            listOf("ᝐ", "⁑", "⚶", "Ѫ", "҉"),
            object : TextHudFeature(
                "customArmorStacksHud",
                "$desc Allows you to move the armor stacks (nether armor) that appears in the actionbar (above your hotbar).",
                subcategory = "Actionbar",
                searchTags = customTags,
            ) {
                override fun getEditText(): List<String> = listOf("§610ᝐ")
            },
            object : Feature(
                "hideArmorStacksActionbar",
                "$desc Hides the armor stacks (nether armor) that appears in the actionbar (above your hotbar).",
                subcategory = "Actionbar",
                searchTags = hideTags,
            ) {},
            " ".repeat(2),
        ) {
            override fun modifyStringVanilla(str: String): String = "$str§r"
        },
        RiftTime(
            " Left",
            null,
            object : TextHudFeature(
                "customRiftTimeHud",
                "$desc Allows you to move the rift time that appears in the actionbar (above your hotbar).",
                subcategory = "Actionbar",
                searchTags = customTags,
            ) {
                override fun getEditText(): List<String> = listOf("§a10m59sф Left")
            },
            object : Feature(
                "hideRiftTimeActionbar",
                "$desc Hides the rift time that appears in the actionbar (above your hotbar).",
                subcategory = "Actionbar",
                searchTags = hideTags,
            ) {},
        ) {
            override fun modifyStringHud(str: String): String = str.dropLast(" Left".length)
        },
        GeckoCombo(
            "]",
            null,
            object : TextHudFeature(
                "customGeckoComboHud",
                "$desc Allows you to move the gecko combo that appears in the actionbar (above your hotbar).",
                subcategory = "Actionbar",
                searchTags = customTags,
            ) {
                override fun getEditText(): List<String> = listOf("§a[⬛⬛⬛⬛⬛ §e§lx7 §a⬛⬛⬛⬜⬜]")
            },
            object : Feature(
                "hideGeckoComboActionbar",
                "$desc Hides the gecko combo that appears in the actionbar (above your hotbar).",
                subcategory = "Actionbar",
                searchTags = hideTags,
            ) {},
        ),
        EssenceGained(
            " Essence",
            null,
            object : TextHudFeature(
                "customEssenceGainedHud",
                "$desc Allows you to move the essence gained that appears in the actionbar (above your hotbar).",
                subcategory = "Actionbar",
                searchTags = customTags,
            ) {
                override fun getEditText(): List<String> = listOf("§d+1 Wither Essence")
            },
            object : Feature(
                "hideEssenceGainedActionbar",
                "$desc Hides the essence gained that appears in the actionbar (above your hotbar).",
                subcategory = "Actionbar",
                searchTags = hideTags,
            ) {},
        ),
        RagAxeTimer(
            null,
            listOf("CASTING IN 3s", "CASTING IN 2s", "CASTING IN 1s", "CASTING", "CANCELLED"),
            object : TextHudFeature(
                "customRagAxeTimerHud",
                "$desc Allows you to move the rag axe timer that appears in the actionbar (above your hotbar).",
                subcategory = "Actionbar",
                searchTags = customTags,
            ) {
                override fun getEditText(): List<String> = listOf("§a§lCASTING IN 3s")
            },
            object : Feature(
                "hideRagAxeTimerActionbar",
                "$desc Hides the rag axe timer that appears in the actionbar (above your hotbar).",
                subcategory = "Actionbar",
                searchTags = hideTags,
            ) {},
        ) {
            override fun modifyStringHud(str: String): String =
                if (str.endsWith("CANCELLED")) str
                else "§a$str"
            override fun modifyStringVanilla(str: String): String = "${modifyStringHud(str)}§r"
        },
        AuroraStaffRune(
            null,
            listOf("Defender", "Virtuoso", "Mediator"),
            object : TextHudFeature(
                "customAuroraStaffRuneHud",
                "$desc Allows you to move the aurora staff rune that appears in the actionbar (above your hotbar).",
                subcategory = "Actionbar",
                searchTags = customTags,
            ) {
                override fun getEditText(): List<String> = listOf("§5§lVirtuoso")
            },
            object : Feature(
                "hideAuroraStaffRuneActionbar",
                "$desc Hides the aurora staff rune that appears in the actionbar (above your hotbar).",
                subcategory = "Actionbar",
                searchTags = hideTags,
            ) {},
        ) {
            override fun modifyStringVanilla(str: String): String = "$str§r"
        },
        SoulEsoward(
            "INVULNERABLE",
            null,
            object : TextHudFeature(
                "customSoulEsowardHud",
                "$desc Allows you to move the soul esoward's 'IMMUNITY' that appears in the actionbar (above your hotbar).",
                subcategory = "Actionbar",
                searchTags = customTags,
            ) {
                override fun getEditText(): List<String> = listOf("§9§lINVULNERABLE")
            },
            object : Feature(
                "hideSoulEsowardActionbar",
                "$desc Hides the soul esoward's 'IMMUNITY' that appears in the actionbar (above your hotbar).",
                subcategory = "Actionbar",
                searchTags = hideTags,
            ) {},
        ) {
            override fun modifyStringVanilla(str: String): String = "$str§r"
        },
        BitsGained(
            " Bits from Cookie Buff!",
            null,
            object : TextHudFeature(
                "customBitsGainedHud",
                "$desc Allows you to move the bits gained that appears in the actionbar (above your hotbar).",
                subcategory = "Actionbar",
                searchTags = customTags,
            ) {
                override fun getEditText(): List<String> = listOf("§9§lINVULNERABLE")
            },
            object : Feature(
                "hideBitsGainedActionbar",
                "$desc Hides the bits gained that appears in the actionbar (above your hotbar).",
                subcategory = "Actionbar",
                searchTags = hideTags,
            ) {},
            pad = "",
        );

        open fun initialize() {}

        var updated = 0
        open fun shouldShow(): Boolean = updated >= updateCount

        open fun modifyStringHud(str: String): String = str
        open fun modifyStringVanilla(str: String): String = str
    }
}