# Enthusia Server AutoClicker

Server-side Paper plugin for simple automatic melee attacks.

This plugin does not spoof client packets. It uses Paper/Bukkit's player attack path so the
server applies normal item damage, enchantments, cooldown behavior, fire aspect, sweeping, damage
events, and other server-side combat rules.

## Requirements

- Paper or a Paper-compatible server
- Java 17+
- CombatX installed and enabled
- Geyser/Floodgate players are supported because the plugin acts on the server-side `Player`

## Commands

- `/autoclick` enables cooldown mode. The plugin attacks when the held item's attack cooldown is ready.
- `/autoclick <ticks>` enables fixed interval mode. Example: `/autoclick 20`
- `/autoclick off` disables it.
- `/autoclick status` shows the current mode.

## Safety

- Never targets players.
- Cancels and stops if an auto-attack would damage a player, including sweeping damage.
- Stops while CombatX reports the player is in combat.
- Stops if the player moves farther than the configured movement limit from the activation point.
- Stops on death, quit, world change, teleport, game mode changes, or target loss depending on config.

## Build

```powershell
mvn package
```

The plugin jar is written to `target/EnthusiaServerAutoClicker.jar`.
