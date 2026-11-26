---
description: "Answers to common BetterKeepInventory questions: how it differs from the keepInventory gamerule, keeping only armor, per-world settings, ranks, and supported versions."
---
# FAQ

## How is this different from the keepInventory gamerule?
The `keepInventory` gamerule is all or nothing: players either keep everything or drop everything. BetterKeepInventory lets you decide what happens in between, like keeping armor but dropping resources, damaging gear, taking some XP, or using different rules per world or per rank.

## Which servers and Minecraft versions are supported?
Spigot, Paper, Purpur and Folia on Minecraft 1.21 and newer, including 26.x, with Java 21 or newer. Older releases for older Minecraft versions are on [Modrinth](https://modrinth.com/plugin/betterkeepinventory/versions).

## Does it work with the keepInventory gamerule on or off?
Both. With `default_behavior: INHERIT` (the default) each world's gamerule decides the starting point, and your rules apply on top of it. Set `default_behavior` to `KEEP` or `DROP` to ignore the gamerule. See [Configuration](./Configuration.md#default_behavior).

Rules can only take things away from players, so most setups use `KEEP` and then add rules for what should still be lost.

## Can players keep their armor but drop everything else?
Yes, see the [Keep Gear](./Rules/Examples/Keep%20Gear.md) example.

## Can I make players drop only some items?
Yes. Drop specific items or groups with [Drop Specific Items](./Rules/Examples/Drop%20Specific%20Items.md), or part of each stack with [Partial Loss](./Rules/Examples/Partial%20Loss.md).

## Can I keep inventory in one world but not another?
Yes, see the [Per World](./Rules/Examples/Per%20World.md) example.

## Can VIPs or ranks keep their inventory?
Yes, using permissions. See the [Permission Ranks](./Rules/Examples/Permission%20Ranks.md) example.

## Can players lose XP or money when they die?
Yes, with the [Exp](./Rules/Effects/Exp.md) effect, and the [Vault](./Rules/Effects/Vault.md) effect if you have Vault and an economy plugin installed.

## Can I use PlaceholderAPI in conditions?
Yes, with the [Placeholders](./Rules/Conditions/Placeholders.md) condition. It's available when PlaceholderAPI is installed. See the [Combat Tag](./Rules/Examples/Combat%20Tag.md) example for one way to use it.

## My rules don't do anything, what's wrong?
- Check that the rule has `enabled: true`. Rules are disabled when it's missing.
- Check `default_behavior`: with `INHERIT`, worlds with the gamerule off already drop everything. Use `KEEP` if your rules should decide what players lose.
- Run `/bki reload` after editing the config. If it reports an error, the old config is still in use.
- Use the debug tools on the [Troubleshooting](./Troubleshooting.md) page to see what happened on a death.

## Do I need to restart the server after changing the config?
No, `/bki reload` applies config changes. See [Commands & Permissions](./Commands.md).

## What happens to items dropped in the void?
They're gone, like any item that falls into the void. The [Death Cause](./Rules/Examples/Death%20Cause.md) example shows how to skip drops for void deaths.

## Can other plugins add their own conditions and effects?
Yes, through the [Developer API](./API.md). Run `/bki registry effects` or `/bki registry conditions` to see what's available on your server.

## Where can I get help?
Join the [Discord](https://beepsterr.com/discord) and create a ticket. Attach a debug report from `/bki debug dump`, see [Troubleshooting](./Troubleshooting.md#create-a-debug-report).
