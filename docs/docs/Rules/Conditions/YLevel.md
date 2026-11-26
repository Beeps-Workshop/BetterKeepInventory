# Y Level
The player must die at a Y coordinate (height) matching the specified range.

```yaml
conditions:
  y_level:
    range: "< 0"
```

### range
```yml
range: "< 0"
```
A range expression that defines what Y levels will match. Supports the following formats:
- `< 0` - below Y 0 (deep underground/void)
- `<= -60` - at or below Y -60
- `> 200` - above Y 200
- `>= 64` - at or above Y 64 (surface level)
- `-64..0` - between Y -64 and 0 (inclusive)
- `100` - exactly Y 100