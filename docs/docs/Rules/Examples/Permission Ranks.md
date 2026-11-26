# Permission Ranks
Give ranks better death perks with permissions. A common setup on servers that sell ranks: the higher the rank, the less a player loses when they die.

- **No rank:** drop everything except gear
- **VIP:** drop only resources
- **MVP:** keep everything

## Required Plugins
- A permissions plugin, like [LuckPerms](https://modrinth.com/plugin/luckperms)

## Rule
```yaml
default_behavior: KEEP

rules:
  not_mvp:
    name: "Everyone below MVP"
    enabled: true
    conditions:
      permissions:
        nodes:
          - "!myserver.rank.mvp"   # <-- Players WITHOUT the MVP permission
    effects:
      drop:
        mode: ALL
        filters:
          items:
            - "G:RESOURCES"        # <-- Drop resources
    children:
      not_vip:
        name: "Everyone below VIP"
        enabled: true
        conditions:
          permissions:
            nodes:
              - "!myserver.rank.vip"   # <-- ...who also don't have VIP
        effects:
          drop:
            mode: ALL
            filters:
              items:
                - "*"              # <-- Also drop everything else...
                - "!G:EQUIPMENT"   # <-- ...except gear
```

## How it works
The permission names are up to you, give them to your ranks in your permissions plugin. For example with LuckPerms:
```
/lp group mvp permission set myserver.rank.mvp true
/lp group vip permission set myserver.rank.vip true
```

The `not_mvp` rule applies to everyone without the MVP permission, and makes them drop their resources. Its [child rule](../Intro.md#children) is only checked when the parent's conditions pass, so it only applies to players who have neither MVP **nor** VIP. They lose everything else except their gear too.

MVP players don't match the first rule, so neither rule applies to them and they keep everything.

:::warning
Server operators have every permission by default, so they count as MVP. Test your ranks with an account that isn't opped.
:::

## Variations
A single "keep inventory" perk is just one rule:
```yaml
rules:
  no_keepinventory_perk:
    name: "Drop items without the perk"
    enabled: true
    conditions:
      permissions:
        nodes:
          - "!myserver.keepinventory"
    effects:
      drop:
        mode: ALL
```

:::note
A permission condition matches when the player matches **any** of its nodes, so `["!myserver.rank.vip", "!myserver.rank.mvp"]` in a single condition would match everyone who is missing **either** permission. Use child rules like above when a rule should need several permissions at once.
:::
