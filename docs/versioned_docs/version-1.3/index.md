# BetterKeepInventory 1.3

<center>
![Better Keep Inventory Banner](https://workshop.beepsterr.com/img/header.png)

[![Support button](https://workshop.beepsterr.com/img/buttons/support.png)](https://beepsterr.com/discord)
</center>

:::warning[Legacy version]
1.3 is the last build that runs on Minecraft 1.16 and below. It no longer receives fixes.
If your server can run a newer Minecraft version, use the latest release instead — it is faster, far more configurable, and has none of the [known issues](./Troubleshooting.md#known-issues) listed here.
:::

## About

Do you want to use the KeepInventory gamerule but dislike how it completely removes any risk with death? With this plugin you get to choose how punishing death should be!

1.3 uses a flat `config.yml` with one section per punishment. There are no rules, conditions or per-world settings — those arrived in later versions.

## How it works

The plugin does not turn KeepInventory on. It only reacts to deaths where the inventory is **already** being kept, and then applies the penalties you configured.

That means the `keepInventory` gamerule has to be `true` for the world:

```
/gamerule keepInventory true
```

If a death drops the player's inventory, the plugin ignores it completely.

## Features

- ✅ **Apply durability damage** to hotbar, inventory and armor separately
- ✅ **Control XP levels** lost on death
- ✅ **Re-apply hunger level** after respawning
- ✅ **Keep potion effects** through death, with duration and strength penalties
- ✅ **Take money from players** via [Vault](https://www.spigotmc.org/resources/vault.34315/)
- ✅ **Per-punishment bypass permissions**

## BStats

![Plugin Statistics (Generated Image)](https://bstats.org/signatures/bukkit/BetterKeepInventory.svg)
