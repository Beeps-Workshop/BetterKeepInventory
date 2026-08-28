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
 *
 * The effect only touches what the player is keeping. Damaging items already bound for the
 * ground would be new behaviour rather than a fix: under DROP with the world gamerule off --
 * the usual case -- it has never done so. Gear that should be beaten up *and then* dropped is
 * expressed by ordering `damage` before a `drop` effect, which is what the second test covers.
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

  it('reaches the ground damaged when the rule damages before it drops',
    { timeout: 120_000 }, async () => {
      const { rcon, bot } = session;

      // Config order is execution order, so `damage` runs while the pickaxe is still in a slot
      // and `drop` then moves the damaged stack out. This is how a rule asks for worn gear on
      // the floor.
      await applyConfig(rcon, {
        defaultBehavior: 'KEEP',
        rules: {
          wear_then_drop: {
            name: 'wear then drop',
            enabled: true,
            effects: {
              damage: { mode: 'SIMPLE', min: DAMAGE, max: DAMAGE },
              drop: { mode: 'ALL' },
            },
          },
        },
      });
      await resetPlayer(rcon, bot, { keepInventory: false });
      await give(rcon, bot, [{ name: 'iron_pickaxe', count: 1 }]);

      await killAndRespawn(rcon, bot);

      assert.equal(
        await droppedDamage(rcon, 'iron_pickaxe'), DAMAGE,
        'the dropped item is not the one the effect damaged',
      );
    });

  it('leaves items alone that were already bound for the ground',
    { timeout: 120_000 }, async () => {
      const { rcon, bot } = session;

      // Under DROP nothing is being kept, so there is nothing for the effect to act on. Pinned
      // deliberately: this is the behaviour 2.x had with the gamerule off, and reversing it
      // would be a new feature rather than a fix.
      await applyConfig(rcon, { defaultBehavior: 'DROP', rules: DAMAGE_RULE });
      await resetPlayer(rcon, bot, { keepInventory: false });
      await give(rcon, bot, [{ name: 'iron_pickaxe', count: 1 }]);

      await killAndRespawn(rcon, bot);

      assert.equal(
        await droppedDamage(rcon, 'iron_pickaxe'), null,
        'an item that was never kept should carry no damage component at all',
      );
    });
});
