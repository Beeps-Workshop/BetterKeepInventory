# Hunger
Determines how much hunger the player loses on death.
```yaml
effects:
  hunger:
    min: 6
    amount: 2
```

### min
```yml
min: 6
```
:::warning
Not recommended to set this value too low as it may lead to starvation immediately after respawning
:::
The minimum hunger level the player should respawn with

### amount
```yml
amount: 2
```
The amount of hunger points to lose by dying