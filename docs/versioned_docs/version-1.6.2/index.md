# BetterKeepInventory 1.6.2

<center>
![Better Keep Inventory Banner](https://workshop.beepsterr.com/img/header.png)

[![Support button](https://workshop.beepsterr.com/img/buttons/support.png)](https://beepsterr.com/discord)
</center>

:::warning[Legacy version]
1.6.2 is the last release of the 1.x line. It no longer receives fixes.
It exists for servers on Minecraft 1.17 and 1.18, which the current 2.x releases don't support. If your server can run 1.19 or newer, use the latest release instead — it replaces everything below with a far more flexible rule system.
:::

## About

Do you want to use the KeepInventory gamerule but dislike how it completely removes any risk with death? With this plugin you get to choose how punishing death should be!

1.6.2 uses a flat `config.yml` with one section per punishment, plus an `overrides` section for switching punishments off in specific worlds, towns or claims. The rule-based configuration system arrived in 2.0.

## How it works

The plugin does not turn KeepInventory on. It only reacts to deaths where the inventory is **already** being kept, and then applies the penalties you configured.

That means the `keepInventory` gamerule has to be `true` for the world:

```
/gamerule keepInventory true
```

If a death drops the player's inventory, the plugin ignores it completely.

## Features

- ✅ **Apply durability damage** to hotbar, inventory and armor separately
- ✅ **Exempt items** by material, custom name or lore
- ✅ **Control XP levels** lost on death
- ✅ **Re-apply hunger level** after respawning
- ✅ **Keep potion effects** through death, with duration and strength penalties
- ✅ **Take money from players** via [Vault](https://www.spigotmc.org/resources/vault.34315/), flat or as a percentage, optionally paid to their killer
- ✅ **Switch punishments off** per world, per damage type, and in [Towny](https://www.spigotmc.org/resources/towny-advanced.72694/) or [GriefPrevention](https://www.spigotmc.org/resources/griefprevention.1884/) territory
- ✅ **Grace period** after a death
- ✅ **[PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) support**

## BStats

![Plugin Statistics (Generated Image)](https://bstats.org/signatures/bukkit/BetterKeepInventory.svg)
