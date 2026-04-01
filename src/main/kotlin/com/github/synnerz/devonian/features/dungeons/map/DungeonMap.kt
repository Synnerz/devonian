package com.github.synnerz.devonian.features.dungeons.map

import com.github.synnerz.devonian.Devonian
import com.github.synnerz.devonian.api.ItemUtils
import com.github.synnerz.devonian.api.Scheduler
import com.github.synnerz.devonian.api.bufimgrenderer.BufferedImageRenderer
import com.github.synnerz.devonian.api.bufimgrenderer.BufferedImageUploader
import com.github.synnerz.devonian.api.dungeon.*
import com.github.synnerz.devonian.api.events.RenderOverlayEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.commands.DevonianCommand
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.hud.HudFeature
import com.github.synnerz.devonian.hud.texthud.StylizedTextHud
import com.github.synnerz.devonian.hud.texthud.StylizedTextHud.*
import com.github.synnerz.devonian.mixin.accessor.ScreenAccessor
import com.github.synnerz.devonian.utils.BasicState
import com.github.synnerz.devonian.utils.BoundingBox
import com.github.synnerz.devonian.utils.math.MathUtils
import com.github.synnerz.devonian.utils.render.states.QuadRenderState
import com.github.synnerz.devonian.utils.render.states.TexturedQuadRenderState
import com.mojang.blaze3d.textures.GpuSampler
import com.mojang.blaze3d.textures.GpuTextureView
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.ChatComponent
import net.minecraft.client.gui.components.PlayerFaceRenderer.*
import net.minecraft.client.gui.navigation.ScreenRectangle
import net.minecraft.client.gui.render.TextureSetup
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.resources.Identifier
import org.joml.Matrix3x2f
import java.awt.Color
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.sin

