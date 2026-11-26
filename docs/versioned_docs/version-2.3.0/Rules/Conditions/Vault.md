# Vault
The player's economy balance (via Vault) must be **at least** the configured minimum.

```yaml
conditions:
  vault:
    min_balance: 100.0
```
:::warning
Requires [Vault](https://www.spigotmc.org/resources/vault.34315/) and a compatible economy plugin to be installed
:::

### min_balance
The minimum balance the player must hold for the check to pass