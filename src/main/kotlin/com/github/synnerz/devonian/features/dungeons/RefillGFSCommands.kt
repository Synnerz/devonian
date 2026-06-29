package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.devonian.Devonian
import com.github.synnerz.devonian.api.ChatUtils
import com.github.synnerz.devonian.api.ItemUtils
import com.github.synnerz.devonian.api.Location
import com.github.synnerz.devonian.api.dungeon.DungeonClass
import com.github.synnerz.devonian.api.dungeon.Dungeons
import com.github.synnerz.devonian.commands.DevonianCommand
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.config.Config
import com.github.synnerz.devonian.config.ConfigData

object RefillGFSCommands {
    val inventory get() = Devonian.minecraft.player?.inventory
    val SETTING_TOXIC_KUUDRA = ConfigData.Slider(
        "refill\$ToxicKuudra",
        16.0,
        null,
        0.0, 64.0,
        "Max that it should refill in Kuudra",
        "Toxic Arrow Poison",
    ).also {
        Config.registerCategory(it, Categories.GLOBAL, "Commands")
    }
    val SETTING_TWILIGHT_KUUDRA = ConfigData.Slider(
        "refill\$TwilightKuudra",
        16.0,
        null,
        0.0, 64.0,
        "Max that it should refill in Kuudra",
        "Twilight Arrow Poison",
    ).also {
        Config.registerCategory(it, Categories.GLOBAL, "Commands")
    }
    val SETTING_ARCHER = ConfigData.Slider(
        "refill\$ArcherDungeons",
        16.0,
        null,
        0.0, 64.0,
        "Max that it should refill in Dungeons while class is Archer",
        "Twilight Archer",
    ).also {
        Config.registerCategory(it, Categories.GLOBAL, "Commands")
    }
    val SETTING_BERSERK = ConfigData.Slider(
        "refill\$BerserkDungeons",
        16.0,
        null,
        0.0, 64.0,
        "Max that it should refill in Dungeons while class is Berserk",
        "Twilight Berserk",
    ).also {
        Config.registerCategory(it, Categories.GLOBAL, "Commands")
    }
    val SETTING_HEALER = ConfigData.Slider(
        "refill\$HealerDungeons",
        16.0,
        null,
        0.0, 64.0,
        "Max that it should refill in Dungeons while class is Healer",
        "Twilight Healer",
    ).also {
        Config.registerCategory(it, Categories.GLOBAL, "Commands")
    }
    val SETTING_TANK = ConfigData.Slider(
        "refill\$TankDungeons",
        16.0,
        null,
        0.0, 64.0,
        "Max that it should refill in Dungeons while class is Tank",
        "Twilight Tank",
    ).also {
        Config.registerCategory(it, Categories.GLOBAL, "Commands")
    }
    val SETTING_MAGE = ConfigData.Slider(
        "refill\$MageDungeons",
        16.0,
        null,
        0.0, 64.0,
        "Max that it should refill in Dungeons while class is Mage",
        "Twilight Mage",
    ).also {
        Config.registerCategory(it, Categories.GLOBAL, "Commands")
    }

    fun initialize() {
        DevonianCommand.command.subcommand("pearls") { _, args ->
            val inv = inventory ?: return@subcommand 0
            var stacks = 0
            var amount = 0
            inv.forEach {
                if (ItemUtils.skyblockId(it) != "ENDER_PEARL") return@forEach
                stacks++
                amount += it.count
            }
            if (amount == 0) stacks = 1
            val remaining = stacks * 16 - amount
            if (remaining > 0) ChatUtils.command("gfs ender pearl $remaining")
            1
        }

        DevonianCommand.command.subcommand("leaps") { _, args ->
            val inv = inventory ?: return@subcommand 0
            var stacks = 0
            var amount = 0
            inv.forEach {
                if (ItemUtils.skyblockId(it) != "SPIRIT_LEAP") return@forEach
                stacks++
                amount += it.count
            }
            if (amount == 0) stacks = 1
            val remaining = stacks * 16 - amount
            if (remaining > 0) ChatUtils.command("gfs spirit leap $remaining")
            1
        }

        DevonianCommand.command.subcommand("superbooms") { _, args ->
            val inv = inventory ?: return@subcommand 0
            var stacks = 0
            var amount = 0
            inv.forEach {
                if (ItemUtils.skyblockId(it) != "SUPERBOOM_TNT") return@forEach
                stacks++
                amount += it.count
            }
            if (amount == 0) stacks = 1
            val remaining = stacks * 64 - amount
            if (remaining > 0) ChatUtils.command("gfs superboom tnt $remaining")
            1
        }

        DevonianCommand.command.subcommand("decoys") { _, args ->
            val inv = inventory ?: return@subcommand 0
            var stacks = 0
            var amount = 0
            inv.forEach {
                if (ItemUtils.skyblockId(it) != "DUNGEON_DECOY") return@forEach
                stacks++
                amount += it.count
            }
            if (amount == 0) stacks = 1
            val remaining = stacks * 64 - amount
            if (remaining > 0) ChatUtils.command("gfs decoy $remaining")
            1
        }

        DevonianCommand.command.subcommand("twilight") { _, args ->
            val inv = inventory ?: return@subcommand 0
            var stacks = 0
            var amount = 0
            inv.forEach {
                if (ItemUtils.skyblockId(it) != "TWILIGHT_ARROW_POISON") return@forEach
                stacks++
                amount += it.count
            }
            if (amount == 0) stacks = 1
            val remaining = stacks * maxTwilight() - amount
            if (remaining > 0) ChatUtils.command("gfs twilight arrow poison $remaining")
            1
        }

        DevonianCommand.command.subcommand("toxic") { _, args ->
            val inv = inventory ?: return@subcommand 0
            var stacks = 0
            var amount = 0
            inv.forEach {
                if (ItemUtils.skyblockId(it) != "TOXIC_ARROW_POISON") return@forEach
                stacks++
                amount += it.count
            }
            if (amount == 0) stacks = 1
            val remaining = stacks * maxToxic() - amount
            if (remaining > 0) ChatUtils.command("gfs toxic arrow poison $remaining")
            1
        }
    }

    fun maxTwilight(): Int {
        val inDungeons = Location.area == "catacombs"
        val inKuudra = Location.area == "kuudra"
        if (!inDungeons && !inKuudra) return 64

        if (inKuudra) return SETTING_TWILIGHT_KUUDRA.get().toInt()

        val self = Dungeons.selfPlayer

        return when(self.role) {
            DungeonClass.Archer -> SETTING_ARCHER.get().toInt()
            DungeonClass.Berserk -> SETTING_BERSERK.get().toInt()
            DungeonClass.Healer -> SETTING_HEALER.get().toInt()
            DungeonClass.Mage -> SETTING_MAGE.get().toInt()
            DungeonClass.Tank -> SETTING_TANK.get().toInt()
            else -> 64
        }
    }

    fun maxToxic(): Int {
        if (Location.area != "kuudra") return 64

        return SETTING_TOXIC_KUUDRA.get().toInt()
    }
}