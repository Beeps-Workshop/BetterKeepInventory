# Command
Executes commands when the player dies and/or respawns.

```yaml
effects:
  command:
    on_death:
      - "broadcast %player% has died!"
    on_respawn:
      - "give %player% bread 5"
    executor: CONSOLE
```

### on_death
```yml
on_death:
  - "broadcast %player% was slain by %killer%!"
  - "playsound minecraft:entity.wither.spawn master %player%"
```
A list of commands to execute when the player dies. Commands are run after a 1 tick delay to ensure the death event has fully processed.

### on_respawn
```yml
on_respawn:
  - "give %player% bread 5"
  - "effect give %player% regeneration 10 1"
```
A list of commands to execute when the player respawns.

### executor
```yml
executor: "CONSOLE"
```
Who executes the commands:
- `CONSOLE`: Commands run as the server console (default)
- `PLAYER`: Commands run as the player

## Placeholders
The following placeholders are available in commands:
- `%player%` - The player's name
- `%uuid%` - The player's UUID
- `%world%` - The world name where the player died
- `%x%`, `%y%`, `%z%` - The death coordinates (block coordinates)
- `%killer%` - The killer's name (player name, mob type, or "Unknown")