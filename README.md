# devonian
Devonian is a minecraft mod focused on enhancing your gameplay experience while playing Hypixel Skyblock.
Adding various QOL features for a better experience while gaming.

# Features
<details>
    <summary>
        <strong>
            Dungeons (expand)
        </strong>
    </summary>

* BoxStarMobs
  - Adds a box at dungeon mobs that are meant to complete the room.
* AutoRequeueDungeons
  - Automatically calls the `/instancerequeue` command at the end of a run.
* ExtraStats
  - Automatically calls the `/showextrastats` command at the end of a run.
* MimicKilled
  - Whenever a mimic is killed it will send a party message.
* CryptsDisplay
  - Displays the current amount of Crypts killed.
* DeathsDisplay
  - Displays the current amount of Team Deaths.
* MilestoneDisplay
  - Displays your current Milestone.
* PuzzlesDisplay
  - Displays the current Puzzle count as well as their name and state.
* RemoveDamageTag
  - Removes the damage tags created by you or others.
* HideNoStarTag
  - Hides name tag of mobs that do not have star in their name tag
* DungeonBreakerDisplay
  - Displays the amount of charges you have left
* SecretsClickedBox
  - Highlights the secrets you have clicked surrounding them with a box, if a chest secret for example is locked the color will change to red.
* SecretsSounds
  - Plays a sound whenever you click, pick up (a secret) or kill a bat
  - This also plays an anvil sound whenever the chest is locked
* LividSolver
  - Highlights the correct livid in F5/M5
* RunSplits
  - Displays how long your party has take to complete Blood Rush, Blood Open & Boss Enter
* BossSplits
  - Similar to RunSplits but inside boss room
* Prince Killed
  - Announced whenever you killed a prince
* DungeonMap
* BoxDoors
  - Highlights doorways inside the current dungeon run
* ScoreDisplay
  - Displays score information
* BoulderSolver
* ThreeWeirdosSolver
* BoxIcedMobs
  - Box mobs that are ice sprayed
* BlazeSolver
* KeyPickup
  - Adds a title, sound and highlights the wither/blood key that is currently dropped
* CreeperBeamsSolver
* ArrowAlignSolver
* CurrentRoomName
  - Displays a hud with the player's current room name
* CurrentRoomCleared
  - Displays a title whenever the current room is cleared
* TeleportMazeSolver
* TriviaSolver
  - Quiz puzzle solver
* IcePathSolver
* TicTacToeSolver
* WaterBoardSolver
* ScoreAlert
* GoldorFrenzyTimer
* ChestProfit

</details>

<details>
    <summary>
        <strong>
            Garden (expand)
        </strong>
    </summary>

* GardenDisplay
  - Displays all your Garden's current stats from tab. (for example current composter Fuel)
* PestsDisplay
  - Displays all your Garden's current Pests stats.

</details>

<details>
    <summary>
        <strong>
            Slayers (expand)
        </strong>
    </summary>

* BossSlainTime
  - Displays the amount of time taken to kill a Slayer Boss.
* BossSpawnTime
  - Displays the amount of time taken to spawn a Slayer Boss.

</details>

<details>
    <summary>
        <strong>
            End (expand)
        </strong>
    </summary>

* GolemWaypoint
  - Sets a waypoint to where the golem should spawn
* GolemDps
  - Tells you how much DPS you did during the Golem fight
* GolemLootQuality
  - Shows your loot quality for the Golem and whether you could roll for a Tier Booster Core/Legendary Golem Pet/Epic Golem Pet
* GolemSpawnTimer
  - Displays a timer whenever the Golem has hit stage 5 of 20 seconds (according to wiki)
* GolemStage5Sound
  - Plays an Anvil Place sound whenever the golem hits stage 5
* EyesPlaced
  - Displays the amount of eyes placed whenever in the Dragon's Nest

</details>

<details>
    <summary>
        <strong>
            Diana (expand)
        </strong>
    </summary>

* BurrowWaypoint
  - Adds a waypoint with the type of burrow whenever the particles are detected
* BurrowGuesser
  - Whenever right clicking on a spade, it will attempt to guess where the location will be at (This scanner was taken from [SkyHanni](https://github.com/hannibal002/SkyHanni))
* DianMobTracker
  - Tells you how many kills you have on a specific mob
* DianaDropTracker
  - Tells you how many drops you got

</details>

<details>
    <summary>
        <strong>
            Misc (expand)
        </strong>
    </summary>

* RemoveFallingBlocks
  - Stops blocks that are falling from rendering (this can give more performance)
* RemoveFireOverlay
  - Stops the Fire Overlay from rendering in your screen.
* NoCursorReset
  - Avoids resetting the cursor whenever you change Guis quickly.
* PreventPlacingWeapons
  - Prevents placing weapons that are placeable.
* RemoveBlockBreakParticle
  - Stops Block Break particles from rendering.
* RemoveExplosionParticle
  - Stops Explosion particles from rendering.
* MiddleClickGui
  - Changes your Left Clicks into Middle Clicks (useful for higher ping players)
* ProtectItem
  - Protects an item, so you can no longer accidentally throw it away or sell it.
* NoHurtCamera
  - Stops the hurt camera from rendering.
* RemoveLightning
  - Stops Lightning from rendering.
* HideInventoryEffects
  - Stops the Potion effects inside your inventory from rendering.
* HidePotionEffectOverlay
  - Stops the Potion effects "sprites" that go to the top-right side of your screen from rendering.
* BlockOverlay
  - Adds a more customizable Block Overlay.
* EtherwarpOverlay
  - Renders a box at the location where the etherwarp is going to be at.
* PreventPlacingPlayerHeads
  - Stops Player Heads from being placeable.
* NoDeathAnimation
  - Removes the Death Animation from entities that die.
* RemoveFrontView
  - Removes the Front View perspective when switching perspectives.
* ChatWaypoint
  - Renders a waypoint at the location where a player sent in Party/Coop chat
  - You can send coordinates for other people by doing `/devonian sendcoords`
* RemoveChatLimit
  - Removes the chat limit from being limited to only display 100 chat messages in your chat.
* CopyChat
  - Right click to copy a message in chat.
* WorldAge
  - Displays the current World's age.
* RemoveTabPing
  - Removes the Ping section from tab.
* CompactChat
  - Adds stacking messages, so whenever there's multiple of the same message only one is displayed.
* FactoryHelper
  - Highlights the best (cheapest) employee or coach jackrabbit upgrade to go for next.
* PreviousLobby
  - Alerts you whenever you join the same server (lobby) and tells you how long its been since you were last seen in it, if the time is above 60s it will be removed (from the list) after the alert.
* EtherwarpSound
  - Changes the sound the etherwarp makes whenever you have etherwarped successfully, customize it via `/devonian etherwarpsound`
* InventoryHistoryLog
  - Displays the items changed, removed or added to your inventory
* SpeedDisplay
* DisableChatAutoScroll
  - Disables auto scrolling down whenever there is a new message
* DisableAttachedArrows
* DisableVignette
* DisableWaterOverlay
* DisableSuffocatingOverlay
* PingDisplay
* DisableVanillaArmor
  - Disables the "vanilla armor" that appears above the hotbar
* AccurateAbsorption
* ChangeCrouchHeight
  - Visually changes your crouching height
* DisableFog
* QuiverDisplay
  - Displays your quiver's current arrow count as well as the arrow type

</details>

# Credits
Special thanks to [Chattriggers Fabric](https://github.com/ChatTriggers/ctjs), a lot of internal logic wouldn't be possible without it.