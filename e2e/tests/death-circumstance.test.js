import assert from 'node:assert/strict';
import { after, before, describe, it } from 'node:test';

import { createBot, waitUntil } from '../src/bot.js';
import { applyConfig } from '../src/config.js';
import { ARENA, give, killAndRespawn, openSession, resetPlayer, snapshot } from '../src/harness.js';
import { run } from '../src/rcon.js';

/**
 * What the context captured about *how* the player died.
 *
 * Whether one player hitting another produces `ENTITY_ATTACK` is a question about Minecraft,
 * not about this plugin -- the condition only reads an enum. What is worth checking here is
 * that the cause and the killer are populated at the moment our handler runs, and that they
 * still mean the same thing once the player has respawned somewhere else.
 */

const KILLER_USERNAME = 'BkiKiller';
const LOADOUT = [{ name: 'diamond', count: 4 }];

function dropOnCause(causes) {
  return {
    drop_on_cause: {
      name: 'drop on cause',
      enabled: true,
      conditions: { cause: { nodes: causes } },
      effects: { drop: { mode: 'ALL' } },
    },
  };
}

describe('the circumstances of a death', { timeout: 600_000 }, () => {
  let session;

  before(async () => { session = await openSession(); }, { timeout: 120_000 });
  after(async () => { await session?.close(); });

  it('matches a rule against the cause the player actually died of',
    { timeout: 120_000 }, async () => {
      const { rcon, bot } = session;

      await applyConfig(rcon, { defaultBehavior: 'KEEP', rules: dropOnCause(['LAVA']) });
      await resetPlayer(rcon, bot, { keepInventory: false });
      await give(rcon, bot, LOADOUT);

      await killAndRespawn(rcon, bot, { kind: 'damage', type: 'minecraft:lava' });
      const result = await snapshot(rcon, bot);

      assert.equal(result.ground.get('diamond') ?? 0, 4, 'a lava death should have matched LAVA');
      assert.equal(result.inventory.get('diamond') ?? 0, 0);
    });

  it('does not match a rule against a cause the player did not die of',
    { timeout: 120_000 }, async () => {
      const { rcon, bot } = session;

      await applyConfig(rcon, { defaultBehavior: 'KEEP', rules: dropOnCause(['LAVA']) });
      await resetPlayer(rcon, bot, { keepInventory: false });
      await give(rcon, bot, LOADOUT);

      await killAndRespawn(rcon, bot, { kind: 'damage', type: 'minecraft:fall' });
      const result = await snapshot(rcon, bot);

      assert.equal(result.inventory.get('diamond') ?? 0, 4, 'a fall death should not have matched LAVA');
      assert.equal(result.ground.get('diamond') ?? 0, 0);
    });

  /**
   * The killer is read straight back out of the context, via the command effect's `%killer%`
   * placeholder -- the only thing that makes it observable from outside the plugin.
   *
   * `/damage ... by <entity>` attributes the blow without needing the two bots to actually
   * fight, which keeps reach and combat timing out of the test.
   */
  it('captures who did the killing', { timeout: 180_000 }, async () => {
    const { rcon, bot } = session;

    await applyConfig(rcon, {
      defaultBehavior: 'KEEP',
      rules: {
        announce_killer: {
          name: 'announce killer',
          enabled: true,
          effects: {
            command: { executor: 'CONSOLE', on_death: ['say BKI_KILLER=%killer%'] },
          },
        },
      },
    });
    await resetPlayer(rcon, bot, { keepInventory: false });

    const killer = await createBot({ username: KILLER_USERNAME });
    try {
      await run(rcon, `gamemode survival ${KILLER_USERNAME}`);
      await run(rcon, `tp ${KILLER_USERNAME} ${ARENA.x} ${ARENA.y} ${ARENA.z}`);

      // The victim hears the broadcast the command effect makes.
      let announced = null;
      const onMessage = (message) => {
        const text = message.toString();
        const match = text.match(/BKI_KILLER=(\S+)/);
        if (match) announced = match[1];
      };
      bot.on('message', onMessage);

      try {
        await killAndRespawn(rcon, bot, {
          kind: 'damage',
          type: `minecraft:player_attack by ${KILLER_USERNAME}`,
        });

        await waitUntil(bot, () => announced !== null, {
          label: 'the killer announcement',
        });

        assert.equal(
          announced, KILLER_USERNAME,
          'the context did not capture who dealt the killing blow',
        );
      } finally {
        bot.off('message', onMessage);
      }
    } finally {
      try { killer.quit(); } catch { /* already gone */ }
    }
  });
});
