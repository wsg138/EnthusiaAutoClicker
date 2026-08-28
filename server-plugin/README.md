# Enthusia Server AutoClicker

Server-side automatic melee attacks for Enthusia SMP. This is separate from the optional client mod in the repository root: **players do not need the client mod to use `/autoclick` on the SMP**, and the server-side command works for both Java and Bedrock players.

The plugin does not spoof client packets or apply custom damage. Real attacks use Paper/Bukkit's normal `player.attack(target)` path so normal item damage, enchantments, attack-cooldown scaling, durability, Fire Aspect, sweeping, damage events, and other server combat rules still apply.

## Using it on Enthusia

Ordinary players have permission to use the server autoclicker by default.

```text
/autoclick
```

Toggles **cooldown mode**. In this mode, the plugin attempts an attack whenever the player's normal attack cooldown is fully ready. Running `/autoclick` again turns it off.

```text
/autoclick <ticks>
```

Starts **fixed interval mode**. The number is the number of Minecraft ticks between attempts. There are 20 ticks per second, so for example:

| Command | Attempt interval |
| --- | ---: |
| `/autoclick 1` | every tick / up to 20 attempts per second |
| `/autoclick 5` | every 0.25 seconds |
| `/autoclick 10` | every 0.5 seconds |
| `/autoclick 20` | every 1 second |

Enthusia currently permits a minimum fixed interval of **1 tick**. Faster attempts do **not** bypass normal attack strength: actual hits still use the vanilla/Paper attack path, so attacking again before the weapon cooldown recovers produces the corresponding cooldown-scaled damage.

Other player commands:

```text
/autoclick off
/autoclick stop
/autoclick disable
/autoclick status
```

`off`, `stop`, and `disable` are equivalent. `status` reports whether the autoclicker is off, in cooldown mode, or using a fixed interval.

## What it targets

The SMP configuration is intentionally aimed at hostile-mob farming rather than unattended PvP or passive-mob killing.

The server autoclicker:

- **never deliberately targets players**;
- excludes armor stands, villagers, and wandering traders;
- excludes tamed animals;
- excludes passive animals, water mobs, ambient mobs, and golems by default;
- can attack other valid living mobs under the player's crosshair;
- uses a **3-block attack range**;
- uses a small **0.35-block ray size** so targeting is slightly forgiving without becoming an area attack.

When no valid target is under the crosshair, the current configuration lets the player continue swinging rather than automatically ending the session. Empty swings do not reset the player's attack cooldown.

## Stationary farming and automatic stop conditions

The command is designed as a mostly stationary farming aid, not a combat movement bot.

When enabled, the activation position becomes the session anchor. On Enthusia, moving more than **0.75 blocks** from that point stops the autoclicker. Small movement inside that tolerance is allowed.

The current SMP configuration also stops the autoclicker when the player:

- enters PvP combat;
- opens a chest, menu, villager trade, or other non-default inventory;
- teleports or changes worlds;
- dies or leaves the server;
- changes into a game mode where the automated attack is not allowed;
- moves too far from the activation point.

A stopped session must be enabled again manually.

### PvP safety

The plugin checks CombatX when that integration is available and stops immediately if the player is combat-tagged. If CombatX cannot be used, Enthusia's configuration permits a built-in PvP damage tracker to act as the fallback; its current combat window is **10 seconds**.

Auto-attacks are also guarded against player damage, including player damage caused indirectly by an automated attack such as sweeping behavior. The feature is intended for mobs, not PvP.

## Walls and partial blocks

The production configuration prevents attacks through obstructing block collision shapes.

- Full solid walls stop target acquisition.
- Passable blocks such as grass can be ignored.
- Partial blocks such as slabs and trapdoors use their actual collision shapes, so they block an attack only when the attack ray intersects the collision geometry.

This allows normal mob-farm windows while preventing the autoclicker from simply hitting mobs through walls.

## Java and Bedrock

The server feature operates on the server-side Bukkit `Player`, so it works for Java players and Geyser/Floodgate Bedrock players without installing anything client-side.

The separate Enthusia AutoClicker client mod remains available for Java players who want its client-side left/right clicking, hold modes, food handling, durability guard, and other client features. Installing that mod is **not required** for the server `/autoclick` command.

## Client-mod detection

Staff can silently check whether the official client mod completed its private handshake:

```text
/autoclick check <player>
```

When detected, the check can report the mod version, loader, Minecraft version, and when the handshake was received. The target player is not notified.

The handshake is a convenience signal only. It does not prove that the client is unmodified or that no other automation is installed.

### Staff client evidence service

The server plugin publishes the backward-compatible `EnthusiaAutoClickerClientApi` version 1 through Bukkit's service manager. Its evidence schema is versioned separately. `evidence(UUID)` reports whether a handshake was observed, the client protocol, validation state, bounded version fields, observation time, and whether the observation belongs to the current session. The original `handshake(UUID)` query remains available and returns only a validated handshake from the current session.

Recent evidence remains available in memory after logout for moderation lookups. By default, observations expire after 30 minutes and the oldest record is discarded when the 2,048-record limit is reached. `client-evidence.retention-minutes` and `client-evidence.maximum-records` control those bounds and apply on `/autoclick reload`. Unsupported or malformed replacement payloads are reported as such instead of being presented as validated evidence. All observations are cleared on shutdown and are never written to disk.

## Administrative command

```text
/autoclick reload
```

Reloads the server-plugin configuration and stops all currently active autoclick sessions.

Permissions:

- `enthusia.autoclicker.use` — player command; granted by default
- `enthusia.autoclicker.check` — client-mod detection check; operator by default
- `enthusia.autoclicker.admin` — configuration reload; operator by default

## Current SMP configuration summary

| Setting | Enthusia SMP |
| --- | --- |
| Maximum movement from activation point | 0.75 blocks |
| Attack range | 3.0 blocks |
| Target ray size | 0.35 blocks |
| Minimum fixed interval | 1 tick |
| CombatX strictly required | No; internal PvP fallback allowed |
| Internal PvP fallback duration | 200 ticks / 10 seconds |
| Block attacks through obstacles | Yes |
| Allow ray through passable blocks | Yes |
| Stop when inventory/menu opens | Yes |
| Target players | No |
| Target passive animals | No |
| Target tamed animals | No |
| Swing with no target | Yes |
| Stop merely because no target exists | No |
| Stop on teleport/world change | Yes |

## Build

```powershell
mvn package
```

The plugin JAR is written to `target/EnthusiaServerAutoClicker.jar`.
