import assert from 'node:assert/strict';
import { after, before, describe, it } from 'node:test';

import { formatCounts, totalOf } from '../src/bot.js';
import { applyConfig } from '../src/config.js';
import { give, killAndRespawn, openSession, resetPlayer, snapshot } from '../src/harness.js';

/**
 * A deliberately mixed loadout: full stacks, part stacks, and one unstackable tool.
 * A bug that only mishandles one of those shapes still shows up in the total.
 */
const LOADOUT = [
  { name: 'diamond', count: 12 },
  { name: 'oak_log', count: 7 },
  { name: 'golden_apple', count: 3 },
  { name: 'cobblestone', count: 45 },
  { name: 'iron_pickaxe', count: 1 },
];

const EXPECTED = new Map(LOADOUT.map(({ name, count }) => [name, count]));
const EXPECTED_TOTAL = LOADOUT.reduce((sum, { count }) => sum + count, 0);

const BEHAVIOURS = ['KEEP', 'DROP', 'INHERIT'];
const GAMERULE_STATES = [false, true];

/** Where a given combination is expected to leave the items. */
function expectedDestination(behaviour, keepInventory) {
  if (behaviour === 'KEEP') return 'inventory';
  if (behaviour === 'DROP') return 'ground';
  return keepInventory ? 'inventory' : 'ground';
}

function sorted(counts) {
  return Object.fromEntries([...counts.entries()].sort(([a], [b]) => a.localeCompare(b)));
}

describe('inventory conservation across a death', { timeout: 600_000 }, () => {
  let session;

  before(async () => { session = await openSession(); }, { timeout: 120_000 });
  after(async () => { await session?.close(); });

  for (const behaviour of BEHAVIOURS) {
    for (const keepInventory of GAMERULE_STATES) {
      const destination = expectedDestination(behaviour, keepInventory);

      it(
        `conserves every item with default_behavior=${behaviour}, gamerule keepInventory=${keepInventory}`,
        { timeout: 120_000 },
        async () => {
          const { rcon, bot } = session;

          await applyConfig(rcon, { defaultBehavior: behaviour });
          await resetPlayer(rcon, bot, { keepInventory });
          await give(rcon, bot, LOADOUT);

          const before = await snapshot(rcon, bot);
          assert.equal(
            before.total, EXPECTED_TOTAL,
            `setup is wrong: bot holds ${formatCounts(before.inventory)}`,
          );
          assert.equal(
            totalOf(before.ground), 0,
            `setup is wrong: ground is not clear, holds ${formatCounts(before.ground)}`,
          );

          await killAndRespawn(rcon, bot);

          const result = await snapshot(rcon, bot);
          const detail = `\n  inventory: ${formatCounts(result.inventory)}`
            + `\n  ground:    ${formatCounts(result.ground)}`;

          // The invariant. Items may move between the inventory and the floor, but no
          // combination of behaviour and gamerule may create or destroy any of them.
          assert.deepEqual(
            sorted(result.combined), sorted(EXPECTED),
            `items were created or destroyed across the death.${detail}`,
          );

          // Secondary, and also pure world state: the items ended up where this
          // combination says they should. Nothing here reads event.getDrops(), which an
          // in-flight change will leave empty by design.
          const held = totalOf(result.inventory);
          const dropped = totalOf(result.ground);
          if (destination === 'inventory') {
            assert.equal(held, EXPECTED_TOTAL, `expected everything kept.${detail}`);
            assert.equal(dropped, 0, `expected nothing on the ground.${detail}`);
          } else {
            assert.equal(dropped, EXPECTED_TOTAL, `expected everything dropped.${detail}`);
            assert.equal(held, 0, `expected an empty inventory.${detail}`);
          }
        },
      );
    }
  }
});
