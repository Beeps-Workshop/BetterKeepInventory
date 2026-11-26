# About Rules
Rules are the bread and butter of BetterKeepInventory. They allow you to customize exactly what happens on player death based on a variety of conditions and effects.
Rules can be very complex, so don't be afraid to check out the [Examples](../Rules/Examples/Teams.md) section for inspiration!
## Order of operations
Rules are applied from top to bottom, meaning that if you have a rule at the top that makes a player drop their inventory, and a rule at the bottom that damages items the latter will not apply because the items had already been dropped.

## Fields
Each rule consists of the following components:

### name
```yaml
name: "Example Rule"
```
A unique identifier for the rule. This is used for reference in logs and debugging.
### enabled
```yaml
enabled: true
```
Specifies whether the rule is active. Set to `true` to enable the rule, or `false` to disable it.

### conditions
```yaml
    conditions:
      worlds:
        nodes:
        - world
      ...
```
A list of conditions that must be met for the rule to apply. Other plugins can use the API to add additional conditions. Please use `/betterkeepinventory registry conditions` to see a list of available conditions for your specific server.
### effects
```yaml
    effects:
      drop:
        mode: "ALL"
        items:
          - "DIAMOND"
      ...
```
A list of conditions that must be met for the rule to apply. Other plugins can use the API to add additional conditions. Please use `/betterkeepinventory registry conditions` to see a list of available conditions for your specific server.

### children
```yaml
    children:
      example_child_rule:
        name: "Example Child Rule"
        enabled: true
        conditions:
          ...
        effects:
          ...
```
Child rules are sub-rules that are only evaluated if the parent rule's conditions are met. They allow for more granular control and advanced configurations. Child rules follow the same structure as parent rules, with their own `name`, `enabled`, `conditions`, `effects`, and even their own `children`.