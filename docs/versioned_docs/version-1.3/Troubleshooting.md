# Troubleshooting

## Nothing happens when players die

Work through these in order:

### Is the plugin loaded?
Does it show up green in `/pl`? If it isn't there, the jar is in the wrong place or the server hasn't been restarted.

### Is `keepInventory` on?
This is by far the most common cause. The plugin only acts on deaths where the inventory is **already kept** — if the death drops the player's items, it does nothing at all.

```
/gamerule keepInventory true
```

The gamerule is per-world, so check the world you actually died in.

### Is anything configured?
Every `mode` ships as `NONE`, which means "leave this alone". A fresh install is deliberately a no-op. Run `/bki info` — sections showing `VANILLA` are switched off.

### Is `main.enabled` true?
`/bki info` says so outright if it isn't.

### Does the player have a bypass?
`betterkeepinventory.bypass` skips everything. Operators are **not** granted it by default, but check your permissions plugin for wildcard nodes like `betterkeepinventory.*`, which would.

### Turn on debug
Set `main.debug: true` and reload. Every death then logs which slots were handled, the damage that was rolled, and why an item was skipped.

## Known issues

1.3 is unmaintained, so these are documented rather than fixed. Each one is resolved in a later release.

### `use_enchantments` is inverted
Unbreaking is only taken into account when `use_enchantments` is set to **`false`**. If you want enchanted gear to take reduced damage, set it to `false`.

### `dont_break: false` destroys whole stacks
When an item runs out of durability, 1.3 clears the entire inventory slot rather than removing one item, so a stack is lost in full. Use `dont_break: true` unless you want that.

### Items can become unstackable
With `mode: "SIMPLE"`, damage is applied without first checking whether the item has any durability to lose. Items that can't be damaged still get damage data written to them, which makes them refuse to stack with ordinary copies.

`mode: "PERCENTAGE"` is not affected, because a percentage of zero durability is always zero. This was fixed in 1.6.2.

### Creative-mode players are still punished
Deaths in creative mode are logged as ignored but processed anyway.

### `hunger.extra_removed_on_death` does nothing
The option ships in the default config but is never read.

### `potions.duration_penalty` is missing from the config
The plugin reads it and `/bki info` reports it, but it isn't in the default file. Add it under `potions:` yourself if you want a flat duration penalty.

### Everything applies everywhere
1.3 has no per-world, per-region or damage-type control. Any world with `keepInventory` on gets the same treatment. Per-world overrides were added in 1.4 and expanded in 1.6.

## Still having issues?

Feel free to join the [Discord](https://beepsterr.com/discord) for help! Just `#create-a-ticket` and i'll help you out!

Do keep in mind that 1.3 is only supported for servers that genuinely cannot run anything newer.
