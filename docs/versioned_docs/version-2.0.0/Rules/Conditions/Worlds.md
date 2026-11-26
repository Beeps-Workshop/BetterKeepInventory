# Worlds
The player must die in **ANY** of the matched worlds.

```yaml
conditions:
  worlds:
    nodes:
        - "world"
        - "!peaceful_world"
        - "arena_*"
```
### nodes
- `!` can be used at the start to require the player to NOT be in that world
- `*` can be used anywhere to indicate a wildcard. for example: `arena_*` would match `arena_overworld` but not `lobby_overworld`