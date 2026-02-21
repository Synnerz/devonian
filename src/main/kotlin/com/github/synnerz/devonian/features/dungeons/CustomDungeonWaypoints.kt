package com.github.synnerz.devonian.features.dungeons

import com.github.synnerz.devonian.api.ChatUtils
import com.github.synnerz.devonian.api.Scheduler
import com.github.synnerz.devonian.api.dungeon.DungeonEvent
import com.github.synnerz.devonian.api.dungeon.DungeonRoom
import com.github.synnerz.devonian.api.dungeon.DungeonScanner
import com.github.synnerz.devonian.api.dungeon.Dungeons
import com.github.synnerz.devonian.api.dungeon.mapEnums.CheckmarkTypes
import com.github.synnerz.devonian.api.events.RenderWorldEvent
import com.github.synnerz.devonian.api.events.UseItemOnEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.commands.DevonianCommand
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.config.Config
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.mixin.accessor.ScreenAccessor
import com.github.synnerz.devonian.utils.PersistentJson
import com.github.synnerz.devonian.utils.PersistentJsonClass
import com.github.synnerz.devonian.utils.render.Render3DImmediate
import com.google.gson.reflect.TypeToken
import com.mojang.blaze3d.platform.InputConstants
import com.mojang.brigadier.context.CommandContext
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.client.gui.components.ChatComponent
import net.minecraft.world.level.block.Block
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.phys.shapes.VoxelShape
import org.lwjgl.glfw.GLFW
import java.awt.Color
import java.util.*
import kotlin.math.abs

