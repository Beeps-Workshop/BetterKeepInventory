# Cause
The player must have died from **ANY** of the matched damage causes.

```yaml
conditions:
  cause:
    nodes:
      - "FALL"
      - "LAVA"
      - "!VOID"
      - "ENTITY_*"
```
### nodes
Each node is matched against the [Bukkit `DamageCause`](https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/event/entity/EntityDamageEvent.DamageCause.html) of the player's last damage, written in uppercase (e.g. `FALL`, `LAVA`, `ENTITY_ATTACK`).

- `!` can be used at the start to require the death to NOT be from that cause
- `*` can be used anywhere to indicate a wildcard. for example: `ENTITY_*` would match `ENTITY_ATTACK` and `ENTITY_EXPLOSION` but not `FALL`

:::note
If the player has no recorded last damage cause (for example when killed by a plugin without dealing damage), this condition will not match.
:::

### Common causes
- `FALL` - fall damage
- `LAVA` - standing in lava
- `FIRE` / `FIRE_TICK` - burning
- `DROWNING` - out of air
- `VOID` - fell out of the world
- `ENTITY_ATTACK` - killed by a mob or player
- `ENTITY_EXPLOSION` / `BLOCK_EXPLOSION` - explosions (creepers, TNT)
- `PROJECTILE` - arrows and other projectiles
- `STARVATION` - ran out of hunger
- `MAGIC` / `POISON` / `WITHER` - status effects
