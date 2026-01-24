package com.github.synnerz.devonian.features.misc

import com.github.synnerz.devonian.Devonian
import com.github.synnerz.devonian.api.ChatUtils
import com.github.synnerz.devonian.api.Scheduler
import com.github.synnerz.devonian.api.events.ActionbarEvent
import com.github.synnerz.devonian.api.events.RenderOverlayEvent
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.hud.texthud.TextHudFeature

object ActionbarParser : Feature(
    "actionbarParser",
    "Allows for moving/hiding elements from the actionbar (above your hotbar). " +
    "This is a global toggle. None of the features below will work when this is disabled.",
    subcategory = "Actionbar",
) {
    private val splitReg = "\\s{2,}".toRegex()
    // term laser makes it "§a7,952§a❈ Defense§a§l  T3!"
    private val cringe = listOf("§r", "§a§l", "§6")

    private var updateCount = 0

    override fun initialize() {
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
                "Allows you to move the health that appears in the actionbar (above your hotbar).",
                subcategory = "Actionbar",
                searchTags = customTags,
            ) {
                override fun getEditText(): List<String> = listOf("§616,460/13,607❤")
            },
            object : Feature(
                "hideHealthActionbar",
                "Hides the health that appears in the actionbar (above your hotbar).",
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
                "Allows you to move the defense that appears in the actionbar (above your hotbar).",
                subcategory = "Actionbar",
                searchTags = customTags,
            ) {
                override fun getEditText(): List<String> = listOf("§a7,952§a❈")
            },
            object : Feature(
                "hideDefenseActionbar",
                "Hides the defense that appears in the actionbar (above your hotbar).",
                subcategory = "Actionbar",
                searchTags = hideTags,
            ) {},
        ) {
            override fun modifyStringHud(str: String): String = str.dropLast(" Defense".length)
        },
        Mana(
            null,
            listOf("✎", "ʬ", " Mana"),
            object : TextHudFeature(
                "customManaHud",
                "Allows you to move the mana that appears in the actionbar (above your hotbar).",
                subcategory = "Actionbar",
                searchTags = customTags,
            ) {
                override fun getEditText(): List<String> = listOf("§b469/1,154✎ §3600ʬ")
            },
            object : Feature(
                "hideManaActionbar",
                "Hides the mana that appears in the actionbar (above your hotbar).",
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
                "Allows you to move the mana used '18 Mana (Instant Transmission)' that appears in the actionbar (above your hotbar).",
                subcategory = "Actionbar",
                searchTags = customTags,
            ) {
                override fun getEditText(): List<String> = listOf("§b-18 Mana (§6Instant Transmission§b)")
            },
            object : Feature(
                "hideManaUseActionbar",
                "Hides the mana used '18 Mana (Instant Transmission)' that appears in the actionbar (above your hotbar).",
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
                "Allows you to move the true defense that appears in the actionbar (above your hotbar).",
                subcategory = "Actionbar",
                searchTags = customTags,
            ) {
                override fun getEditText(): List<String> = listOf("§f69§f❂")
            },
            object : Feature(
                "hideTrueDefenseActionbar",
                "Hides the true defense that appears in the actionbar (above your hotbar).",
                subcategory = "Actionbar",
                searchTags = hideTags,
            ) {},
        ) {
            override fun modifyStringHud(str: String): String = str.dropLast(" True Defense".length)
        },
        SkillXP(
            ")",
            null,
            object : TextHudFeature(
                "customStatXpHud",
                "Allows you to move the stat xp '+163.7 Combat (141,560,940/0)' that appears in the actionbar (above your hotbar).",
                subcategory = "Actionbar",
                searchTags = customTags,
            ) {
                override fun getEditText(): List<String> = listOf("§3+163.7 Combat (141,560,940/0)")
            },
            object : Feature(
                "hideStatXpActionbar",
                "Hides the stat xp '+163.7 Combat (141,560,940/0)' that appears in the actionbar (above your hotbar).",
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
                "Allows you to move the secret count that appears in the actionbar (above your hotbar).",
                displayName = "Custom Secret Count Hud",
                subcategory = "Actionbar",
                searchTags = customTags,
            ) {
                override fun getEditText(): List<String> = listOf("§70/4 Secrets")
            },
            object : Feature(
                "hideSecretCountActionbar",
                "Hides the secret count that appears in the actionbar (above your hotbar).",
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
                "Allows you to move the term laser that appears in the actionbar (above your hotbar).",
                subcategory = "Actionbar",
                searchTags = customTags,
            ) {
                override fun getEditText(): List<String> = listOf("§a§lT3!")
            },
            object : Feature(
                "hideTermLaserActionbar",
                "Hides the term laser that appears in the actionbar (above your hotbar).",
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
                "Allows you to move the drill fuel that appears in the actionbar (above your hotbar).",
                subcategory = "Actionbar",
                searchTags = customTags,
            ) {
                override fun getEditText(): List<String> = listOf("§22,609/3k Drill Fuel")
            },
            object : Feature(
                "hideDrillFuelActionbar",
                "Hides the drill fuel that appears in the actionbar (above your hotbar).",
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
                "Allows you to move the armor stacks (nether armor) that appears in the actionbar (above your hotbar).",
                subcategory = "Actionbar",
                searchTags = customTags,
            ) {
                override fun getEditText(): List<String> = listOf("§610ᝐ")
            },
            object : Feature(
                "hideArmorStacksActionbar",
                "Hides the armor stacks (nether armor) that appears in the actionbar (above your hotbar).",
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
                "Allows you to move the rift time that appears in the actionbar (above your hotbar).",
                subcategory = "Actionbar",
                searchTags = customTags,
            ) {
                override fun getEditText(): List<String> = listOf("§a10m59sф Left")
            },
            object : Feature(
                "hideRiftTimeActionbar",
                "Hides the rift time that appears in the actionbar (above your hotbar).",
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
                "Allows you to move the gecko combo that appears in the actionbar (above your hotbar).",
                subcategory = "Actionbar",
                searchTags = customTags,
            ) {
                override fun getEditText(): List<String> = listOf("§a[⬛⬛⬛⬛⬛ §e§lx7 §a⬛⬛⬛⬜⬜]")
            },
            object : Feature(
                "hideGeckoComboActionbar",
                "Hides the gecko combo that appears in the actionbar (above your hotbar).",
                subcategory = "Actionbar",
                searchTags = hideTags,
            ) {},
        );

        var updated = 0
        fun shouldShow(): Boolean = updated >= updateCount

        open fun modifyStringHud(str: String): String = str
        open fun modifyStringVanilla(str: String): String = str
    }
}