object CustomDungeonWaypoints : Feature(
    "customDungeonWaypoints",
    "Enables custom dungeon waypoints do /dv cdw help or /dv dwc help.",
    Categories.DUNGEONS,
    "catacombs",
    subcategory = "Highlights",
) {
    private val SETTING_REMOVE_ON_COLLECT = addSwitch(
        "removeOnCollect",
        true,
        "Removes the secret waypoints that you've already collected/clicked/killed/etherwarped onto.",
        "CDW Remove Collected",
    )
    private val SETTING_LINE_WIDTH = addSlider(
        "lineWidth",
        3.0,
        1.0, 10.0,
        "Line width for each outline.",
        "CDW Line Width",
    )
    private val SETTING_PHASE_MODE = addSwitch(
        "phaseMode",
        false,
        "Whether or not to see through walls the waypoints.",
        "CDW Phase Mode",
    )
    private val SETTING_TEXT_PHASE_MODE = addSwitch(
        "textPhaseMode",
        true,
        "Whether or not to see through walls the waypoints' text.",
        "CDW Text Phase Mode",
    )
    private val SETTING_ORDERED_ETHERS = addSwitch(
        "orederedEthers",
        true,
        "Displays numbers above each etherwarp type waypoint depending on when they were added to the list (first ether waypoint will display 1 etc).",
        "CDW Ordered Ethers",
    )
    private val SETTING_RENDER_TEXT = addSwitch(
        "renderText",
        true,
        "Whether waypoints should render their text (not: if ordered ethers is enabled it will override this).",
        "CDW Render Text",
    )
    private val SETTING_REMOVE_ON_DONE = addSwitch(
        "removeOnDone",
        false,
        "Removes all the waypoints of the current room if its Green Check Marked",
        "CDW Remove On Done"
    )
    private const val KEY = "currentDungeonProfile"
    private const val BOSS_ID = 1000 // 1000 + floor number for each roomId that is boss
    private val waypointData = object : PersistentJsonClass<MutableList<WaypointProfile>>(
        "devonian/customdungeonwaypoints.json",
        object : TypeToken<MutableList<WaypointProfile>>() {},
    ) {
        override fun onLoadDefault() {
            data = mutableListOf(WaypointProfile("default", mutableListOf()))
        }
    }
    private var editMode = false
    private val clearedRooms = mutableListOf<Int>()
    private var currentProfile = "default"
    private var currentRoom: Int? = null
    private var currentParent: ParentWaypoint? = null
    private var currentWaypointType = WaypointType.CHEST
    private var textPos: Triple<Int, Int, Int>? = null

    data class WaypointProfile(val name: String, val parents: MutableList<ParentWaypoint>) {
        fun onRoomEnter(room: DungeonRoom) {
            if (!room.hasRotation()) return
            parents.find { it.id == room.roomID }?.onRoomEnter(room)
        }

        fun resetId(roomId: Int) {
            parents.find { it.id == roomId }?.reset()
        }

        fun reset() {
            parents.forEach { it.reset() }
        }
    }

    data class ParentWaypoint(val id: Int, val waypoints: MutableList<ComponentWaypointPosition>) {
        fun onRoomEnter(room: DungeonRoom) {
            waypoints.forEach { it.onRoomEnter(room) }
            currentParent = this
        }

        fun reset() {
            waypoints.forEach { it.reset() }
        }
    }

    data class WorldWaypointPosition(val x: Int, val y: Int, val z: Int)
    data class ComponentWaypointPosition(
        val cx: Int,
        val cy: Int,
        val cz: Int,
        val type: WaypointType,
        var text: String? = null,
        @Transient var clicked: Boolean = false,
    ) {
        @Transient
        var cachedPos: WorldWaypointPosition? = null

        fun onRoomEnter(room: DungeonRoom) {
            if (cachedPos != null) return
            val roomPos = room.fromComp(cx, cz) ?: return
            cachedPos = WorldWaypointPosition(roomPos.first, cy, roomPos.second)
        }

        fun pos(): WorldWaypointPosition? = cachedPos

        fun reset() {
            cachedPos = null
            clicked = false
        }
    }

    enum class WaypointType(val shape: VoxelShape = Shapes.block()) {
        CHEST(Block.column(14.0, 0.0, 14.0)),
        ITEM(Block.column(8.0, 0.0, 8.0)),
        BAT(Block.column(8.0, 0.0, 8.0)),
        ESSENCE(Block.column(8.0, 0.0, 8.0)),
        REDSTONE(Block.column(8.0, 0.0, 8.0)),
        ETHERWARP,
        ETHERWARPPEARL,
        DOUBLEPEARL,
        TEXT,
        MINE,
        LEVER(Block.column(6.0, 8.0, 10.0, 16.0)),
        SUPERBOOM;

        companion object {
            fun byName(name: String) =
                WaypointType.entries.find { it.name == name.uppercase() }
        }
    }

    override fun initialize() {
        Config.set(KEY, "default")

        waypointData.load()
        if (waypointData.data?.isEmpty() != false) waypointData.onLoadDefault()

        Config.onAfterLoad {
            currentProfile = Config.get<String>(KEY) ?: "default"
            if (waypointData.data!!.find { it.name == currentProfile } == null) {
                waypointData.data!!.add(WaypointProfile(currentProfile, mutableListOf()))
            }
        }

        Config.onPreSave {
            Config.set(KEY, currentProfile)
        }

        DevonianCommand.command.subcommand("cdw", true, ::onCommand)
            .word("CommandType")
            .word("other1")
            .word("other2")
            .suggest(
                "CommandType",
                *listOf(
                    "import",
                    "export",
                    "profiles",
                    "create",
                    "createbase",
                    "setprofile",
                    "delprofile",
                    "switch",
                    "settext",
                    "help"
                ).toTypedArray()
            )
        DevonianCommand.command.subcommand("dwc", true, ::onCommand)
            .word("CommandType")
            .word("other1")
            .word("other2")
            .suggest(
                "CommandType",
                *listOf(
                    "import",
                    "export",
                    "profiles",
                    "create",
                    "createbase",
                    "setprofile",
                    "delprofile",
                    "switch",
                    "settext",
                    "help"
                ).toTypedArray()
            )

        on<DungeonEvent.RoomEnter> { event ->
            waypointData.data!!.forEach { it.onRoomEnter(event.room) }
            currentRoom = event.room.roomID
            currentParent = waypointData.data!!
                .find { it.name == currentProfile.lowercase() }?.parents?.find { it.id == currentRoom }
                ?: ParentWaypoint(currentRoom!!, mutableListOf())
        }

        on<DungeonEvent.BossRoomEnter> { event ->
            currentRoom = BOSS_ID + event.floor.floorNum
            currentParent = waypointData.data!!
                .find { it.name == currentProfile.lowercase() }?.parents?.find { it.id == currentRoom }
                ?: ParentWaypoint(currentRoom!!, mutableListOf())
            currentParent?.waypoints?.forEach {
                it.cachedPos = WorldWaypointPosition(it.cx, it.cy, it.cz)
            }
        }

        on<RenderWorldEvent> { event ->
            if (Dungeons.inBoss.value && currentRoom != null && currentRoom!! < BOSS_ID) return@on
            if (SETTING_REMOVE_ON_DONE.get() && clearedRooms.contains(currentRoom)) return@on

            currentParent?.waypoints?.forEach {
                if (SETTING_REMOVE_ON_COLLECT.get() && it.clicked) return@forEach

                val pos = it.pos() ?: return@forEach
                val color = when (it.type) {
                    WaypointType.CHEST -> Color(0, 255, 0, 255)
                    WaypointType.ITEM -> Color(0, 0, 255, 255)
                    WaypointType.BAT -> Color(0, 255, 150, 255)
                    WaypointType.ESSENCE -> Color(255, 0, 255, 255)
                    WaypointType.REDSTONE -> Color(255, 0, 0, 255)
                    WaypointType.ETHERWARP -> Color(0, 255, 255, 255)
                    WaypointType.ETHERWARPPEARL -> Color(172, 0, 249)
                    WaypointType.DOUBLEPEARL -> Color(249, 117, 0)
                    WaypointType.TEXT -> Color(0, 0, 0, 0)
                    WaypointType.MINE -> Color(230, 250, 50, 255)
                    WaypointType.LEVER -> Color(0, 150, 255, 255)
                    WaypointType.SUPERBOOM -> Color(255, 0, 0, 255)
                }

                if (it.text != null && SETTING_RENDER_TEXT.get()) {
                    Render3DImmediate.renderString(
                        it.text!!,
                        pos.x + 0.5, pos.y + 1.5, pos.z + 0.5,
                        2f,
                        phase = SETTING_TEXT_PHASE_MODE.get()
                    )
                } else if (it.type == WaypointType.ETHERWARP && SETTING_ORDERED_ETHERS.get()) {
                    // TODO: make more efficient
                    val idx = currentParent!!.waypoints.filter { f -> f.type == WaypointType.ETHERWARP }.indexOf(it)
                    if (idx != -1) {
                        Render3DImmediate.renderString(
                            "${idx + 1}",
                            pos.x + 0.5, pos.y + 1.5, pos.z + 0.5,
                            2f,
                            phase = SETTING_TEXT_PHASE_MODE.get()
                        )
                    }
                }
                if (it.type == WaypointType.TEXT) return@forEach

                Render3DImmediate.renderWireframeShape(
                    it.type.shape,
                    pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble(),
                    color,
                    phase = SETTING_PHASE_MODE.get(),
                    lineWidth = SETTING_LINE_WIDTH.get()
                )
            }
        }

        on<UseItemOnEvent> { event ->
            if (!editMode || currentRoom == null) return@on

            val bp = event.blockHitResult.blockPos ?: return@on
            val room = DungeonScanner.currentRoom ?: return@on
            if (currentRoom!! < BOSS_ID && room.roomID != currentRoom) return@on
            val isBoss = currentRoom!! > BOSS_ID
            val comps =
                if (isBoss) bp.x to bp.z
                else room.fromPos(bp.x, bp.z) ?: return@on

            val profile = waypointData.data!!.find { it.name == currentProfile.lowercase() } ?: return@on
            var shouldAdd = currentParent != null && !profile.parents.contains(currentParent)
            if (currentParent == null) {
                currentParent = ParentWaypoint(currentRoom!!, mutableListOf())
                shouldAdd = true
            }

            val pos = ComponentWaypointPosition(
                comps.first,
                when (currentWaypointType) {
                    WaypointType.ITEM -> bp.y + 1
                    WaypointType.BAT -> bp.y - 1
                    else -> bp.y
                },
                comps.second,
                currentWaypointType,
            )
            if (InputConstants.isKeyDown(minecraft.window, GLFW.GLFW_KEY_LEFT_SHIFT)) {
                textPos = Triple(comps.first, bp.y, comps.second)
                Scheduler.scheduleTask {
                    minecraft.openChatScreen(ChatComponent.ChatMethod.COMMAND)
                    Scheduler.scheduleTask {
                        (minecraft.screen as ScreenAccessor?)?.insertText("/dv cdw settext ", true)
                    }
                }
            }
            if (!shouldAdd && InputConstants.isKeyDown(minecraft.window, GLFW.GLFW_KEY_LEFT_CONTROL)) {
                if (currentParent!!.waypoints.removeIf { it.cx == pos.cx && it.cy == pos.cy && it.cz == pos.cz }) {
                    if (!isBoss) profile.onRoomEnter(room)
                    else {
                        currentParent = waypointData.data!!
                            .find { it.name == currentProfile.lowercase() }
                            ?.parents
                            ?.find { it.id > BOSS_ID }
                        currentParent?.waypoints?.forEach {
                            it.cachedPos = WorldWaypointPosition(it.cx, it.cy, it.cz)
                        }
                    }
                    ChatUtils.sendMessage(
                        "&cCDW Removed &b$currentWaypointType &b[${pos.cx}, ${pos.cy}, ${pos.cz}]",
                        true
                    )
                }
                return@on
            }

            if (!currentParent!!.waypoints.contains(pos))
                currentParent!!.waypoints.add(pos)

            if (shouldAdd) profile.parents.add(currentParent!!)
            ChatUtils.sendMessage("&aCDW Added &b$currentWaypointType &b[${pos.cx}, ${pos.cy}, ${pos.cz}]", true)
            if (isBoss) {
                currentParent = waypointData.data!!
                    .find { it.name == currentProfile.lowercase() }
                    ?.parents
                    ?.find { it.id >= BOSS_ID }
                currentParent?.waypoints?.forEach {
                    it.cachedPos = WorldWaypointPosition(it.cx, it.cy, it.cz)
                }
                return@on
            }
            profile.onRoomEnter(room)
        }

        on<DungeonEvent.SecretClicked> { event -> onSecret(event.x, event.y, event.z, 0) }
        on<DungeonEvent.SecretBat> { event -> onSecret(event.x, event.y, event.z, 1) }
        on<DungeonEvent.SecretPickup> { event -> onSecret(event.x, event.y, event.z, 2) }

        on<DungeonEvent.RoomUpdateEvent> { event ->
            if (event.previousCheck != CheckmarkTypes.WHITE || event.currentCheck != CheckmarkTypes.GREEN) return@on
            event.room.roomID?.let { clearedRooms.add(it) }
        }
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        waypointData.data!!.forEach { it.reset() }
        clearedRooms.clear()
    }

    private fun onCommand(ctx: CommandContext<FabricClientCommandSource>, args: List<Any>): Int {
        if (args.isEmpty()) {
            if (!isEnabled()) {
                ChatUtils.sendMessage("&cCDW feature is not enabled.", true)
                return 0
            }
            editMode = !editMode
            ChatUtils.sendMessage("&bCDW Edit mode was ${if (editMode) "&aEnabled" else "&cDisabled"}", true)
            return 1
        }
        val commandMode = args.first() as String

        when (commandMode) {
            "create" -> {
                val waypointName = args.getOrNull(1) as? String?
                if (waypointName == null) {
                    ChatUtils.sendMessage("&cCDW Invalid waypoint name", true)
                    return 0
                }

                waypointData.data!!.add(WaypointProfile(waypointName.lowercase(), mutableListOf()))
                currentProfile = waypointName.lowercase()
                setCurrentParent()
                ChatUtils.sendMessage(
                    "&bCDW Successfully created waypoint profile with name &a$waypointName &7(this profile has been set as the current one)",
                    true
                )
            }

            "profiles" -> {
                ChatUtils.sendMessage("&bCDW current profiles are&f: &a${waypointData.data!!.joinToString { it.name }}", true)
            }

            "createbase" -> {
                val waypointName1 = args.getOrNull(1) as? String?
                val waypointName2 = args.getOrNull(2) as? String?
                if (waypointName1.isNullOrEmpty() || waypointName2.isNullOrEmpty()) {
                    ChatUtils.sendMessage("&cCDW Invalid waypoint name", true)
                    return 0
                }
                val profile1 = waypointData.data!!.find { it.name == waypointName1 }
                if (profile1 == null) {
                    ChatUtils.sendMessage("&cCDW Profile \"$waypointName1\" does not exist", true)
                    return 0
                }
                val profile2 = WaypointProfile(waypointName2.lowercase(), mutableListOf())

                profile1.parents.forEach { profile2.parents.add(it) }
                waypointData.data!!.add(profile2)
                currentProfile = profile2.name
                setCurrentParent()

                ChatUtils.sendMessage(
                    "&bCDW Successfully created profile &a$waypointName2&b with base profile as &a$waypointName1 &7(this profile has been set as the current one)",
                    true
                )
            }

            "setprofile" -> {
                val waypointName = args.getOrNull(1) as? String?
                if (waypointName == null) {
                    ChatUtils.sendMessage("&cCDW Invalid waypoint name", true)
                    return 0
                }
                currentProfile = waypointName.lowercase()
                setCurrentParent()
                ChatUtils.sendMessage("&bCDW Set profile to &a$waypointName", true)
            }

            "delprofile" -> {
                val waypointName = args.getOrNull(1) as? String?
                if (waypointName == null) {
                    ChatUtils.sendMessage("&cCDW Invalid waypoint name", true)
                    return 0
                }
                val profile = waypointData.data!!.indexOfFirst { it.name == waypointName }
                if (profile == -1) {
                    ChatUtils.sendMessage("&cCDW Seems like that profile does not exist", true)
                    return 0
                }

                waypointData.data!!.removeAt(profile)
                currentProfile = "default"
                currentParent = null
                ChatUtils.sendMessage("&bCDW Successfully removed waypoint with profile name &a$waypointName", true)
            }

            "switch" -> {
                val waypointType = args.getOrNull(1) as? String?
                if (!waypointType.isNullOrEmpty()) {
                    val enum = WaypointType.byName(waypointType)
                    if (enum == null) {
                        ChatUtils.sendMessage("&cCDW Waypoint Type with name \"$waypointType\" does not exist", true)
                        return 0
                    }
                    currentWaypointType = enum
                    ChatUtils.sendMessage("&bCDW Set current waypoint type to &a$currentWaypointType", true)
                    return 1
                }
                var nextIdx = currentWaypointType.ordinal + 1
                val entries = WaypointType.entries.toTypedArray()
                if (nextIdx > entries.size - 1) nextIdx = 0

                currentWaypointType = entries[nextIdx]
                ChatUtils.sendMessage("&bCDW Set current waypoint type to &a$currentWaypointType", true)
            }

            "settext" -> {
                if (textPos == null) {
                    ChatUtils.sendMessage("&cCDW There is not a previous shift + right click block in edit mode", true)
                    return 0
                }
                val text = args.getOrNull(1) as? String?
                if (text == null) {
                    ChatUtils.sendMessage("&cCDW Seems like you did not provide a correct text", true)
                    return 0
                }
                val data =
                    currentParent?.waypoints?.find { it.cx == textPos!!.first && it.cy == textPos!!.second && it.cz == textPos!!.third }
                if (data == null) {
                    ChatUtils.sendMessage("&cCDW Could not find the waypoint to set Text at", true)
                    return 0
                }
                data.text = text
                textPos = null
                ChatUtils.sendMessage("&aCDW Successfully set text &b$text", true)
            }

            "import" -> {
                var encode = args.getOrNull(1) as? String?
                if (encode.isNullOrEmpty()) encode = minecraft.keyboardHandler.clipboard
                if (encode == null) {
                    ChatUtils.sendMessage("&cCDW Invalid import", true)
                    return 0
                }
                val decoded = Base64.getDecoder().decode(encode)
                val json = PersistentJson.gson.fromJson(decoded.toString(Charsets.UTF_8), WaypointProfile::class.java)

                if (waypointData.data!!.any { it.name == json.name }) {
                    ChatUtils.sendMessage("&cCDW Cannot import profiles with similar names \"${json.name}\"", true)
                    return 0
                }
                waypointData.data!!.add(json)
                ChatUtils.sendMessage("&aCDW Successfully added profile &a${json.name}", true)
            }

            "export" -> {
                var waypointName = args.getOrNull(1) as? String?
                if (waypointName.isNullOrEmpty()) waypointName = currentProfile
                val profile = waypointData.data!!.find { it.name == waypointName.lowercase() }
                if (profile == null) {
                    ChatUtils.sendMessage("&cCDW No profile currently selected", true)
                    return 0
                }
                val json = PersistentJson.gson.toJson(profile)
                val encoded = Base64.getEncoder().encodeToString(json.toByteArray(Charsets.UTF_8))

                minecraft.keyboardHandler.clipboard = encoded
                ChatUtils.sendMessage("&bCDW Exported profile &a${profile.name}&b to clipboard", true)
            }

            else -> {
                val title = "${ChatUtils.prefix} &b&lCustom Dungeon Waypoints Guide"
                ChatUtils.sendMessage("${ChatUtils.centerTextPadding(title)}$title")
                ChatUtils.sendMessage("&b&l/dv cdw &f| &aWhenever doing this command it'll &aEnable&b or &cDisable&b the edit mode")
                ChatUtils.sendMessage(" &e&l- What is the edit mode ?")
                ChatUtils.sendMessage("   &bThis mode will allow you to &aright click&b a block to add it onto the waypoints list as well as &ashift + right click&b to add a text into it which you can then set by doing &6/dv cdw settext&b you can also use &actrl + right click&b to remove a waypoint")
                ChatUtils.sendMessage(" &e&l- Adding/Removing &7(only works inside edit mode)")
                ChatUtils.sendMessage("   &e- &aRight Click &f| &bWill add a new waypoint &7(The waypoint type depends on the current one)")
                ChatUtils.sendMessage("   &e- &aShift + Right Click &f| &bWill tell devonian that this should have a text &7(Whenever the &f/dv cdw settext &7command is ran it'll set the text)")
                ChatUtils.sendMessage("   &e- &a(CTRL)CONTROL + Right Click &f| &bThis will &cRemove&b the clicked waypoint")
                ChatUtils.sendMessage("&b&l/dv cdw create &7<ProfileName> &f| &aCreates a new waypoint profile with the specified profile name")
                ChatUtils.sendMessage("&b&l/dv cdw createbase &7<ProfileName1> <ProfileName2> &f| &aCreates a new waypoint profile with the specified ProfileName1's waypoints as default base &7(sets ProfileName1's waypoints into the new one)")
                ChatUtils.sendMessage("&b&l/dv cdw switch &7<WaypointType> &f| &aSwitches the waypoints mode, if no &bWaypointType&a is set it'll cycle through them")
                ChatUtils.sendMessage("&b&l/dv cdw profiles &f| &aDisplays your current profiles")
                ChatUtils.sendMessage("&b&l/dv cdw setprofile &7<ProfileName> &f| &aChanges your current profile for waypoints")
                ChatUtils.sendMessage("&b&l/dv cdw delprofile &7<ProfileName> &f| &aDeletes the specified profile for waypoints")
                ChatUtils.sendMessage("&b&l/dv cdw settext &7<TextHere> &f| &aSets the text to the previously shift + right clicked waypoint")
                ChatUtils.sendMessage("${ChatUtils.centerTextPadding("------")}&7------")
            }
        }

        return 1
    }

    private fun onSecret(x: Double, y: Double, z: Double, type: Int) {
        // TODO: ETHER, REDSTONE, LOCKED_CHEST, BLOCK_MINE
        if (!SETTING_REMOVE_ON_COLLECT.get()) return
        if (editMode) return
        val room = DungeonScanner.currentRoom ?: return
        val roomId = room.roomID ?: return
        if (currentParent == null || currentParent!!.id != roomId) return

        currentParent!!.waypoints.forEach {
            if (
                type == 0 && (it.type != WaypointType.CHEST && it.type != WaypointType.ESSENCE) ||
                type == 1 && it.type != WaypointType.BAT ||
                type == 2 && it.type != WaypointType.ITEM
            ) return@forEach
            val pos = it.pos() ?: return@forEach
            if (it.clicked) return@forEach
            val dist = abs(pos.x - x.toInt()) + abs(pos.z - z.toInt())
            if (type == 1 && dist < 10 || type == 2 && dist < 8) {
                it.clicked = true
                return@forEach
            }
            if (pos.x != x.toInt() || pos.y != y.toInt() || pos.z != z.toInt()) return@forEach
            it.clicked = true
        }
    }

    private fun setCurrentParent() {
        if (currentParent == null) return

        currentParent = waypointData.data!!
            .find { it.name == currentProfile.lowercase() }
            ?.parents
            ?.find { it.id == currentRoom }
            ?: ParentWaypoint(currentRoom!!, mutableListOf())
    }
}