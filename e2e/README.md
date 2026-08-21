# End-to-end tests

These tests boot a real Paper server with the plugin installed, join it with a headless
bot, and assert on what the world looks like afterwards. They exist to cover the things
MockBukkit cannot reach: real death mechanics, the `keepInventory` gamerule interaction,
and where items and experience actually end up.

They are **not** part of the Maven build. `mvn test` does not run them, and this
directory is not a Maven module.

## What they check

The core invariant, and the reason this harness exists:

> Give the bot a known set of items, kill it, then count the items in its inventory
> **plus** the item entities lying on the ground. The total must equal what it started
> with.

That runs across `default_behavior` of `KEEP`, `DROP` and `INHERIT`, each with the world's
`keepInventory` gamerule both on and off — six combinations, none of which may create or
destroy an item. The same idea is then applied to experience: levels kept plus the value
of the orbs on the floor must equal the levels the player went in with.

Assertions are on **world state only**. Nothing here reads `event.getDrops()` or any other
server internal, deliberately: a change in flight pins `keepInventory(true)` and
distributes drops by hand, which leaves that list empty by design. Items-on-the-ground
assertions survive that; assertions about the drops list would fail for the wrong reason.

## Running them

Prerequisites:

```sh
# Node: the version in .nvmrc, which is what CI uses too.
# With nvm, from this directory:
nvm use        # installs it first with `nvm install` if you don't have it

npm ci         # `ci` not `install` -- the lockfile pins mineflayer deliberately

# A plugin jar, from the repository root:
mvn -B package -DskipTests

# A Paper server jar, from the repository root:
./scripts/get_paper_jar.sh paper 1.21.8
```

Then:

```sh
npm test
```

That prepares `e2e/.server/`, boots Paper once, runs every test file against it, and shuts
it down. A full run takes about 45 seconds, most of which is the ~10-30s server boot.

Useful variations:

| Command | What it does |
| --- | --- |
| `npm run server` | Boots the server and leaves it up. Ctrl-C to stop. |
| `node --test --test-concurrency=1 tests/conservation.test.js` | Runs one file against an already-running server. Much faster to iterate with. |
| `BKI_E2E_RCON_TRACE=1 npm test` | Logs every RCON command and its reply. |
| `BKI_E2E_REPORTER=tap npm test` | Swap the test reporter. |
| `npm run test:keep-server` | Leaves the server running after the tests finish. |

Running Paper accepts the [Minecraft EULA](https://aka.ms/MinecraftEULA); `run.js` writes
`eula=true` into the scratch server directory so the boot is unattended.

## How it is put together

| File | Role |
| --- | --- |
| `run.js` | Boots one server for the whole suite, runs the test files, shuts it down. |
| `server.properties` | The test server's configuration. Copied into `.server/` on every run. |
| `src/server.js` | Lays down `.server/`, starts Paper, waits for `Done`, stops it. |
| `src/rcon.js` | RCON connection, plus the parsing for batched `data get` replies. |
| `src/bot.js` | mineflayer connection, inventory reads, and event/condition waits. |
| `src/config.js` | Renders a scenario's `config.yml` and applies it with `/bki reload`. |
| `src/harness.js` | World setup, per-test reset, killing the bot, taking a snapshot. |
| `tests/` | The tests themselves. |

Two channels, because neither is enough alone:

- **RCON** does setup and teardown — `/clear`, `/give`, `/xp set`, `/gamerule`, `/kill`,
  `/damage`, and `/bki reload` — and reads world state out of entity NBT.
- **mineflayer** provides the player. A death needs a real player entity, which RCON
  cannot conjure, and the bot is also the cleanest read of the post-respawn inventory.

The server boots **once per suite**; state is reset per test over RCON. Paper takes tens of
seconds to start, so a boot per test is not viable.

Each test writes a full `config.yml` and reloads it, which exercises the reload path on
every single test as a side effect.

### The arena

Tests kill the bot at `(1000, 101, 1000)`, on a bedrock platform in force-loaded chunks,
and let it respawn at world spawn. The distance matters: a bot that respawns on top of its
own drops picks them up while the test is trying to count them, and the conservation check
becomes a race. Force-loading matters too, or the arena chunk unloads once the bot is away
and `@e[type=item]` stops seeing the drops.

### Waiting

Nothing in the harness sleeps for a fixed duration. Waits are either on a bot event with a
deadline, on a predicate polled per tick, or on an RCON round trip — a reply proves the
server has ticked past everything sent before it. That matters because effects schedule
delayed work: `hunger` re-applies five ticks after respawn, `command` runs one tick after
death.

## Things that will bite you

**The bot cannot die until it says it has loaded.** Since 1.21.2 the server keeps a player
invulnerable until the client sends `ServerboundPlayerLoadedPacket`; `ServerPlayer` checks
`!hasClientLoaded()` in `isInvulnerableTo`, and no damage type bypasses it — not even the
one `/kill` uses. mineflayer never sends that packet. The symptom is thoroughly misleading:
`/kill BkiBot` answers `Killed BkiBot` while the player keeps all twenty hearts, and
`/damage` answers `Target is invulnerable to the given damage type` even though every
invulnerability flag in the player's NBT is `0b`. `src/bot.js` sends `player_loaded` on
connect and again on every spawn, because the flag is cleared each time the player
respawns.

**RCON runs batched command output together with no separator.** A reply to
`execute as @e[...] run data get entity @s Item` reads
`...count: 1}Cobblestone has the following entity data: {...}` — no newline, no space.
Splitting it on newlines returns only the first entity, which is indistinguishable from a
world containing exactly one item, so the bug reads as a plausible test failure rather than
a parse error. `src/rcon.js` scans for each marker and reads one balanced, quote-aware
value instead. `tests/rcon-parsing.test.js` pins this and needs no server.

**A successful `/give` spawns a real item entity.** Vanilla drops a one-count item at the
player and immediately marks it never-pick-up with `Age: 5999s`, purely to play the pickup
animation; it despawns a tick later. Query the ground inside that tick and every setup
looks one item heavier than it is. Ground queries exclude `PickupDelay: 32767s` for this
reason.

**Pin the Minecraft version.** mineflayer tracks protocol versions through `minecraft-data`
and lags new releases. The version lives in `src/paths.js` (`MINECRAFT_VERSION`) and is
passed explicitly to `createBot`, so a mismatch fails at connect rather than as a confusing
packet error mid-test. This suite tests mechanics, not version compatibility.

**Vanilla experience is not conserved.** A vanilla death drops `min(level * 7, 100)` points
and destroys the rest, so the conservation invariant only holds for the paths the plugin
fully controls. `tests/experience.test.js` covers the plugin-controlled cases and pins the
vanilla case as deliberately lossy.

## Deliberately not asserted

Two behaviour changes are coming and are intentional, so nothing here treats the current
behaviour as correct:

- `ExpEffect` currently calls `setExp(0)`, discarding partial level progress; it will start
  preserving it. The experience tests therefore start on exact level boundaries, where
  there is no partial progress to lose either way.
- In `DROP` mode `event.getDrops()` will stay empty, so other plugins observing it see
  nothing. No test reads that list.
