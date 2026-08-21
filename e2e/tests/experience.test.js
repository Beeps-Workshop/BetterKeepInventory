import assert from 'node:assert/strict';
import { after, before, describe, it } from 'node:test';

import { applyConfig } from '../src/config.js';
import { killAndRespawn, openSession, resetPlayer, setLevels, snapshot } from '../src/harness.js';

/**
 * Vanilla's level-to-points curve. Levels are not linear in experience, so "levels kept
 * plus orbs dropped" only balances once both sides are converted to points.
 */
function pointsForLevel(level) {
  if (level <= 16) return level * level + 6 * level;
  if (level <= 31) return Math.trunc(2.5 * level * level - 40.5 * level + 360);
  return Math.trunc(4.5 * level * level - 162.5 * level + 2220);
}

const START_LEVEL = 30;
const START_POINTS = pointsForLevel(START_LEVEL);

/** A rule with a single `exp` effect and no conditions, so it always fires. */
function expRule(effect) {
  return {
    exp_rule: {
      name: 'Experience',
      enabled: true,
      effects: { exp: effect },
    },
  };
}

describe('experience conservation across a death', { timeout: 600_000 }, () => {
  let session;

  before(async () => { session = await openSession(); }, { timeout: 120_000 });
  after(async () => { await session?.close(); });

  /** Set up, kill, and report what the world looks like afterwards. */
  async function dieWith(scenario) {
    const { rcon, bot } = session;
    await applyConfig(rcon, scenario);
    await resetPlayer(rcon, bot, { keepInventory: false });
    await setLevels(rcon, bot, START_LEVEL);

    const before = await snapshot(rcon, bot);
    assert.equal(before.experience.level, START_LEVEL, 'setup is wrong: bot is not at the start level');
    assert.equal(before.experience.orbs, 0, 'setup is wrong: orbs were left lying around');

    await killAndRespawn(rcon, bot);
    return snapshot(rcon, bot);
  }

  it('keeps every level when the behaviour is KEEP and no rule touches experience',
    { timeout: 120_000 }, async () => {
      const result = await dieWith({ defaultBehavior: 'KEEP' });

      assert.equal(result.experience.level, START_LEVEL, 'levels should have survived the death');
      assert.equal(result.experience.progress, 0, 'progress should not have moved');
      assert.equal(result.experience.orbs, 0, 'nothing should have been dropped');
    });

  it('drops exactly the experience it takes when the exp effect drops everything',
    { timeout: 120_000 }, async () => {
      const result = await dieWith({
        defaultBehavior: 'KEEP',
        rules: expRule({ mode: 'ALL', how: 'DROP' }),
      });

      const kept = pointsForLevel(result.experience.level);
      const detail = `\n  kept: level ${result.experience.level} (${kept} points)`
        + `\n  orbs: ${result.experience.orbs} points`;

      assert.equal(result.experience.level, 0, `everything should have been taken.${detail}`);
      // The invariant: what the player kept plus what hit the floor is what they had.
      assert.equal(
        kept + result.experience.orbs, START_POINTS,
        `experience was created or destroyed across the death.${detail}`,
      );
    });

  it('drops exactly the experience it takes on a partial exp effect',
    { timeout: 120_000 }, async () => {
      // A fixed 50% so the split is deterministic, but the assertion below does not
      // depend on the split being any particular size.
      const result = await dieWith({
        defaultBehavior: 'KEEP',
        rules: expRule({ mode: 'PERCENTAGE', how: 'DROP', min: 50, max: 50 }),
      });

      const kept = pointsForLevel(result.experience.level);
      const detail = `\n  kept: level ${result.experience.level} (${kept} points)`
        + `\n  orbs: ${result.experience.orbs} points`;

      assert.ok(
        result.experience.level > 0 && result.experience.level < START_LEVEL,
        `expected a partial loss, got level ${result.experience.level}`,
      );
      assert.equal(
        kept + result.experience.orbs, START_POINTS,
        `experience was created or destroyed across the death.${detail}`,
      );
    });

  it('destroys experience, rather than dropping it, when the exp effect deletes',
    { timeout: 120_000 }, async () => {
      const result = await dieWith({
        defaultBehavior: 'KEEP',
        rules: expRule({ mode: 'ALL', how: 'DELETE' }),
      });

      // Deliberately not conservative: DELETE means the experience is gone, and this
      // test exists to pin that difference from DROP.
      assert.equal(result.experience.level, 0, 'levels should have been deleted');
      assert.equal(result.experience.orbs, 0, 'deleted experience must not reach the floor');
    });

  it('leaves vanilla in charge of the experience drop when the behaviour is DROP',
    { timeout: 120_000 }, async () => {
      const result = await dieWith({ defaultBehavior: 'DROP' });

      // Vanilla caps a death drop at 100 points and destroys the remainder, so this is
      // the one case that is expected NOT to conserve. Asserted loosely on purpose: the
      // point is that the player loses their levels and something lands on the floor.
      assert.equal(result.experience.level, 0, 'a vanilla death should cost every level');
      assert.ok(result.experience.orbs > 0, 'a vanilla death should drop some experience');
      assert.ok(
        result.experience.orbs <= 100,
        `vanilla caps the drop at 100 points, saw ${result.experience.orbs}`,
      );
      assert.ok(
        result.experience.orbs < START_POINTS,
        'this case is expected to be lossy; if it now conserves, the test is out of date',
      );
    });
});
