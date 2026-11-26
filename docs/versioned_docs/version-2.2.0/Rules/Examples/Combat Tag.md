# Combat Tag
## Required Plugins
- Any Combat tagging plugin with PlaceholderAPI support (e.g. [CombatTag](https://modrinth.com/plugin/combat-tag))

## Rule
```yaml

rules:
  # ... other rules ...
  example_placeholder_rule:
    name: Example PlaceholderAPI Rule
    enabled: true
    conditions:
      placeholders:
        '1':
          placeholder: ct_in_combat # <-- PlaceholderAPI placeholder for CombatTag plugin
          operator: EQUALS
          value: 'true'
    effects:
      drop:
        mode: ALL
  # ... other rules ...
```