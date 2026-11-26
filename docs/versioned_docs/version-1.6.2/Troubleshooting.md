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

### Is an override in the way?
Stand where the deaths happen and run `/bki test`. Anything marked ✔ is switched off there. See [Overrides](./Overrides.md).

### Does the player have a bypass?
`betterkeepinventory.bypass` skips everything. Operators are **not** granted it by default, but check your permissions plugin for wildcard nodes like `betterkeepinventory.*`, which would.

### Are they still in their grace period?
A death within `main.grace` seconds of the previous one is ignored entirely. Check `%BetterKeepInventory_grace_timer_active%` or set `grace: false` while testing.

### Turn on debug
Set `main.debug: true` and reload. Every death then logs which slots were handled, the damage that was rolled, why an item was skipped, and every override that was evaluated.

## My config lost all its comments

Expected on the first migration. Your settings were kept and the previous file is sitting next to it as `config.old.yml`. See [Installing](./Installing.md#about-configoldyml).

## Money isn't being taken

- `eco.enabled` must be `true` — it ships as `false`.
- [Vault](https://www.spigotmc.org/resources/vault.34315/) **and** an economy plugin must both be installed. Without Vault the console logs a warning on every death.
- Players below `eco.min_balance` are never charged.

## Known issues

1.6.2 is unmaintained, so these are documented rather than fixed. All of them are resolved in 2.x.

### `grace: 0` does not disable the grace period
It leaves a window of up to one second. Use `grace: false` instead.

### Creative-mode players are still punished
Deaths in creative mode are logged as ignored but processed anyway.

### The Essentials integration does nothing
The plugin lists Essentials as a soft dependency and looks for the `essentials.keepinv` permission, but the check never takes effect — deaths that don't already keep the inventory are always skipped, whatever Essentials says.

### `hunger.extra_removed_on_death` does nothing
The option ships in the default config but is never read.

### `eco.max_amount` does nothing
The option ships in the default config but is never read, so percentage-based losses are uncapped.

### `potions.duration_penalty` is missing from the config
The plugin reads it and `/bki info` reports it, but it isn't in the default file. Add it under `potions:` yourself if you want a flat duration penalty.

### `betterkeepinventory.bypass` lists a node that doesn't exist
Its child list names `betterkeepinventory.bypass.econ` while the real node is `betterkeepinventory.bypass.eco`. See [Permissions](./Permissions.md).

## Still having issues?

Feel free to join the [Discord](https://beepsterr.com/discord) for help! Just `#create-a-ticket` and i'll help you out!

Do keep in mind that 1.6.2 is only supported for servers that genuinely cannot run anything newer.
