# World Guard Region
## Required Plugins
- PlaceholderAPI (With [World Guard Expansion](https://api.extendedclip.com/expansions/worldguard/))

## Rule
```yaml

rules:
  in_worldguard_region:
    name: "Must be in world_guard region"
    enabled: true
    conditions:
      placeholders:
        '1':
          placeholder: worldguard_region_name
          operator: EQUALS
          value: 'my_region_name' # <-- Change this to the name of your region
    effects:
      drop:
        mode: ALL

```