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

## Safety

- Never targets players.
- Cancels and stops if an auto-attack would damage a player, including sweeping damage.
- Stops while CombatX reports the player is in combat.
- If CombatX cannot be hooked, falls back to a built-in PvP damage tracker.
- Stops if the player moves farther than the configured movement limit from the activation point.
- Stops on death, quit, world change, teleport, game mode changes, or target loss depending on config.
- By default, targeting is allowed through partial blocks because Bukkit line-of-sight checks are too strict
  for trapdoors and slabs.

## Mod Checks

The check command is intentionally silent to the target player. It only reports `DETECTED` after the
client mod sends a private plugin-message handshake on `enthusia_autoclicker:handshake`. The command
does not use client brand strings or passive plugin-channel guessing.

Current client jars send this handshake during the play connection join phase. `/autoclick check <player>`
reports `NOT DETECTED` until that player's client has joined with a jar that includes the handshake.

## Build

```powershell
mvn package
```

The plugin jar is written to `target/EnthusiaServerAutoClicker.jar`.
