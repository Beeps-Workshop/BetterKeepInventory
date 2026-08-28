import assert from 'node:assert/strict';
import { after, before, describe, it } from 'node:test';

import { createBot, settled, waitForEvent, waitUntil } from '../src/bot.js';
import { applyConfig } from '../src/config.js';
import { killAndRespawn, openSession, replaceBot, resetPlayer } from '../src/harness.js';
import { run } from '../src/rcon.js';

/**
 * The respawn half of a death.
 *
 * Only one built-in effect does work in this phase, so `hunger` stands in for the whole
 * mechanism: it saves a value during the death and re-applies it a few ticks after the player
 * is back. Unit tests drive that by calling into the respawn phase directly; these are what
 * prove a real respawn actually delivers it.
 */

const START_FOOD = 20;

function hungerRule(hunger) {
  return {
    lose_hunger: {
      name: 'lose hunger',
      enabled: true,
      effects: { hunger },
    },
  };
}

/** Food arrives on its own packet, a few ticks after the respawn. */
function settledFood(bot) {
  return settled(bot, () => bot.food, { label: 'Food' });
}

describe('the respawn phase', { timeout: 600_000 }, () => {
  let session;

  before(async () => { session = await openSession(); }, { timeout: 120_000 });
  after(async () => { await session?.close(); });

  it('re-applies the saved hunger after the player is back', { timeout: 120_000 }, async () => {
    const { rcon, bot } = session;

    await applyConfig(rcon, { defaultBehavior: 'KEEP', rules: hungerRule({ min: 0, amount: 6 }) });
    await resetPlayer(rcon, bot, { keepInventory: false });

    // The effect's whole job is to bring food back down from full.
    assert.equal(bot.food, START_FOOD, 'setup is wrong: the bot did not start fully fed');

    await killAndRespawn(rcon, bot);
    await settledFood(bot);

    assert.equal(
      bot.food, START_FOOD - 6,
      'the value saved during the death was not applied after the respawn',
    );
  });

  it('does not take hunger below the configured minimum', { timeout: 120_000 }, async () => {
    const { rcon, bot } = session;

    // 20 - 15 would be 5, but the floor is 18.
    await applyConfig(rcon, { defaultBehavior: 'KEEP', rules: hungerRule({ min: 18, amount: 15 }) });
    await resetPlayer(rcon, bot, { keepInventory: false });
    assert.equal(bot.food, START_FOOD, 'setup is wrong: the bot did not start fully fed');

    await killAndRespawn(rcon, bot);
    await settledFood(bot);

    assert.equal(bot.food, 18, 'the minimum should have floored the loss');
  });

  /**
   * The assumption the whole pending-death design rests on.
   *
   * A player who is kicked, banned, or who simply quits at the death screen never presses
   * respawn during that session. The design says this only *defers* the respawn phase -- they
   * rejoin still dead and have to press it then -- and so the pending death is held until it
   * is claimed rather than being swept up.
   *
   * If that is wrong, every respawn-phase effect silently never runs for those players, and
   * nothing else in the test suite would notice. The `kick` effect makes it deterministic:
   * it disconnects the player a tick after death, before any respawn can happen.
   */
  it('still runs when the player is kicked and only respawns after rejoining',
    { timeout: 180_000 }, async () => {
      const { rcon, bot } = session;

      await applyConfig(rcon, {
        defaultBehavior: 'KEEP',
        rules: {
          kick_and_starve: {
            name: 'kick and starve',
            enabled: true,
            effects: {
              hunger: { min: 0, amount: 6 },
              kick: { message: 'e2e: kicked on death' },
            },
          },
        },
      });
      await resetPlayer(rcon, bot, { keepInventory: false });
      assert.equal(bot.food, START_FOOD, 'setup is wrong: the bot did not start fully fed');

      // Die, and get disconnected by the kick effect before respawning.
      const disconnected = waitForEvent(bot, 'end', 30_000, 'the bot to be kicked');
      await run(rcon, `kill ${bot.username}`);
      await disconnected;

      // The server needs a moment to release the username before it can be reused.
      await new Promise((resolve) => { setTimeout(resolve, 1_000); });

      const rejoined = await createBot();
      try {
        // Rejoining lands on the death screen: still dead, respawn not yet pressed.
        await waitUntil(rejoined, () => rejoined.food !== undefined && rejoined.health !== undefined, {
          label: 'the rejoined bot to report its state',
        });

        try { rejoined.respawn(); } catch { /* mineflayer may have respawned already */ }

        await waitUntil(rejoined, () => rejoined.health > 0, {
          timeoutMs: 30_000, label: 'the rejoined bot to respawn',
        });
        await settledFood(rejoined);

        assert.equal(
          rejoined.food, START_FOOD - 6,
          'the respawn phase never ran for a player who was kicked before respawning',
        );
      } finally {
        try { rejoined.quit(); } catch { /* already gone */ }
      }

      // Put the shared session's bot back so later tests are not left with a dead connection.
      await replaceBot(session);
    });
});
