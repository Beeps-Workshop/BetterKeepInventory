# Light Level
The player must die in a location matching the specified light level range.

```yaml
conditions:
  light_level:
    range: "< 8"
    type: ANY
```

### range
```yml
range: "< 8"
```
A range expression that defines what light levels will match. Supports the following formats:
- `< 8` - below 8 (dark areas where mobs spawn)
- `<= 7` - at or below 7
- `> 10` - above 10
- `>= 8` - at or above 8 (lit areas)
- `0..7` - between 0 and 7 (inclusive)
- `5` - exactly 5

Spaces around operators are optional (`<8` works the same as `< 8`).

### type
```yml
type: "ANY"
```
Which light level to check:
- `ANY`: Combined light level from all sources (default)
- `BLOCK`: Light from blocks only (torches, glowstone, etc.)
- `SKY`: Light from the sky only