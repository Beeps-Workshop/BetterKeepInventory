# Lightning
Strikes lightning at the death and/or respawn location.

```yaml
effects:
  lightning:
    on_death: true
    on_respawn: false
    damage: false
```

### on_death
```yml
on_death: true
```
Whether to strike lightning at the death location. Default: `true`

### on_respawn
```yml
on_respawn: false
```
Whether to strike lightning at the respawn location. Default: `false`

### damage
```yml
damage: false
```
Whether the lightning should deal damage:
- `false`: Visuals only. (Strike & Sound. No damage to entities and no fire started.)
- `true`: Real lightning. (Strike, Sound, Damage, and Fire.)

Default: `false`