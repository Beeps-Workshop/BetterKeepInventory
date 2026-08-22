import assert from 'node:assert/strict';
import { after, before, describe, it } from 'node:test';

import { applyConfig } from '../src/config.js';
import { give, killAndRespawn, openSession, resetPlayer } from '../src/harness.js';
import { run } from '../src/rcon.js';

/**
 * Durability written by the damage effect has to survive the death itself.
 *
 * The unit tests already cover the arithmetic -- which mode produces which number, unbreaking,
 * `dont_break`, the elytra case. What they cannot answer is whether the damaged item is the one
 * that actually reaches the player or the ground afterwards, which depends on the application
 * step handing over the same stack the effect mutated rather than a copy of the original.
 */

const DAMAGE = 5;

const DAMAGE_RULE = {
  wear_gear: {
    name: 'wear gear',
    enabled: true,
    effects: {
      damage: { mode: 'SIMPLE', min: DAMAGE, max: DAMAGE },
    },
  },
};

/** Durability of a dropped item, read straight off the entity. */
async function droppedDamage(rcon, itemId) {
  const raw = await run(
    rcon,
    `execute as @e[type=item,nbt={Item:{id:"minecraft:${itemId}"}}] `
    + 'run data get entity @s Item.components."minecraft:damage"',
  );
  const match = raw.match(/:\s*(\d+)/);
  return match ? Number(match[1]) : null;
}

describe('damage survives the death', { timeout: 600_000 }, () => {
  let session;

  before(async () => { session = await openSession(); }, { timeout: 120_000 });
  after(async () => { await session?.close(); });

  it('is still on the item the player keeps', { timeout: 120_000 }, async () => {
    const { rcon, bot } = session;

    await applyConfig(rcon, { defaultBehavior: 'KEEP', rules: DAMAGE_RULE });
    await resetPlayer(rcon, bot, { keepInventory: false });
    await give(rcon, bot, [{ name: 'iron_pickaxe', count: 1 }]);

    const before = bot.inventory.slots.find((s) => s && s.name === 'iron_pickaxe');
    assert.equal(before.durabilityUsed, 0, 'setup is wrong: the pickaxe was already damaged');

    await killAndRespawn(rcon, bot);

    const after = bot.inventory.slots.find((s) => s && s.name === 'iron_pickaxe');
    assert.ok(after, 'the pickaxe should still be in the inventory under KEEP');
    assert.equal(
      after.durabilityUsed, DAMAGE,
      'the kept item is not the one the effect damaged',
    );
  });

  it('is still on the item that hits the ground', { timeout: 120_000 }, async () => {
    const { rcon, bot } = session;

    await applyConfig(rcon, { defaultBehavior: 'DROP', rules: DAMAGE_RULE });
    await resetPlayer(rcon, bot, { keepInventory: false });
    await give(rcon, bot, [{ name: 'iron_pickaxe', count: 1 }]);

    await killAndRespawn(rcon, bot);

    assert.equal(
      await droppedDamage(rcon, 'iron_pickaxe'), DAMAGE,
      'the dropped item is not the one the effect damaged',
    );
  });
});
