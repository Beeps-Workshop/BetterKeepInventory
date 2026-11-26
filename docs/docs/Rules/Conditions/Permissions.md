# Permissions
The player must have **ANY** of the matched permissions.
```yaml
conditions:
  permissions:
    nodes:
      - "player.keepinventory"
      - "player.admin"
```
### nodes
- `!` can be used at the start to require the player to NOT have said permission