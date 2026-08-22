import assert from 'node:assert/strict';
import { after, before, describe, it } from 'node:test';

import { applyConfig } from '../src/config.js';
import { give, killAndRespawn, openSession, resetPlayer, snapshot } from '../src/harness.js';

/**
 * Conservation when only *some* of the inventory moves.
 *
 * The baseline conservation tests move everything or nothing, which cannot catch a bug that
 * loses an item on the boundary between the two buckets -- an off-by-one in the slot handling,
 * or a stack that is removed from one side without arriving on the other.
 *
 * Which items a filter selects is unit-tested and not re-checked here. What matters is that
 * whatever it selects, nothing is created or destroyed on the way.
 */

const LOADOUT = [
  { name: 'diamond', count: 12 },
  { name: 'cobblestone', count: 45 },
  { name: 'oak_log', count: 7 },
];

const DROP_ONLY_COBBLESTONE = {
  drop_the_rubble: {
    name: 'drop the rubble',
    enabled: true,
    effects: {
      drop: { mode: 'ALL', filters: { items: ['COBBLESTONE'] } },
    },
  },
};

describe('conservation on a partial drop', { timeout: 600_000 }, () => {
  let session;

  before(async () => { session = await openSession(); }, { timeout: 120_000 });
  after(async () => { await session?.close(); });

  it('moves only the filtered items, and loses none of the rest', { timeout: 120_000 }, async () => {
    const { rcon, bot } = session;

    // KEEP so the only thing moving anything is the rule itself.
    await applyConfig(rcon, { defaultBehavior: 'KEEP', rules: DROP_ONLY_COBBLESTONE });
    await resetPlayer(rcon, bot, { keepInventory: false });
    await give(rcon, bot, LOADOUT);

    await killAndRespawn(rcon, bot);
    const result = await snapshot(rcon, bot);

    for (const { name, count } of LOADOUT) {
      assert.equal(
        result.combined.get(name) ?? 0, count,
        `${name} was created or destroyed across the death`,
      );
    }

    assert.equal(result.ground.get('cobblestone') ?? 0, 45, 'the filtered item should have dropped');
    assert.equal(result.inventory.get('diamond') ?? 0, 12, 'diamonds do not match the filter');
    assert.equal(result.inventory.get('oak_log') ?? 0, 7, 'logs do not match the filter');
    assert.equal(result.ground.get('diamond') ?? 0, 0, 'nothing unfiltered should have dropped');
    assert.equal(result.ground.get('oak_log') ?? 0, 0, 'nothing unfiltered should have dropped');
  });
});
