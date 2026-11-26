# Teams
## Required Plugins
- PlaceholderAPI (With [Teams Expansion](https://discord.com/channels/850836877924761610/1438021729636061287/1438023006030135326))

## Rule
```yaml

rules:
  team_drops_everything_example:
    name: "Team Drops Everything Example"
    enabled: true
    conditions:
        placeholders:
          1:
            placeholder: "team_name"
            operator: "EQUALS"
            value: "Blue" # <-- Change this to the name of your team
    effects:
      drop:
        mode: "ALL"
        filters:
          items:
            - "G:TOOLS"
            - "G:WEAPONS"   # <-- Drop any tools & weapons the player has
          name:
            - '!Soulbound'  # <-- Unless they have "Soulbound" in the name

```