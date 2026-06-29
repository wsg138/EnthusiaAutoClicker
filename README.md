# Enthusia AutoClicker

Enthusia AutoClicker is a client-only, rate-limited auto-clicker for Minecraft Java Edition.
It drives Minecraft's normal attack/use key mappings and client item-use path instead of
constructing packet objects itself.

## Supported Builds

| Minecraft | Fabric | Quilt | Forge | NeoForge | Java |
|---|---:|---:|---:|---:|---:|
| 1.20 | Yes | Fabric JAR | Yes | No | 17 |
| 1.20.1 | Yes | Fabric JAR | Yes | No | 17 |
| 1.20.2-1.20.4 | Yes | Fabric JAR | Yes | No | 17 |
| 1.20.5-1.20.6 | Yes | Fabric JAR | Yes | No | 21 |
| 1.21.x | Yes | Fabric JAR | Yes | Yes | 21 |
| 26.1.x | Yes | Fabric JAR | Yes | Yes | 25 |
| 26.2 | Yes | Fabric JAR | Yes | Yes | 25 |

The 26.1.x Fabric metadata also accepts later compatible 26.1.x hotfixes. Minecraft 26.2 is
published as a separate compatibility band because Minecraft client APIs changed between 26.1.x
and 26.2.

Quilt Loader supports Fabric mods, so the Fabric JAR is also the Quilt artifact. A separate
Quilt-only build would duplicate the same code without improving compatibility.

Minecraft 1.20 is published as four separate compatibility bands because Mojang changed client APIs
mid-series: `1.20`, `1.20.1`, `1.20.2-1.20.4`, and `1.20.5-1.20.6`. Releases therefore use one JAR
per Minecraft compatibility band and loader, not one universal 1.20 JAR.

## Usage

1. Install the Fabric, Forge, or NeoForge JAR matching the client. Quilt users install the
   matching Fabric JAR.
2. Open Minecraft's Controls screen and assign the **Start/Stop** key.
3. Press `O` by default to open the settings screen. Fabric users can also open it from Mod Menu.
4. Enable the left and right autoclickers independently.
5. Choose **Click** for interval-based input or **Hold** to keep that mouse action pressed.
6. Enter intervals as ticks. Set **Stop after** to `0` for an unlimited run.
7. Optionally enable **Eat offhand food** under the left clicker. When the configured
   hunger threshold is reached, left clicking pauses while food in the offhand is eaten.

The **Extras** tab includes:

- A durability guard that disables the entire autoclicker before the main-hand item breaks.
- Optional armor-stand eating after 10 seconds of continuous autoclicker use.
- Automatic offhand food restocking from the player inventory.
- An optional shutdown when hungry and no usable offhand food remains.

Click intervals support decimal tick values and cannot be configured below 12.5 ticks
(625 milliseconds).

## Safety Behavior

- All automated input pauses while any screen or GUI is open.
- Synthetic held inputs are released when paused or disabled.
- One-shot clicks are queued through Minecraft's normal key mapping click path.
- Left-click input is suppressed while the player is actively using an item.
- All automated mouse actions pause while the crosshair targets another player.
- Offhand food mode pauses both clickers while vanilla completes the eating action.
- Food mode only uses items marked as both food and consumable in the offhand.
- Armor-stand eating never changes entity hitboxes and cannot activate during the first 10 seconds.
- Restocking uses Minecraft's player-inventory click handler, merges matching stackable food, and never replaces a non-food offhand item.
- Inventory scans are limited to once per second while the relevant automation is enabled.
- The autoclicker disables on world exit, and GUI time does not count toward the armor-stand delay.
- A newly started right-click hold or right click takes priority over left click for that tick.
- The mod does not construct vanilla gameplay packet objects. The private server-plugin handshake is
  currently disabled by default while the newer custom payload encoders are reworked.
- The mod does not run without a loaded player, level, and client game mode.

This behavior is intended to avoid malformed or impossible protocol sequences. Server owners
must still test it against their exact anti-cheat configuration. A client-only mod cannot
securely prove that it is the only automation installed; enforcing that policy requires a
separate server-side integration with the chosen anti-cheat.

## Build

Java 25 is required to run the complete multi-version build. The `1.20`, `1.20.1`, and `1.20.2-1.20.4`
artifacts target Java 17 bytecode, the `1.20.5-1.20.6` and `1.21.x` artifacts target Java 21,
and the `26.1.x` and `26.2` artifacts require Java 25.

```powershell
.\gradlew.bat build
```

Output JARs:

- `versions/1.20/fabric/build/libs/EnthusiaAutoClicker-1.20-Fabric.jar`
- `versions/1.20/forge/build/libs/EnthusiaAutoClicker-1.20-Forge.jar`
- `versions/1.20.x/fabric/build/libs/EnthusiaAutoClicker-1.20.1-Fabric.jar`
- `versions/1.20.x/forge/build/libs/EnthusiaAutoClicker-1.20.1-Forge.jar`
- `versions/1.20.2-1.20.4/fabric/build/libs/EnthusiaAutoClicker-1.20.2-1.20.4-Fabric.jar`
- `versions/1.20.2-1.20.4/forge/build/libs/EnthusiaAutoClicker-1.20.2-1.20.4-Forge.jar`
- `versions/1.20.5-1.20.6/fabric/build/libs/EnthusiaAutoClicker-1.20.5-1.20.6-Fabric.jar`
- `versions/1.20.5-1.20.6/forge/build/libs/EnthusiaAutoClicker-1.20.5-1.20.6-Forge.jar`
- `versions/1.21.x/fabric/build/libs/EnthusiaAutoClicker-1.21.x-Fabric.jar`
- `versions/26.x/fabric/build/libs/EnthusiaAutoClicker-26.1.x-Fabric.jar`
- `versions/26.2/fabric/build/libs/EnthusiaAutoClicker-26.2-Fabric.jar`
- `versions/1.21.x/forge/build/libs/EnthusiaAutoClicker-1.21.x-Forge.jar`
- `versions/1.21.x/neoforge/build/libs/EnthusiaAutoClicker-1.21.x-NeoForge.jar`
- `versions/26.x/forge/build/libs/EnthusiaAutoClicker-26.1.x-Forge.jar`
- `versions/26.x/neoforge/build/libs/EnthusiaAutoClicker-26.1.x-NeoForge.jar`
- `versions/26.2/forge/build/libs/EnthusiaAutoClicker-26.2-Forge.jar`
- `versions/26.2/neoforge/build/libs/EnthusiaAutoClicker-26.2-NeoForge.jar`

To build and collect only the publishable runtime JARs in one place:

```powershell
.\gradlew.bat releaseJars
```

The files are copied to the ignored `release-jars/` directory.

## Configuration

Settings are stored in `config/enthusia-autoclicker.properties`. Writes use a temporary file
and an atomic replacement when the filesystem supports it.

## Server Plugin

The `server-plugin/` directory contains a separate Paper plugin that provides server-side automatic
melee attacks for Java and Geyser/Floodgate Bedrock players. It requires CombatX and stops when
CombatX reports the player is in combat.