object DungeonMap : HudFeature(
    "dungeonMap",
    "",
    Categories.DUNGEON_MAP,
    "catacombs",
    subcategory = "Toggle",
) {
    private val SETTING_MC_TEXT = addSwitch(
        "mcText",
        true,
        "may negatively impact performance",
        "Use MC Font Renderer",
        subcategory = "Style",
    )
    private val SETTING_FUNNY = addButton(
        {
            Scheduler.scheduleTask {
                minecraft.openChatScreen(ChatComponent.ChatMethod.COMMAND)
                Scheduler.scheduleTask(2) {
                    (minecraft.screen as ScreenAccessor?)?.insertText("/dv font Mojangles", true)
                }
            }
        },
        description = ":(",
        displayName = "\"guys how do i change map font\"",
        subcategory = "Style",
    )
    private val SETTING_DOC_STYLE = addButton(
        {
            SETTING_DOOR_SIZE.set(0.36)
            SETTING_MARKER_SCALE.set(2.4)
            SETTING_HEAD_SCALE.set(2.46)
            SETTING_NAME_SCALE.set(2.4)
            SETTING_TEXT_SIZE.set(1.27)
            SETTING_MAP_BORDER.set(0.0)
            SETTING_MAP_PADDING.set(0.0)
            SETTING_ROOM_SIZE.set(0.8)
            SETTING_ICON_SIZE.set(0.6) // not used
            SETTING_HIDDEN_ROOM_DARKEN.set(0.7)
            SETTING_TEXT_ALIGNMENT.set(5)
            SETTING_ICON_ALIGNMENT.set(0) // not used
            SETTING_TEXT_SHADOW.set(2)
            SETTING_ICON_STYLE.set(0) // not used
            SETTING_RENDER_NAMES_ONLY_LEAP.set(false)
            SETTING_USE_PLAYER_HEADS.set(true)
            SETTING_USE_MARKER_SELF.set(false)
            SETTING_RENDER_ROOM_NAMES.set(false)
            SETTING_RENDER_PUZZLE_ICON.set(false)
            SETTING_RENDER_PUZZLE_NAME.set(true)
            SETTING_NORMAL_DOOR_DYNAMIC.set(true)
            SETTING_RENDER_FAIRY_CHECK.set(false)
            SETTING_RENDER_ROOM_NAMES_NOT_EFB.set(false)
            SETTING_DONT_RENDER_YELLOW_NAME.set(false)
            SETTING_RENDER_NAMES.set(true)
            SETTING_MC_TEXT.set(true)
            SETTING_USE_CLASS_NAME.set(true)
            SETTING_COLOR_NAME_BY_CLASS.set(true)
            SETTING_COLOR_MARKER_BY_CLASS.set(true)
            SETTING_RENDER_CHECKMARK.set(false)
            SETTING_RENDER_SECRET_COUNT.set(true)
            SETTING_COLOR_ROOM_TEXT.set(true)
            SETTING_ROTATE.set(false)
            SETTING_DONT_RENDER_CHECK_IF_NAME.set(false)
            SETTING_DONT_RENDER_SECRET_1.set(false)
            SETTING_CHECK_IF_GREEN.set(false)
            SETTING_RENDER_CHECK_IF_0.set(true)
            SETTING_MAP_BACKGROUND_COLOR.set(50462977)
        },
        "Set",
        "Changes the majority of settings to fit a style that docilelm (aka doc) uses NOTE: room colors are not changed",
        displayName = "Doc Style",
        subcategory = "Presets"
    )
    private val SETTING_LEG_STYLE = addButton(
        {
            SETTING_DOOR_SIZE.set(0.4)
            SETTING_MARKER_SCALE.set(2.2)
            SETTING_HEAD_SCALE.set(2.5)
            SETTING_NAME_SCALE.set(1.9)
            SETTING_TEXT_SIZE.set(1.08)
            SETTING_MAP_BORDER.set(0.0)
            SETTING_MAP_PADDING.set(0.0)
            SETTING_ROOM_SIZE.set(0.8)
            SETTING_ICON_SIZE.set(0.4) // not used
            SETTING_HIDDEN_ROOM_DARKEN.set(0.61)
            SETTING_TEXT_ALIGNMENT.set(4)
            SETTING_ICON_ALIGNMENT.set(0) // not used
            SETTING_TEXT_SHADOW.set(2)
            SETTING_ICON_STYLE.set(0) // not used
            SETTING_RENDER_NAMES_ONLY_LEAP.set(true)
            SETTING_USE_PLAYER_HEADS.set(true)
            SETTING_USE_MARKER_SELF.set(true)
            SETTING_RENDER_ROOM_NAMES.set(true)
            SETTING_RENDER_PUZZLE_ICON.set(false)
            SETTING_RENDER_PUZZLE_NAME.set(true)
            SETTING_NORMAL_DOOR_DYNAMIC.set(false)
            SETTING_RENDER_FAIRY_CHECK.set(false)
            SETTING_RENDER_ROOM_NAMES_NOT_EFB.set(true)
            SETTING_DONT_RENDER_YELLOW_NAME.set(false)
            SETTING_RENDER_NAMES.set(true)
            SETTING_MC_TEXT.set(false)
            SETTING_USE_CLASS_NAME.set(true)
            SETTING_COLOR_NAME_BY_CLASS.set(true)
            SETTING_COLOR_MARKER_BY_CLASS.set(true)
            SETTING_RENDER_CHECKMARK.set(false)
            SETTING_RENDER_SECRET_COUNT.set(true)
            SETTING_COLOR_ROOM_TEXT.set(true)
            SETTING_ROTATE.set(false)
            SETTING_DONT_RENDER_CHECK_IF_NAME.set(true)
            SETTING_DONT_RENDER_SECRET_1.set(true)
            SETTING_CHECK_IF_GREEN.set(false)
            SETTING_RENDER_CHECK_IF_0.set(false)
            SETTING_MAP_BACKGROUND_COLOR.set(772723641)
        },
        "Set",
        "Changes the majority of settings to fit a style that legendary_jg (aka leg) uses NOTE: room colors are not changed",
        displayName = "Leg Style",
        subcategory = "Presets"
    )
    private val SETTING_DEFAULT_STYLE = addButton(
        {
            SETTING_DOOR_SIZE.set(0.31)
            SETTING_MARKER_SCALE.set(3.16)
            SETTING_HEAD_SCALE.set(2.5)
            SETTING_NAME_SCALE.set(2.4)
            SETTING_TEXT_SIZE.set(1.33)
            SETTING_MAP_BORDER.set(3.53)
            SETTING_MAP_PADDING.set(0.12)
            SETTING_ROOM_SIZE.set(0.8)
            SETTING_ICON_SIZE.set(0.6) // not used
            SETTING_HIDDEN_ROOM_DARKEN.set(0.7)
            SETTING_TEXT_ALIGNMENT.set(5)
            SETTING_ICON_ALIGNMENT.set(0) // not used
            SETTING_TEXT_SHADOW.set(1)
            SETTING_ICON_STYLE.set(0) // not used
            SETTING_RENDER_NAMES_ONLY_LEAP.set(true)
            SETTING_USE_PLAYER_HEADS.set(true)
            SETTING_USE_MARKER_SELF.set(true)
            SETTING_RENDER_ROOM_NAMES.set(true)
            SETTING_RENDER_PUZZLE_ICON.set(false)
            SETTING_RENDER_PUZZLE_NAME.set(true)
            SETTING_NORMAL_DOOR_DYNAMIC.set(false)
            SETTING_RENDER_FAIRY_CHECK.set(false)
            SETTING_RENDER_ROOM_NAMES_NOT_EFB.set(true)
            SETTING_DONT_RENDER_YELLOW_NAME.set(false)
            SETTING_RENDER_NAMES.set(true)
            SETTING_MC_TEXT.set(true)
            SETTING_USE_CLASS_NAME.set(false)
            SETTING_COLOR_NAME_BY_CLASS.set(false)
            SETTING_COLOR_MARKER_BY_CLASS.set(false)
            SETTING_RENDER_CHECKMARK.set(false)
            SETTING_RENDER_SECRET_COUNT.set(true)
            SETTING_COLOR_ROOM_TEXT.set(true)
            SETTING_ROTATE.set(false)
            SETTING_DONT_RENDER_CHECK_IF_NAME.set(false)
            SETTING_DONT_RENDER_SECRET_1.set(false)
            SETTING_CHECK_IF_GREEN.set(false)
            SETTING_RENDER_CHECK_IF_0.set(false)
            SETTING_MAP_BACKGROUND_COLOR.set(1577189633)

            SETTING_ROOM_ENTRANCE_COLOR.set(Color(20, 133, 0, 255).rgb)
            SETTING_ROOM_NORMAL_COLOR.set(Color(107, 58, 17, 255).rgb)
            SETTING_ROOM_MINIBOSS_COLOR.set(Color(107, 58, 17, 255).rgb)
            SETTING_NORMAL_DOOR_COLOR.set(Color(92, 52, 14, 255).rgb)
            SETTING_ROOM_PUZZLE_COLOR.set(Color(117, 0, 133, 255).rgb)
            SETTING_ROOM_YELLOW_COLOR.set(Color(254, 223, 0, 255).rgb)
            SETTING_ROOM_FAIRY_COLOR.set(Color(224, 0, 255, 255).rgb)
            SETTING_ROOM_BLOOD_COLOR.set(Color(255, 0, 0, 255).rgb)
            SETTING_ROOM_TRAP_COLOR.set(Color(216, 127, 51, 255).rgb)
            SETTING_ROOM_UNKNOWN_COLOR.set(Color(65, 65, 65, 255).rgb)
            SETTING_DOOR_WITHER_COLOR.set(Color(0, 0, 0, 255).rgb)
        },
        "Set",
        "default",
        displayName = "Default Style",
        subcategory = "Presets"
    )
    private val SETTING_USE_PLAYER_HEADS = addSwitch(
        "playerHeads",
        true,
        "",
        "Render Player Heads",
        subcategory = "Markers",
    )
    private val SETTING_USE_MARKER_SELF = addSwitch(
        "markerSelf",
        true,
        "",
        "Use Marker for Self",
        subcategory = "Markers",
    )
    private val SETTING_RENDER_NAMES = addSwitch(
        "renderNames",
        true,
        "Render player names above marker.",
        "Render Names",
        subcategory = "Markers",
    )
    private val SETTING_RENDER_NAMES_ONLY_LEAP = addSwitch(
        "namesRequireLeap",
        true,
        "Only render player names when holding leap.",
        "Render Names When Holding Leap",
        subcategory = "Markers",
    )
    private val SETTING_USE_CLASS_NAME = addSwitch(
        "useClassName",
        false,
        "Render the class name instead of the player name.",
        "Render Class Name",
        subcategory = "Markers",
    )
    private val SETTING_COLOR_NAME_BY_CLASS = addSwitch(
        "colorNameClass",
        false,
        "Colors the player names by their respective class.",
        "Color Player Names",
        subcategory = "Markers",
    )
    private val SETTING_COLOR_MARKER_BY_CLASS = addSwitch(
        "colorMarkerClass",
        false,
        "Colors the player marker by their respective class.",
        "Color Player Markers",
        subcategory = "Markers",
    )
    private val SETTING_NAME_SCALE = addDecimalSlider(
        "nameScale",
        2.4,
        0.0, 10.0,
        "",
        "Player Name Scale",
        subcategory = "Markers",
    )
    private val SETTING_MARKER_SCALE = addDecimalSlider(
        "markerScale",
        3.16,
        0.0, 10.0,
        "",
        "Marker Scale",
        subcategory = "Markers",
    )
    private val SETTING_HEAD_SCALE = addDecimalSlider(
        "headScale",
        2.5,
        0.0, 10.0,
        "",
        "Head Scale",
        subcategory = "Markers"
    )
    private val SETTING_PRESET_HYPIXEL_COLORS = addButton(
        {
            SETTING_ROOM_ENTRANCE_COLOR.set(Color(0, 124, 0, 255).rgb)
            SETTING_ROOM_NORMAL_COLOR.set(Color(114, 67, 27, 255).rgb)
            SETTING_ROOM_MINIBOSS_COLOR.set(Color(114, 67, 27, 255).rgb)
            SETTING_NORMAL_DOOR_COLOR.set(Color(114, 67, 27, 255).rgb)
            SETTING_ROOM_PUZZLE_COLOR.set(Color(178, 76, 216, 255).rgb)
            SETTING_ROOM_YELLOW_COLOR.set(Color(229, 229, 51, 255).rgb)
            SETTING_ROOM_FAIRY_COLOR.set(Color(242, 127, 165, 255).rgb)
            SETTING_ROOM_BLOOD_COLOR.set(Color(255, 0, 0, 255).rgb)
            SETTING_ROOM_TRAP_COLOR.set(Color(216, 127, 51, 255).rgb)
            SETTING_ROOM_UNKNOWN_COLOR.set(Color(65, 65, 65, 255).rgb)
            SETTING_DOOR_WITHER_COLOR.set(Color(0, 0, 0, 255).rgb)
        },
        "Set",
        "",
        "Hypixel Colors",
        subcategory = "Colors",
    )
    private val SETTING_PRESET_LEGALMAP_COLORS = addButton(
        {
            SETTING_ROOM_ENTRANCE_COLOR.set(Color(20, 133, 0, 255).rgb)
            SETTING_ROOM_NORMAL_COLOR.set(Color(107, 58, 17, 255).rgb)
            SETTING_ROOM_MINIBOSS_COLOR.set(Color(107, 58, 17, 255).rgb)
            SETTING_NORMAL_DOOR_COLOR.set(Color(92, 52, 14, 255).rgb)
            SETTING_ROOM_PUZZLE_COLOR.set(Color(117, 0, 133, 255).rgb)
            SETTING_ROOM_YELLOW_COLOR.set(Color(254, 223, 0, 255).rgb)
            SETTING_ROOM_FAIRY_COLOR.set(Color(224, 0, 255, 255).rgb)
            SETTING_ROOM_BLOOD_COLOR.set(Color(255, 0, 0, 255).rgb)
            SETTING_ROOM_TRAP_COLOR.set(Color(216, 127, 51, 255).rgb)
            SETTING_ROOM_UNKNOWN_COLOR.set(Color(65, 65, 65, 255).rgb)
            SETTING_DOOR_WITHER_COLOR.set(Color(0, 0, 0, 255).rgb)
        },
        "Set",
        "",
        "LegalMap Colors",
        subcategory = "Colors",
    )
    private val SETTING_PRESET_BETTERMAP_COLORS = addButton(
        {
            SETTING_ROOM_ENTRANCE_COLOR.set(Color(0, 123, 0, 255).rgb)
            SETTING_ROOM_NORMAL_COLOR.set(Color(107, 58, 17, 255).rgb)
            SETTING_ROOM_MINIBOSS_COLOR.set(Color(85, 51, 19).rgb)
            SETTING_NORMAL_DOOR_COLOR.set(Color(85, 51, 19, 255).rgb)
            SETTING_ROOM_PUZZLE_COLOR.set(Color(176, 75, 213, 255).rgb)
            SETTING_ROOM_YELLOW_COLOR.set(Color(226, 226, 50, 255).rgb)
            SETTING_ROOM_FAIRY_COLOR.set(Color(239, 126, 163, 255).rgb)
            SETTING_ROOM_BLOOD_COLOR.set(Color(255, 0, 0, 255).rgb)
            SETTING_ROOM_TRAP_COLOR.set(Color(213, 126, 50, 255).rgb)
            SETTING_ROOM_UNKNOWN_COLOR.set(Color(64, 64, 64, 255).rgb)
            SETTING_DOOR_WITHER_COLOR.set(Color(0, 0, 0, 255).rgb)
        },
        "Set",
        "",
        "BetterMap Colors",
        subcategory = "Colors",
    )
    private val SETTING_PRESET_CATLAS_COLORS = addButton(
        {
            SETTING_ROOM_ENTRANCE_COLOR.set(Color(20, 133, 0, 255).rgb)
            SETTING_ROOM_NORMAL_COLOR.set(Color(107, 58, 17, 255).rgb)
            SETTING_ROOM_MINIBOSS_COLOR.set(Color(107, 58, 17, 255).rgb)
            SETTING_NORMAL_DOOR_COLOR.set(Color(92, 52, 14, 255).rgb)
            SETTING_ROOM_PUZZLE_COLOR.set(Color(117, 0, 133, 255).rgb)
            SETTING_ROOM_YELLOW_COLOR.set(Color(254, 223, 0, 255).rgb)
            SETTING_ROOM_FAIRY_COLOR.set(Color(224, 0, 255, 255).rgb)
            SETTING_ROOM_BLOOD_COLOR.set(Color(255, 0, 0, 255).rgb)
            SETTING_ROOM_TRAP_COLOR.set(Color(216, 127, 51, 255).rgb)
            SETTING_ROOM_UNKNOWN_COLOR.set(Color(65, 65, 65, 255).rgb)
            SETTING_DOOR_WITHER_COLOR.set(Color(0, 0, 0, 255).rgb)
        },
        "Set",
        "jokes on you, it is the same as LegalMap",
        "Catlas Colors",
        subcategory = "Colors",
    )
    private val SETTING_MAP_BACKGROUND_COLOR = addColorPicker(
        "backgroundColor",
        1577189633,
        "",
        "Map Background Color",
        subcategory = "Colors",
    )
    private val SETTING_MAP_PADDING = addDecimalSlider(
        "padding",
        0.12,
        0.0, 2.0,
        "empty space around the outside of the map, measured in room widths",
        "Map Padding",
        subcategory = "Style",
    )
    private val SETTING_MAP_BORDER = addDecimalSlider(
        "border",
        3.55,
        0.0, 20.0,
        "",
        "Map Border Width",
        subcategory = "Style",
    )
    private val SETTING_MAP_BORDER_COLOR = addColorPicker(
        "borderColor",
        Color(0).rgb,
        "",
        "Map Border Color",
        subcategory = "Colors",
    )
    private val SETTING_ROOM_ENTRANCE_COLOR = addColorPicker(
        "roomEntranceColor",
        Color(20, 133, 0).rgb,
        "",
        "Entrance Room Color",
        subcategory = "Colors",
    )
    private val SETTING_ROOM_NORMAL_COLOR = addColorPicker(
        "roomNormalColor",
        Color(107, 58, 17).rgb,
        "",
        "Normal Room Color",
        subcategory = "Colors",
    )
    private val SETTING_ROOM_MINIBOSS_COLOR = addColorPicker(
        "roomMinibossColor",
        Color(107, 58, 17).rgb,
        "(as in: has a miniboss, not yellow)",
        "Miniboss Room Color",
        subcategory = "Colors",
    )
    private val SETTING_ROOM_FAIRY_COLOR = addColorPicker(
        "roomFairyColor",
        Color(224, 0, 255).rgb,
        "",
        "Fairy Room Color",
        subcategory = "Colors",
    )
    private val SETTING_ROOM_BLOOD_COLOR = addColorPicker(
        "roomBloodColor",
        Color(255, 0, 0).rgb,
        "",
        "Blood Room Color",
        subcategory = "Colors",
    )
    private val SETTING_ROOM_PUZZLE_COLOR = addColorPicker(
        "roomPuzzleColor",
        Color(117, 0, 133).rgb,
        "",
        "Puzzle Room Color",
        subcategory = "Colors",
    )
    private val SETTING_ROOM_TRAP_COLOR = addColorPicker(
        "roomTrapColor",
        Color(216, 127, 51).rgb,
        "",
        "Trap Room Color",
        subcategory = "Colors",
    )
    private val SETTING_ROOM_YELLOW_COLOR = addColorPicker(
        "roomYellowColor",
        Color(254, 223, 0).rgb,
        "",
        "Yellow Room Color",
        subcategory = "Colors",
    )
    private val SETTING_ROOM_RARE_COLOR = addColorPicker(
        "roomRareColor",
        Color(255, 203, 89).rgb,
        "",
        "Rare Room Color",
        subcategory = "Colors",
    )
    private val SETTING_ROOM_UNKNOWN_COLOR = addColorPicker(
        "roomUnknownColor",
        Color(65, 65, 65).rgb,
        "",
        "Unknown Room Color",
        subcategory = "Colors",
    )
    private val SETTING_DOOR_WITHER_COLOR = addColorPicker(
        "doorWitherColor",
        Color(0, 0, 0).rgb,
        "",
        "Wither Door Color",
        subcategory = "Colors",
    )
    private val SETTING_DOOR_BLOOD_COLOR = addColorPicker(
        "doorBloodColor",
        Color(255, 0, 0).rgb,
        "",
        "Blood Door Color",
        subcategory = "Colors",
    )
    private val SETTING_DOOR_ENTRANCE_COLOR = addColorPicker(
        "doorEntranceColor",
        Color(20, 133, 0).rgb,
        "",
        "Entrance Door Color",
        subcategory = "Colors",
    )
    // hear me out chick, i know it doesn't belong here but it's part of the colors
    private val SETTING_NORMAL_DOOR_DYNAMIC = addSwitch(
        "doorNormalDynamicColor",
        false,
        "If enabled, this will ignore the \"Normal Door Color\" and use the room's color for a more blend-in door way.",
        "Normal Door Dynamic Color",
        subcategory = "Colors"
    )
    private val SETTING_NORMAL_DOOR_COLOR = addColorPicker(
        "doorNormalColor",
        Color(107, 58, 17).rgb,
        "",
        "Normal Door Color",
        subcategory = "Colors"
    )
    private val SETTING_ROOM_SIZE = addDecimalSlider(
        "roomSize",
        0.8,
        0.0, 1.0,
        "",
        "Room Size",
        subcategory = "Style",
    )
    private val SETTING_DOOR_SIZE = addDecimalSlider(
        "doorSize",
        0.31,
        0.0, 1.0,
        "",
        "Door Size",
        subcategory = "Style",
    )
    private val SETTING_RENDER_CHECKMARK = addSwitch(
        "renderCheckmark",
        false,
        "",
        "Render Checkmarks",
        subcategory = "Behavior",
    )
    private val SETTING_RENDER_PUZZLE_ICON = addSwitch(
        "renderPuzzleIcon",
        false,
        "",
        "Render Puzzle Icon",
        subcategory = "Behavior",
    )
    private val SETTING_RENDER_ROOM_NAMES = addSwitch(
        "renderRoomNames",
        true,
        "",
        "Render Room Names",
        subcategory = "Behavior",
    )
    private val SETTING_RENDER_ROOM_NAMES_NOT_EFB = addSwitch(
        "dontRenderCommonRoomNames",
        true,
        "Avoids rendering the name for rooms Entrance/Fairy/Blood.",
        "Don't Render Names for Entrance/Fairy/Blood",
        subcategory = "Behavior",
    )
    private val SETTING_DONT_RENDER_YELLOW_NAME = addSwitch(
        "dontRenderYellowName",
        false,
        "Avoids rendering Yellow room's name.",
        "Don't Render Yellow Room Name",
        subcategory = "Behavior"
    )
    private val SETTING_RENDER_FAIRY_CHECK = addSwitch(
        "dontRenderFairyCheckmark",
        false,
        "",
        "Don't Render Fairy Checkmark",
        subcategory = "Behavior"
    )
    private val SETTING_DONT_RENDER_CHECK_IF_NAME = addSwitch(
        "dontRenderCheckIfName",
        false,
        "Avoids rendering checkmarks if the room name is also being rendered.",
        "Don't Render Checkmark and Name",
        subcategory = "Behavior",
    )
    private val SETTING_CHECK_IF_GREEN = addSwitch(
        "checkIfGreen",
        false,
        "Will also hide all room text. Still requires 'Render Checkmarks' to be on, bypasses 'Dont Render Checkmark and Name'.",
        "Render Check If Green",
    )
    private val SETTING_RENDER_SECRET_COUNT = addSwitch(
        "renderSecretCount",
        true,
        "(we dont actually track or sync secrets right now)",
        "Render Secret Count",
        subcategory = "Behavior",
    )
    private val SETTING_DONT_RENDER_SECRET_1 = addSwitch(
        "dontRenderSecret1",
        false,
        "Avoids rendering the secret count if the room only has 1 secret.",
        "Don't Render 1 Secret Count",
        subcategory = "Behavior",
    )
    private val SETTING_RENDER_CHECK_IF_0 = addSwitch(
        "renderCheckIf0",
        false,
        "Renders a checkmark instead of nothing if the room secrets are 0",
        "Render Checkmark If 0",
        subcategory = "Behavior",
    )
    private val SETTING_RENDER_PUZZLE_NAME = addSwitch(
        "renderPuzzleName",
        true,
        "",
        "Render Puzzle Name",
        subcategory = "Behavior",
    )
    private val SETTING_ICON_STYLE = addSelection(
        "iconStyle",
        0,
        listOf("IllegalMap", "NEU", "Vanilla New", "Vanilla Old"),
        "",
        "Icon Style",
        subcategory = "Style",
    )
    private val SETTING_ICON_SIZE = addDecimalSlider(
        "iconSize",
        0.6,
        0.0, 2.0,
        "Affects puzzles + checkmarks. (% of the room)",
        "Icon Size",
        subcategory = "Style",
    )
    private val SETTING_ICON_ALIGNMENT = addSelection(
        "iconAlign",
        DungeonMapRoomInfoAlignment.Center.ordinal,
        DungeonMapRoomInfoAlignment.entries.map { it.str },
        "Alignment of the icon with respect to the room layout.",
        "Icon Alignment",
        subcategory = "Style",
    )
    private val SETTING_TEXT_SIZE = addDecimalSlider(
        "textSize",
        1.33,
        0.0, 2.0,
        "Affects room names + secret count. (% of the room)",
        "Text Size",
        subcategory = "Style",
    )
    private val SETTING_TEXT_ALIGNMENT = addSelection(
        "textAlign",
        DungeonMapRoomInfoAlignment.CenterL.ordinal,
        DungeonMapRoomInfoAlignment.entries.map { it.str },
        "Alignment of the text with respect to the room layout",
        "Text Alignment",
        subcategory = "Style",
    )
    private val SETTING_TEXT_SHADOW = addSelection(
        "textShadow2",
        1,
        listOf("None", "Drop", "Outline"),
        "",
        "Text Shadow",
        subcategory = "Style",
    )
    private val SETTING_COLOR_ROOM_TEXT = addSwitch(
        "colorRoomName",
        true,
        "Change color of room name based on the room checkmark.",
        "Color Room Name",
        subcategory = "Style",
    )
    var SETTING_RENDER_HIDDEN_ROOMS = false
    private val SETTING_HIDDEN_ROOM_DARKEN = addDecimalSlider(
        "hiddenRoomDarken",
        0.7,
        0.0, 1.0,
        "factor by which to darken hidden rooms",
        "Hidden Room Darken Factor",
        subcategory = "Style",
    )
    private val SETTING_ROTATE = addSwitch(
        "rotate",
        false,
        "text rotates too :) (it is upside down)",
        "Rotate Map Around Player",
        subcategory = "Behavior",
    )
    private val SETTING_SHOW_IN_BOSS = addSwitch(
        "showInBoss",
        false,
        "Shows the map in boss room",
        "Show In Boss",
        subcategory = "Behavior"
    )

    override fun createRequirements(): List<BasicState<Boolean>?> {
        return super.createRequirements() + listOf(Dungeons.inBoss.map(Boolean::not).zip(SETTING_SHOW_IN_BOSS.state, Boolean::or))
    }

    private val mapRenderer = DungeonMapBaseRenderer()

    private var dump = false

    init {
        DevonianCommand.command.subcommand("dumpmap") { _, _ ->
            dump = true
            return@subcommand 1
        }
    }

    override fun getBounds(): BoundingBox = BoundingBox(
        x, y,
        100.0 * scale, 100.0 * scale
    )

    fun redrawMap(rooms: List<DungeonRoom?>, doors: List<DungeonDoor?>) {
        var floor = Dungeons.floor
        if (floor == FloorType.None) floor = FloorType.M7

        if (dump) {
            println("Rooms:")
            rooms.forEach {
                if (it == null) return@forEach
                println("${System.identityHashCode(it)} ${it.name} ${it.explored} ${it.type.name} ${it.rotation} ${it.comps.joinToString(",") { it.toComponent().toString() }} ${it.doors.joinToString(",") { "${System.identityHashCode(it)} ${it.type.name} ${it.opened} ${it.comp.toComponent()}" }}")
            }
            println("Doors:")
            doors.forEach {
                if (it == null) return@forEach
                println("${System.identityHashCode(it)} ${it.type.name} ${it.opened} ${it.comp.toComponent()} ${it.rooms.joinToString(",") { "${System.identityHashCode(it)} ${it.name} ${it.explored} ${it.comps.joinToString(",") { it.toComponent().toString() }}" }}")
            }
            dump = false
        }

        val bounds = getBounds()
        val window = minecraft.window
        mapRenderer.update(
            (bounds.w * window.guiScale + 0.5).toInt(),
            (bounds.h * window.guiScale + 0.5).toInt(),
            DungeonMapRenderData(
                rooms, doors,
                DungeonMapRenderOptions(
                    buildMap {
                        put(DungeonMapColors.Background, SETTING_MAP_BACKGROUND_COLOR.getColor())
                        put(DungeonMapColors.Border, SETTING_MAP_BORDER_COLOR.getColor())
                        put(DungeonMapColors.RoomEntrance, SETTING_ROOM_ENTRANCE_COLOR.getColor())
                        put(DungeonMapColors.RoomNormal, SETTING_ROOM_NORMAL_COLOR.getColor())
                        put(DungeonMapColors.RoomMiniboss, SETTING_ROOM_MINIBOSS_COLOR.getColor())
                        put(DungeonMapColors.RoomFairy, SETTING_ROOM_FAIRY_COLOR.getColor())
                        put(DungeonMapColors.RoomBlood, SETTING_ROOM_BLOOD_COLOR.getColor())
                        put(DungeonMapColors.RoomPuzzle, SETTING_ROOM_PUZZLE_COLOR.getColor())
                        put(DungeonMapColors.RoomTrap, SETTING_ROOM_TRAP_COLOR.getColor())
                        put(DungeonMapColors.RoomYellow, SETTING_ROOM_YELLOW_COLOR.getColor())
                        put(DungeonMapColors.RoomRare, SETTING_ROOM_RARE_COLOR.getColor())
                        put(DungeonMapColors.RoomUnknown, SETTING_ROOM_UNKNOWN_COLOR.getColor())
                        if (!SETTING_NORMAL_DOOR_DYNAMIC.get())
                            put(DungeonMapColors.DoorNormal, SETTING_NORMAL_DOOR_COLOR.getColor())
                        put(DungeonMapColors.DoorWither, SETTING_DOOR_WITHER_COLOR.getColor())
                        put(DungeonMapColors.DoorBlood, SETTING_DOOR_BLOOD_COLOR.getColor())
                        put(DungeonMapColors.DoorEntrance, SETTING_DOOR_ENTRANCE_COLOR.getColor())
                    },
                    SETTING_ROOM_SIZE.get(), SETTING_DOOR_SIZE.get(),
                    floor.roomsW, floor.roomsH,
                    SETTING_MAP_PADDING.get(), ceil(SETTING_MAP_BORDER.get() * scale).toInt(),
                    SETTING_ICON_STYLE.get(),
                    SETTING_RENDER_CHECKMARK.get(), SETTING_RENDER_PUZZLE_ICON.get(),
                    SETTING_RENDER_ROOM_NAMES.get(), SETTING_RENDER_ROOM_NAMES_NOT_EFB.get(),
                    SETTING_DONT_RENDER_YELLOW_NAME.get(),
                    SETTING_RENDER_FAIRY_CHECK.get(),
                    SETTING_DONT_RENDER_CHECK_IF_NAME.get(),
                    SETTING_CHECK_IF_GREEN.get(),
                    SETTING_RENDER_SECRET_COUNT.get(),
                    SETTING_DONT_RENDER_SECRET_1.get(),
                    SETTING_RENDER_CHECK_IF_0.get(),
                    SETTING_RENDER_PUZZLE_NAME.get(),
                    SETTING_ICON_SIZE.get(),
                    DungeonMapRoomInfoAlignment.from(SETTING_ICON_ALIGNMENT.getCurrent()),
                    SETTING_TEXT_SIZE.get(),
                    DungeonMapRoomInfoAlignment.from(SETTING_TEXT_ALIGNMENT.getCurrent()),
                    Shadow.from(SETTING_TEXT_SHADOW.get()),
                    SETTING_COLOR_ROOM_TEXT.get(),
                    SETTING_RENDER_HIDDEN_ROOMS,
                    Dungeons.started.value,
                    SETTING_HIDDEN_ROOM_DARKEN.get(),
                    SETTING_MC_TEXT.get(),
                ),
                window.guiScale,
            )
        )
    }

    private val textHuds by lazy {
        List(5) {
            StylizedTextHud("dungeon_map_name_$it").also {
                it.x = 0.0
                it.y = -10.0
                it.scale = 1f
                it.anchor = Anchor.Center
                it.align = Align.Center
                it.backdrop = Backdrop.None
            }
        }
    }

    override fun drawImpl(ctx: GuiGraphics) {
        var floor = Dungeons.floor
        if (floor == FloorType.None) floor = FloorType.M7

        val bounds = getBounds()

        val totalMaxDim = floor.maxDim + SETTING_MAP_PADDING.get() * 2
        val boundsOX = (floor.maxDim - floor.roomsW) / 2.0 + SETTING_MAP_PADDING.get()
        val boundsOY = (floor.maxDim - floor.roomsH) / 2.0 + SETTING_MAP_PADDING.get()
        val compBounds = BoundingBox(
            boundsOX / totalMaxDim * bounds.w,
            boundsOY / totalMaxDim * bounds.h,
            floor.maxDim / totalMaxDim * bounds.w,
            floor.maxDim / totalMaxDim * bounds.h
        )

        ctx.pose()
            .pushMatrix()
            .translate(x.toFloat(), y.toFloat())

        val bgColor = SETTING_MAP_BACKGROUND_COLOR.getColor()
        if (bgColor.alpha > 0) {
            ctx.guiRenderState.submitGuiElement(
                QuadRenderState(
                    RenderPipelines.GUI,
                    Matrix3x2f(ctx.pose()),
                    0f, 0f,
                    bounds.w.toFloat(), bounds.h.toFloat(),
                    SETTING_MAP_BACKGROUND_COLOR.get(),
                    ctx.scissorStack.peek(),
                )
            )
        }

        if (SETTING_ROTATE.get()) {
            ctx.scissorStack.push(
                ScreenRectangle(
                    ceil(x).toInt(),
                    ceil(y).toInt(),
                    bounds.w.toInt(),
                    bounds.h.toInt(),
                )
            )

            val pos = Dungeons.selfPlayer.getLerpedPosition()
            if (pos != null) {
                val px = MathUtils.rescale(
                    pos.x,
                    0.0, floor.maxDim * 2.0,
                    compBounds.x, compBounds.x + compBounds.w
                ).toFloat()
                val py = MathUtils.rescale(
                    pos.z,
                    0.0, floor.maxDim * 2.0,
                    compBounds.y, compBounds.y + compBounds.h
                ).toFloat()
                ctx.pose()
                    .translate((bounds.w * 0.5).toFloat(), (bounds.h * 0.5).toFloat())
                    .rotate((pos.r - PI * 0.5).toFloat())
                    .translate(-px, -py)
            }
        }

        mapRenderer.draw(ctx, 0f, 0f, (1.0 / minecraft.window.guiScale).toFloat())
        if (SETTING_MC_TEXT.get()) {
            mapRenderer.delegatedText.value.forEach {
                it.draw(ctx)
            }
        }
        if (Dungeons.inBoss.value) {
            if (SETTING_ROTATE.get()) ctx.scissorStack.pop()
            ctx.pose().popMatrix()
            return
        }

        val holdingLeap = minecraft.player!!.mainHandItem.let {
            listOf("SPIRIT_LEAP", "INFINITE_SPIRIT_LEAP").contains(ItemUtils.skyblockId(it))
        }

        val shouldRenderName =
            if (SETTING_RENDER_NAMES_ONLY_LEAP.get()) holdingLeap
            else true
        val renderNames = SETTING_RENDER_NAMES.get() && shouldRenderName

        // shrugs
        var idx = Dungeons.players.size
        Dungeons.players.reversed().forEach { (_, player) ->
            val i = --idx
            if (player.isDead) return@forEach
            val pos = player.getLerpedPosition() ?: return@forEach

            val px = MathUtils.rescale(
                pos.x,
                0.0, floor.maxDim * 2.0,
                compBounds.x, compBounds.x + compBounds.w
            ).toFloat()
            val py = MathUtils.rescale(
                pos.z,
                0.0, floor.maxDim * 2.0,
                compBounds.y, compBounds.y + compBounds.h
            ).toFloat()

            val info = player.profileInfo
            val isHead =
                SETTING_USE_PLAYER_HEADS.get() && info != null &&
                        (!SETTING_USE_MARKER_SELF.get() || i > 0)
            var dxf = cos(-pos.r).toFloat() * (if (isHead) SETTING_HEAD_SCALE.get().toFloat() else SETTING_MARKER_SCALE.get().toFloat()) * scale * 0.5f
            var dyf = sin(-pos.r).toFloat() * (if (isHead) SETTING_HEAD_SCALE.get().toFloat() else SETTING_MARKER_SCALE.get().toFloat()) * scale * 0.5f
            var dxr = cos(-pos.r + PI / 2).toFloat() * (if (isHead) SETTING_HEAD_SCALE.get().toFloat() else SETTING_MARKER_SCALE.get().toFloat()) * scale * 0.5f
            var dyr = sin(-pos.r + PI / 2).toFloat() * (if (isHead) SETTING_HEAD_SCALE.get().toFloat() else SETTING_MARKER_SCALE.get().toFloat()) * scale * 0.5f
            val u0: Float
            val v0: Float
            val u1: Float
            val v1: Float
            val maxDy: Float
            val textureView: GpuTextureView
            val sampler: GpuSampler
            if (isHead) {
                dxf *= 4f
                dyf *= 4f
                dxr *= 4f
                dyr *= 4f
                u0 = SKIN_HEAD_U.toFloat() / SKIN_TEX_WIDTH
                v0 = SKIN_HEAD_V.toFloat() / SKIN_TEX_HEIGHT
                u1 = (SKIN_HEAD_U + SKIN_HEAD_WIDTH).toFloat() / SKIN_TEX_WIDTH
                v1 = (SKIN_HEAD_V + SKIN_HEAD_HEIGHT).toFloat() / SKIN_TEX_HEIGHT
                maxDy = 4f
                val skin = info.skin
                val rl = skin.body.texturePath()
                val texture = Devonian.minecraft.textureManager.getTexture(rl)
                textureView = texture.textureView
                sampler = texture.sampler
            } else {
                dxf *= 2.8f
                dyf *= 2.8f
                dxr *= 2f
                dyr *= 2f
                u0 = if (i == 0) MARKER_SELF_U0 else MARKER_OTHER_U0
                v0 = if (i == 0) MARKER_SELF_V0 else MARKER_OTHER_V0
                u1 = if (i == 0) MARKER_SELF_U1 else MARKER_OTHER_U1
                v1 = if (i == 0) MARKER_SELF_V1 else MARKER_OTHER_V1
                maxDy = 2.8f
                textureView = markerAtlasUploader.textureView
                sampler = markerAtlasUploader.sampler
            }

            if (renderNames) {
                val nameFormat =
                    if (SETTING_COLOR_NAME_BY_CLASS.get()) player.role.colorCode
                    else ""
                val text =
                // Force display player name if leap is being held regardless of configurations, maybe should be configurable
                    // or perhaps force this to be the `SETTING_RENDER_NAMES_ONLY_LEAP` standard, since people expect this behavior
                    if (holdingLeap) player.name
                    else if (SETTING_USE_CLASS_NAME.get() && player.role != DungeonClass.Unknown) player.role.shortName
                    else player.name

                val hud = textHuds[i]
                hud.x = px.toDouble()
                hud.y = py - maxDy * (if (isHead) SETTING_HEAD_SCALE.get().toFloat() else SETTING_MARKER_SCALE.get().toFloat()) - hud.getHeight() * 0.5
                hud.shadow = Shadow.from(SETTING_TEXT_SHADOW.get())
                hud.setLine("$nameFormat$text")
                hud.scale = scale * 0.3f * SETTING_NAME_SCALE.get().toFloat()
                hud.draw(ctx)
            }

            ctx.guiRenderState.submitGuiElement(
                TexturedQuadRenderState(
                    BufferedImageRenderer.pipeline,
                    TextureSetup(
                        textureView, null, null,
                        sampler, null, null,
                    ),
                    Matrix3x2f(ctx.pose()),
                    px + dxf - dxr, py + dyf - dyr,
                    px - dxf - dxr, py - dyf - dyr,
                    px + dxf + dxr, py + dyf + dyr,
                    px - dxf + dxr, py - dyf + dyr,
                    u0, v0,
                    u0, v1,
                    u1, v0,
                    u1, v1,
                    -1,
                    ctx.scissorStack.peek()
                )
            )
            if (isHead) {
                ctx.guiRenderState.submitGuiElement(
                    TexturedQuadRenderState(
                        BufferedImageRenderer.pipeline,
                        TextureSetup(
                            textureView, null, null,
                            sampler, null, null,
                        ),
                        Matrix3x2f(ctx.pose()),
                        px + dxf - dxr, py + dyf - dyr,
                        px - dxf - dxr, py - dyf - dyr,
                        px + dxf + dxr, py + dyf + dyr,
                        px - dxf + dxr, py - dyf + dyr,
                        SKIN_HAT_U.toFloat() / SKIN_TEX_WIDTH, SKIN_HAT_V.toFloat() / SKIN_TEX_HEIGHT,
                        SKIN_HAT_U.toFloat() / SKIN_TEX_WIDTH, (SKIN_HAT_V + SKIN_HAT_HEIGHT).toFloat() / SKIN_TEX_HEIGHT,
                        (SKIN_HAT_U + SKIN_HAT_WIDTH).toFloat() / SKIN_TEX_WIDTH, SKIN_HAT_V.toFloat() / SKIN_TEX_HEIGHT,
                        (SKIN_HAT_U + SKIN_HAT_WIDTH).toFloat() / SKIN_TEX_WIDTH, (SKIN_HAT_V + SKIN_HAT_HEIGHT).toFloat() / SKIN_TEX_HEIGHT,
                        -1,
                        ctx.scissorStack.peek()
                    )
                )
            }

            if (SETTING_COLOR_MARKER_BY_CLASS.get() && player.role.color.alpha != 0) {
                val u0: Float
                val v0: Float
                val u1: Float
                val v1: Float
                if (isHead) {
                    u0 = MARKER_HEAD_OUTLINE_U0
                    v0 = MARKER_HEAD_OUTLINE_V0
                    u1 = MARKER_HEAD_OUTLINE_U1
                    v1 = MARKER_HEAD_OUTLINE_V1
                } else {
                    u0 = MARKER_POINTER_OUTLINE_U0
                    v0 = MARKER_POINTER_OUTLINE_V0
                    u1 = MARKER_POINTER_OUTLINE_U1
                    v1 = MARKER_POINTER_OUTLINE_V1
                }

                ctx.guiRenderState.submitGuiElement(
                    TexturedQuadRenderState(
                        BufferedImageRenderer.pipeline,
                        TextureSetup(
                            markerAtlasUploader.textureView, null, null,
                            markerAtlasUploader.sampler, null, null,
                        ),
                        Matrix3x2f(ctx.pose()),
                        px + dxf - dxr, py + dyf - dyr,
                        px - dxf - dxr, py - dyf - dyr,
                        px + dxf + dxr, py + dyf + dyr,
                        px - dxf + dxr, py - dyf + dyr,
                        u0, v0,
                        u0, v1,
                        u1, v0,
                        u1, v1,
                        player.role.colorRgb,
                        ctx.scissorStack.peek()
                    )
                )
            }
        }

        if (SETTING_ROTATE.get()) ctx.scissorStack.pop()

        ctx.pose().popMatrix()
    }

    override fun initialize() {
        on<RenderOverlayEvent> { event ->
            draw(event.ctx)
        }
    }

    override fun sampleDraw(ctx: GuiGraphics, mx: Int, my: Int, selected: Boolean) {
        val pos = getBounds()
        ctx.drawCenteredString(minecraft.font, "Dungeon Map :)", (pos.x + pos.w / 2.0).toInt(), (pos.y + pos.h / 2.0).toInt(), -1)

        super.sampleDraw(ctx, mx, my, selected)
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        mapRenderer.invalidate()
    }

    val mcidMarkerAtlas = Identifier.fromNamespaceAndPath("devonian", "dungeons/map/marker_atlas")
    val markerAtlasUploader = BufferedImageUploader.fromResource("/assets/devonian/dungeons/map/marker_atlas.png")!!
        .register(mcidMarkerAtlas)

    val MARKER_SELF_U0: Float
    val MARKER_SELF_V0: Float
    val MARKER_SELF_U1: Float
    val MARKER_SELF_V1: Float

    val MARKER_OTHER_U0: Float
    val MARKER_OTHER_V0: Float
    val MARKER_OTHER_U1: Float
    val MARKER_OTHER_V1: Float

    val MARKER_POINTER_OUTLINE_U0: Float
    val MARKER_POINTER_OUTLINE_V0: Float
    val MARKER_POINTER_OUTLINE_U1: Float
    val MARKER_POINTER_OUTLINE_V1: Float

    val MARKER_HEAD_OUTLINE_U0: Float
    val MARKER_HEAD_OUTLINE_V0: Float
    val MARKER_HEAD_OUTLINE_U1: Float
    val MARKER_HEAD_OUTLINE_V1: Float

    init {
        val MARKER_ATLAS_WIDTH = 200 * 1f
        val MARKER_ATLAS_HEIGHT = 280 * 1f

        val MARKER_WIDTH = 100
        val MARKER_HEIGHT = 140
        val selfX = 0
        val selfY = 0
        val otherX = MARKER_WIDTH
        val otherY = 0

        val pointerOutlineX = 0
        val pointerOutlineY = MARKER_HEIGHT

        val HEAD_WIDTH = 80
        val HEAD_HEIGHT = 80
        val headOutlineX = MARKER_WIDTH
        val headOutlineY = MARKER_HEIGHT

        MARKER_SELF_U0 = selfX / MARKER_ATLAS_WIDTH
        MARKER_SELF_V0 = selfY / MARKER_ATLAS_HEIGHT
        MARKER_SELF_U1 = (selfX + MARKER_WIDTH) / MARKER_ATLAS_WIDTH
        MARKER_SELF_V1 = (selfY + MARKER_HEIGHT) / MARKER_ATLAS_HEIGHT

        MARKER_OTHER_U0 = otherX / MARKER_ATLAS_WIDTH
        MARKER_OTHER_V0 = otherY / MARKER_ATLAS_HEIGHT
        MARKER_OTHER_U1 = (otherX + MARKER_WIDTH) / MARKER_ATLAS_WIDTH
        MARKER_OTHER_V1 = (otherY + MARKER_HEIGHT) / MARKER_ATLAS_HEIGHT

        MARKER_POINTER_OUTLINE_U0 = pointerOutlineX / MARKER_ATLAS_WIDTH
        MARKER_POINTER_OUTLINE_V0 = pointerOutlineY / MARKER_ATLAS_HEIGHT
        MARKER_POINTER_OUTLINE_U1 = (pointerOutlineX + MARKER_WIDTH) / MARKER_ATLAS_WIDTH
        MARKER_POINTER_OUTLINE_V1 = (pointerOutlineY + MARKER_HEIGHT) / MARKER_ATLAS_HEIGHT

        MARKER_HEAD_OUTLINE_U0 = headOutlineX / MARKER_ATLAS_WIDTH
        MARKER_HEAD_OUTLINE_V0 = headOutlineY / MARKER_ATLAS_HEIGHT
        MARKER_HEAD_OUTLINE_U1 = (headOutlineX + HEAD_WIDTH) / MARKER_ATLAS_WIDTH
        MARKER_HEAD_OUTLINE_V1 = (headOutlineY + HEAD_HEIGHT) / MARKER_ATLAS_HEIGHT
    }
}