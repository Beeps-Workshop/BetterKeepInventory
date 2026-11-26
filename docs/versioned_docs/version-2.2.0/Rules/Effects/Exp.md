# Exp
Determines what should happen to the player’s EXP level on death.
```yaml
effects:
  exp:
    mode: "SIMPLE"
    how: "DROP"
    min: 10.0
    max: 10.0 # Always drop 10 levels
```
### mode
```yml
mode: "PERCENTAGE"
```
- `SIMPLE`: Lose a random amount of levels between `min` and `max`
- `PERCENTAGE`: Lose a random percentage of player’s level between `min` and `max`
- `ALL`: Lose all exp levels.

### how:
```yml
how: "DROP"
```
- `DELETE`: Makes the player lose the levels with no way to recover them
- `DROP`: Spawns an EXP orb containing the levels at the players death position

### min
```yml
min: 10.0
```
the minimum bounds for the operation

### max
```yml
max: 10.0
```
the max bounds for the operation