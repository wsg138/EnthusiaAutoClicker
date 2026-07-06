# Enthusia Server AutoClicker

Server-side Paper plugin for simple automatic melee attacks.

This plugin does not spoof client packets. It uses Paper/Bukkit's player attack path so the
server applies normal item damage, enchantments, cooldown behavior, fire aspect, sweeping, damage
events, and other server-side combat rules.

## Requirements

- Paper or a Paper-compatible server
- Java 17+
- CombatX installed and enabled, or the built-in PvP combat fallback enabled
- Geyser/Floodgate players are supported because the plugin acts on the server-side `Player`

## Commands

- `/autoclick` enables cooldown mode. The plugin attacks when the held item's attack cooldown is ready.
  Running `/autoclick` again toggles it off.
- `/autoclick <ticks>` enables fixed interval mode. Example: `/autoclick 20`
- `/autoclick off` disables it.
- `/autoclick status` shows the current mode.
- `/autoclick check <player>` silently checks whether the client mod completed its private handshake.
- `/autoclick reload` reloads the plugin configuration and stops active sessions.

## Safety

- Never targets players.
- Cancels and stops if an auto-attack would damage a player, including sweeping damage.
- Uses normal `player.attack(target)` for real target attacks. Fast fixed interval mode can swing faster
  than a tool's full attack cooldown, but Paper/vanilla still applies normal cooldown-scaled damage.
- Does not apply custom damage and does not reset attack cooldown before real attacks.
- Blocks attacks through full solid blocks by default while allowing safe passable/partial-block setups.
- Stops when a player opens a chest, menu, trade, or other non-default inventory.
- Excludes villagers, armor stands, tamed pets, passive animals, and players by default.
- Stops while CombatX reports the player is in combat.
- If CombatX cannot be hooked, falls back to a built-in PvP damage tracker.
- Stops if the player moves farther than the configured movement limit from the activation point.
- Stops on death, quit, world change, teleport, game mode changes, or target loss depending on config.

## Mod Checks

The check command is intentionally silent to the target player. It only reports `DETECTED` after the
client mod sends a private plugin-message handshake on `enthusia_autoclicker:handshake`. The command
does not use client brand strings or passive plugin-channel guessing.

Current client jars send this handshake during the play connection join phase. `/autoclick check <player>`
reports whether Enthusia AutoClicker was detected for that player, including mod version, loader,
Minecraft version, and received time when present. This is a convenience signal, not secure proof that
the exact client mod is installed.

## Manual QA Checklist

- Normal player can use `/autoclick` by default.
- `/autoclick` toggles cooldown mode.
- `/autoclick <ticks>` allows fast fixed interval mode.
- Fast fixed interval mode does not do full cooldown damage every tick.
- Plugin uses normal server attack behavior, not custom damage.
- Autoclick stops or pauses when opening a chest/menu.
- Autoclick cannot hit players directly.
- Autoclick disables if an auto-attack would damage a player.
- Autoclick cannot hit mobs through full solid blocks.
- Autoclick can hit intended allowed mobs within range.
- Autoclick refuses excluded targets like villagers, armor stands, pets, and passive mobs by default.
- CombatX unavailable path works according to `require-combatx`.
- `/autoclick reload` applies config changes.
- `/autoclick check <player>` shows detected mod version, loader, and Minecraft version when present.

## Build

```powershell
mvn package
```

The plugin jar is written to `target/EnthusiaServerAutoClicker.jar`.
