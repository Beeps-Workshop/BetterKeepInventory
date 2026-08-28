import assert from 'node:assert/strict';
import { after, before, describe, it } from 'node:test';

import { settledInventory } from '../src/bot.js';
import { applyConfig } from '../src/config.js';
import { CANARY_ITEM, give, killAndRespawn, openSession, resetPlayer } from '../src/harness.js';

/**
 * Effects must leave alone what they were not asked to touch.
 *
 * In 1.6.1 the damage effect wrote a damage component onto items that have no durability at
 * all. It did not surface as wrong damage -- it surfaced as cobblestone that would no longer
 * stack, because a stack carrying that component will not merge with an identical stack
 * without it.
 *
 * The technique here is to compare a survivor against a pristine copy of itself. Give the bot
 * the control item, put it through a death, then hand it an identical one afterwards. If the
 * survivor came out of the death unchanged the two merge into a single stack; if anything at
 * all was written onto it -- the known damage component or some future equivalent nobody
 * thought to check for -- they cannot merge and sit in two slots.
 *
 * That makes the assertion blind to *what* the side effect was, which is the point. Asserting
 * "the NBT has no damage field" would only ever catch the bug we already know about.
 */

/** Damages everything it can, with no filters -- so only the durability check protects the control item. */
const UNFILTERED_DAMAGE_RULE = {
  damage_everything: {
    name: 'damage everything',
    enabled: true,
    effects: {
      damage: { mode: 'SIMPLE', min: 5, max: 5 },
    },
  },
};

/** How many separate slots hold this item. One means it stacked; two means it did not. */
function slotsHolding(bot, name) {
  return bot.inventory.slots.filter((slot) => slot && slot.name === name).length;
}

function stackIn(bot, name) {
  return bot.inventory.slots.find((slot) => slot && slot.name === name);
}

describe('effects leave untouched items untouched', { timeout: 600_000 }, () => {
  let session;

  before(async () => { session = await openSession(); }, { timeout: 120_000 });
  after(async () => { await session?.close(); });

  for (const keepInventory of [false, true]) {
    it(`does not modify an item with no durability, gamerule keepInventory=${keepInventory}`,
      { timeout: 120_000 }, async () => {
        const { rcon, bot } = session;

        // KEEP so the control item stays in the inventory to be compared against.
        await applyConfig(rcon, { defaultBehavior: 'KEEP', rules: UNFILTERED_DAMAGE_RULE });
        await resetPlayer(rcon, bot, { keepInventory });

        // The pickaxe is not incidental: it proves the damage rule actually ran. Without it a
        // rule that failed to load would make this test pass for entirely the wrong reason.
        await give(rcon, bot, [
          { name: CANARY_ITEM, count: 1 },
          { name: 'iron_pickaxe', count: 1 },
        ]);

        await killAndRespawn(rcon, bot);
        await settledInventory(bot);

        const pickaxe = stackIn(bot, 'iron_pickaxe');
        assert.ok(pickaxe, 'setup is wrong: the pickaxe did not survive the death');
        assert.ok(
          pickaxe.durabilityUsed > 0,
          'the damage rule did not run, so this test would pass vacuously',
        );

        // Checked before the comparison so a failure says which thing went wrong. With a
        // max durability of zero the "would this break the item" branch is always true, so a
        // regression here destroys the item outright rather than merely tagging it.
        const survivor = stackIn(bot, CANARY_ITEM);
        assert.ok(survivor, `the ${CANARY_ITEM} was destroyed by the death, not just left alone`);
        assert.equal(survivor.count, 1, `the ${CANARY_ITEM} stack changed size across the death`);

        // Hand it a pristine one. Minecraft merges a /give into a compatible existing stack.
        await give(rcon, bot, [{ name: CANARY_ITEM, count: 1 }]);
        await settledInventory(bot);

        const slots = slotsHolding(bot, CANARY_ITEM);
        const stack = stackIn(bot, CANARY_ITEM);

        assert.equal(
          slots, 1,
          `the ${CANARY_ITEM} that went through the death would not stack with a pristine one, `
          + `so something was written onto it (it is sitting in ${slots} slots)`,
        );
        assert.equal(stack.count, 2, `both ${CANARY_ITEM} should have merged into the one stack`);
        assert.equal(
          stack.durabilityUsed ?? 0, 0,
          `${CANARY_ITEM} has no durability and must not have gained any`,
        );
      });
  }
});
