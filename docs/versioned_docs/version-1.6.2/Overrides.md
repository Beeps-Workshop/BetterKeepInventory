# Overrides

Overrides switch punishments **off** in a given context — a world, a kind of death, a town, or a land claim. Everything lives under the `overrides` section of `config.yml`.

An override never adds a punishment, it only removes one. If any override matches, that punishment is skipped for that death.

Use [`/bki test`](./Commands.md#bki-test) while standing somewhere to see which punishments are currently overridden there.

## Punishment keys

Every override is a list of these keys.

| Key         | Disables                                              |
|-------------|-------------------------------------------------------|
| `ALL`       | The plugin entirely — the death is treated as vanilla KeepInventory |
| `ITEMS`     | All durability damage (armor, hotbar and inventory)    |
| `ARMOR`     | Durability damage to worn armor                        |
| `HOTBAR`    | Durability damage to the hotbar and off-hand           |
| `INVENTORY` | Durability damage to the main inventory                |
| `EXP`       | Losing XP levels                                       |
| `EXP_LEVEL` | Resetting progress towards the next level              |
| `POTIONS`   | Carrying potion effects through death                  |
| `HUNGER`    | Restoring the hunger level on respawn                  |
| `ECO`       | Losing money                                           |
| `NONE`      | Nothing. A placeholder for "no override here"          |

:::info
`ALL` is checked before anything else runs, so it also stops the plugin from recording the hunger level and potion effects. A player who dies in an `ALL` world respawns exactly as vanilla would leave them.
:::

## Worlds

```yaml
overrides:
  worlds:
    world_disabled:
      - ALL
    world_no_exp_loss:
      - EXP
      - EXP_LEVEL
```

The key is the world name as the server knows it — the folder name, matched exactly and case-sensitively.

### Every world at once

The special world name `all` applies on top of whatever the current world says:

```yaml
overrides:
  worlds:
    all:
      - ECO
```

That switches the money penalty off server-wide while leaving every other setting per-world.

### By damage type

Instead of a plain list, a world can carry a `damage_types` map, which only applies when the player died that way:

```yaml
overrides:
  worlds:
    world_no_pvp:
      damage_types:
        PVP:
          - ITEMS
        FALL:
          - EXP
```

In `world_no_pvp`, players killed by another player keep their gear undamaged, and players who fall to their death keep their levels.

Valid damage types are the names from the [Bukkit DamageCause list](https://hub.spigotmc.org/javadocs/spigot/org/bukkit/event/entity/EntityDamageEvent.DamageCause.html), plus two extras:

| Type  | Matches |
|-------|---------|
| `PVP` | Any death where the last damage came from another player. Takes the place of the normal cause, so use `PVP` rather than `ENTITY_ATTACK` for player kills |
| `ANY` | Any death, whatever the cause |

`damage_types` can be combined with the `all` world, and with a plain list on the same world:

```yaml
overrides:
  worlds:
    all:
      damage_types:
        VOID:
          - ALL
```

Nobody is ever punished for falling out of the world, in any world.

## Towny

Requires [Towny](https://www.spigotmc.org/resources/towny-advanced.72694/). Set `enabled: true` to switch the integration on — without it the whole section is skipped.

```yaml
overrides:
  towny:
    enabled: false
    towns:
      wilderness:
        - NONE
      any_town:
        - ECO
      player_town:
        - ALL
      'North Appletown':
        - ITEMS
        - HUNGER
    nations:
      any_nation:
        - ECO
      player_nation:
        - NONE
      'The Empire':
        - ITEMS
        - HUNGER
```

### towns

| Key            | Matches when the player dies…                          |
|----------------|--------------------------------------------------------|
| `wilderness`   | …outside any town                                       |
| `any_town`     | …inside any town at all                                 |
| `player_town`  | …inside the town they are a member of                   |
| *a town name*  | …inside that specific town. Quote names containing spaces |

### nations

| Key              | Matches when the player dies…                        |
|------------------|------------------------------------------------------|
| `any_nation`     | …inside any nation's territory                        |
| `player_nation`  | …inside the nation they belong to                     |
| *a nation name*  | …inside that specific nation                          |

## GriefPrevention

Requires [GriefPrevention](https://www.spigotmc.org/resources/griefprevention.1884/). Set `enabled: true` to switch the integration on.

```yaml
overrides:
  grief_prevention:
    enabled: false
    claims:
      own:
        - ALL
      admin:
        - NONE
      edit:
        - NONE
      build:
        - NONE
      inventory:
        - NONE
      access:
        - NONE
      manage:
        - NONE
      any:
        - NONE
```

The keys are matched against the claim the player died in. Only claims count — deaths outside any claim never match this section.

| Key         | Matches when the player dies in…                            |
|-------------|-------------------------------------------------------------|
| `own`       | …a claim they own                                            |
| `admin`     | …an admin claim                                              |
| `edit`      | …a claim where they were explicitly granted Edit permission  |
| `build`     | …a claim where they were explicitly granted Build permission |
| `inventory` | …a claim where they were explicitly granted Inventory permission |
| `access`    | …a claim where they were explicitly granted Access permission |
| `manage`    | …a claim where they were explicitly granted Manage permission |
| `any`       | …any claim                                                   |

:::info
The permission keys check explicitly granted trust. A claim owner is matched by `own`, not by `build` or `edit`.
:::

The default config gives players a safe haven on their own land: `own: [ALL]` means dying inside your own claim carries no penalty at all.
