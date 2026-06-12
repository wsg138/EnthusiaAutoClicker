# Enthusia AutoClicker

Enthusia AutoClicker is a client-only, rate-limited auto-clicker for Minecraft Java Edition.
It drives Minecraft's normal attack/use key mappings and client item-use path instead of
constructing packet objects itself.

## Supported Builds

| Minecraft | Fabric | Quilt | Forge | NeoForge | Java |
|---|---:|---:|---:|---:|---:|
| 1.21.x | Yes | Fabric JAR | Yes | Yes | 21 |
| 26.1.2 | Yes | Fabric JAR | No | No | 25 |

The 26.1.2 Fabric metadata also accepts later compatible 26.1.x hotfixes. It intentionally
does not claim compatibility with 26.2 because Minecraft client APIs can change between game
drops.

Quilt Loader supports Fabric mods, so the Fabric JAR is also the Quilt artifact. A separate
Quilt-only build would duplicate the same code without improving compatibility.

Minecraft 1.19 through 1.20.x are not yet published by this repository. Supporting that range
requires separate source adapters and builds for incompatible Minecraft API bands. Releases should
therefore use one JAR per Minecraft compatibility band and loader, not one universal JAR.

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
- The mod does not construct packet objects or send custom protocol messages.
- The mod does not run without a loaded player, level, and client game mode.

This behavior is intended to avoid malformed or impossible protocol sequences. Server owners
must still test it against their exact anti-cheat configuration. A client-only mod cannot
securely prove that it is the only automation installed; enforcing that policy requires a
separate server-side integration with the chosen anti-cheat.

## Build

Java 25 is required to run the complete multi-version build. The 1.21.x artifacts target
Java 21 bytecode; only the 26.1.2 artifact requires Java 25 at runtime.

```powershell
.\gradlew.bat build
```

Output JARs:

- `fabric/build/libs/EnthusiaAutoClicker-1.21.x-Fabric.jar`
- `fabric-26.1/build/libs/EnthusiaAutoClicker-26.1.2-Fabric.jar`
- `forge/build/libs/EnthusiaAutoClicker-1.21.x-Forge.jar`
- `neoforge/build/libs/EnthusiaAutoClicker-1.21.x-NeoForge.jar`

To build and collect only the publishable runtime JARs in one place:

```powershell
.\gradlew.bat releaseJars
```

The files are copied to the ignored `release-jars/` directory.

## Configuration

Settings are stored in `config/enthusia-autoclicker.properties`. Writes use a temporary file
and an atomic replacement when the filesystem supports it.
