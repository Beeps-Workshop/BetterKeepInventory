# Placeholders

This condition lets you define a list of placeholders to match, if ANY of the conditions match, the check is passed
```yaml
conditions:
  placeholders:
    1:
      placeholder: "player_first_join"
      operator: ">="
      value: "1717790357452"
```
:::warning
Requires [PlaceholderAPI](https://modrinth.com/plugin/placeholderapi) to be installed
:::

### placeholder
The name of a placeholder, Please refer to the PlaceholderAPI documentation for a list of available placeholders: https://wiki.placeholderapi.com/
### operator

The operator to use when comparing the placeholder value to the provided value. Must be one of the following values:
- `EQUALS` or `==`: Must be the same
- `NOT` or `!=`: Must not be the same
- `INCLUDES`: Must contain the value
- `BEGINS_WITH` or `.*`: Must start with the value
- `STOPS_WITH` or `*.`: Must end with the value
- `GREATER_THAN` or `>`: Must be more than the value
- `LESS_THAN` or `<`: Must be less than the value
- `GREATER_THAN_OR_EQUALS` or `>=`: Must be more or exactly the same as the value
- `LESS_THAN_OR_EQUALS` or `<=`: Must be less than or exactly the same as the value
### value
The value to test the placeholder for. The type of this value depends on the placeholder being used. For example, a placeholder that returns a number should be compared against a number. The value will always be parsed from the point of view of the deceased player.