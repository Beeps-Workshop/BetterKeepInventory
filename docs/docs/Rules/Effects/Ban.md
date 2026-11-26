# Ban
Determines what should happen to the player’s EXP level on death.
```yaml
effects:
  ban:
    message: You died! You will respawn in 1 hour
    duration: 1h
```

### message
```yml
message: "Goodbye, cruel world!"
```
The message to display as the kick reason

### duration
```yml
duration: 1d
```
How long to ban the player for

| value | duration |
| --- | --- |
| forever | No time limit. banned until manually unbanned |
| 5s | 5 seconds |
| 5m | 5 minutes (default) |
| 5h | 5 hours |
| 5d | 5 days |

You can replace the 5 with any number you’d like