# Drop
Determines the items that should still be dropped on death.
```yaml
effects:
  drop:
    mode: "PERCENTAGE"
    min: 10.0
    max: 25.0 # Between 10% and 25%
    filters:
      items:
        - "SHEARS"
        - "G:RESOURCES"
      slots:
        - "ALL"
      name:
        - '!*Crate Key*'
      lore:
        - '!*Soulbound*'
```
### mode
```yml
mode: "PERCENTAGE"
```
- `SIMPLE`: Drops a random amount of items between `min` and `max` from each stack.
- `PERCENTAGE`: Drops a percentage of items between `min` and `max` from each stack.
- `ALL`: Drops all items.

:::note
`SIMPLE` and `PERCENTAGE` work per stack and round down, so they never drop items that don't stack (like tools and armor). For example, 25% of a single sword is 0.25 swords, which rounds down to nothing. Use `ALL` with a filter to drop those.
:::

### min
```yml
min: 10.0
```
the minimum bounds for the operation

### max
```yml
max: 10.0
```
the max bounds for the operation

### Filters:
```yml
filters:
  items:
    - "SHEARS"
    - "G:RESOURCES"
  slots:
    - "ALL"
```

Allows you to make sure the effect only applies to certain items based on various criteria.
<details>
<summary><strong>Filter options</strong></summary>

### Items
```yaml
items:
  - "SHEARS"
  - "G:RESOURCES"
```
A List of items that are affected by this effects.
Defaults to all items.

Supports [Resource Lists](../../Types/ResourceLists.md) for item groups.

### Slots
```yaml
slots:
  - "ALL"
```
A list of slots that are affected by this effect.
Defaults to all slots.

### Name
```yaml
name:
  - '!*Crate Key*'
```
A string or list of strings matched against the item's custom name. Items without a custom name are matched as an empty name.
- An item is affected if its name matches **any** entry.
- Entries must match the **whole** name and are case-sensitive. Use `*` as a wildcard, e.g. `*Key*` matches any name containing `Key`.
- `!` at the start matches names that do **not** match the rest, e.g. `!*Soulbound*` affects every item without `Soulbound` in its name.

:::note
Because an item only has to match one entry, several `!` entries don't stack: `['!*Key*', '!*Soulbound*']` still affects a `Crate Key`, since it doesn't contain `Soulbound`. Unlike item lists, there's no `*` then `!` pattern here.
:::

### Lore
```yaml
lore:
  - '!*Soulbound*'
```
A string or list of strings matched against each line of the item's lore, using the same matching as `name`.
- An item is skipped if **any** of its lore lines doesn't match an entry.
- Items without lore are always affected.

This makes it most useful for protecting items: `!*Soulbound*` affects every item, except ones with a lore line containing `Soulbound`.
</details>