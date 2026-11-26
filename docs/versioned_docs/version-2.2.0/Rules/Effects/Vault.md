# Vault
Determines what should happen to the player’s EXP level on death.
```yaml
effects:
  vault:
    mode: "SIMPLE"
      min: 50
      max: 50 #Lose $50 for dying
      allow_negative_balance: false
      # let's not be too mean, ok?
```

### mode
```yml
mode: "SIMPLE"
```
- `SIMPLE`: Lose a random amount of money between `min` and `max`
- `PERCENTAGE`: Lose a random percentage of player’s balance between `min` and `max`

### min
```yml
min: 50
```
the minimum bounds for the operation
### max
```yml
max: 50
```
the max bounds for the operation

### allow_negative_balance
```yml
allow_negative_balance: false
```
Wether the player is allowed to go into debt by dying. (Realistic)