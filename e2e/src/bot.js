import mineflayer from 'mineflayer';

import { MINECRAFT_VERSION, connection } from './paths.js';

export const BOT_USERNAME = 'BkiBot';

/** Connect a bot and resolve once it has spawned into the world. */
export async function createBot({ username = BOT_USERNAME, timeoutMs = 60_000 } = {}) {
  const bot = mineflayer.createBot({
    host: connection.host,
    port: connection.gamePort,
    username,
    // Pinned rather than negotiated: minecraft-data lags new releases, and a mismatch
    // surfaces as confusing packet errors deep inside the test instead of at connect.
    version: MINECRAFT_VERSION,
    auth: 'offline',
    checkTimeoutInterval: 60_000,
    hideErrors: true,
  });

  await new Promise((resolve, reject) => {
    const timer = setTimeout(() => reject(new Error(`Bot did not spawn within ${timeoutMs}ms`)), timeoutMs);
    const onSpawn = () => { clearTimeout(timer); bot.off('error', onError); resolve(); };
    const onError = (err) => { clearTimeout(timer); bot.off('spawn', onSpawn); reject(err); };
    bot.once('spawn', onSpawn);
    bot.once('error', onError);
  });

  // Kicks and socket errors after connect would otherwise crash the test process. They
  // are still worth seeing -- a swallowed protocol error looks exactly like a hang.
  bot.on('error', (err) => console.error(`[bot] error: ${err?.message ?? err}`));
  bot.on('kicked', (reason) => console.error(`[bot] kicked: ${JSON.stringify(reason)}`));
  bot.on('end', (reason) => console.error(`[bot] disconnected: ${reason}`));

  keepClientLoaded(bot);

  return bot;
}

/**
 * Tell the server the client has finished loading the world.
 *
 * Since 1.21.2 the server holds a player invulnerable until it receives
 * ServerboundPlayerLoadedPacket -- `ServerPlayer#isInvulnerableTo` short-circuits on
 * `!hasClientLoaded()`, and no damage type bypasses that, not even the one `/kill` uses.
 * mineflayer never sends the packet, so without this a bot silently cannot die: `/kill`
 * still answers "Killed BkiBot" while the player keeps all twenty hearts.
 *
 * The flag is cleared again every time the player respawns, so this re-sends on spawn
 * rather than only once at login.
 */
function keepClientLoaded(bot) {
  const send = () => {
    try {
      bot._client.write('player_loaded', {});
    } catch {
      // Older protocol versions have no such packet, and do not need one.
    }
  };

  send();
  bot.on('spawn', send);
  bot.on('respawn', send);
}

export function waitForEvent(bot, event, timeoutMs, label = event) {
  return new Promise((resolve, reject) => {
    const timer = setTimeout(() => {
      bot.off(event, onEvent);
      reject(new Error(`Timed out after ${timeoutMs}ms waiting for ${label}`));
    }, timeoutMs);
    const onEvent = (...args) => { clearTimeout(timer); resolve(args); };
    bot.once(event, onEvent);
  });
}

/** Resolve after `count` server ticks have been observed client-side. */
export function waitTicks(bot, count = 1) {
  return new Promise((resolve) => {
    let seen = 0;
    const onTick = () => {
      seen += 1;
      if (seen >= count) {
        bot.off('physicsTick', onTick);
        resolve();
      }
    };
    bot.on('physicsTick', onTick);
  });
}

/**
 * Poll a predicate on every physics tick until it holds.
 *
 * The suite never sleeps for a fixed duration; anything that needs to wait waits on a
 * condition or an event with a deadline, so a slow machine costs time rather than a
 * false failure.
 */
export async function waitUntil(bot, predicate, { timeoutMs = 15_000, label = 'condition' } = {}) {
  const deadline = Date.now() + timeoutMs;
  for (;;) {
    // eslint-disable-next-line no-await-in-loop
    if (await predicate()) return;
    if (Date.now() > deadline) throw new Error(`Timed out after ${timeoutMs}ms waiting for ${label}`);
    // eslint-disable-next-line no-await-in-loop
    await waitTicks(bot, 1);
  }
}

/**
 * Every item the bot is carrying, as a Map of material name to total count.
 *
 * Reads the raw slot array rather than `bot.inventory.items()` so armour, offhand and
 * the crafting grid are included -- an effect that shuffles an item into a slot
 * `items()` hides would otherwise look like the item vanished.
 */
export function inventoryCounts(bot) {
  const counts = new Map();
  for (const slot of bot.inventory.slots) {
    if (!slot) continue;
    counts.set(slot.name, (counts.get(slot.name) ?? 0) + slot.count);
  }
  return counts;
}

/**
 * Wait for a piece of client-side state to stop changing.
 *
 * Death, drops and respawn each produce their own packets, and they do not all arrive on
 * the same tick. Two identical reads a few ticks apart means the server has finished
 * telling us about it. Waiting on a fixed delay instead would either be flaky or slow.
 */
export async function settled(bot, read, { stableTicks = 4, timeoutMs = 15_000, label = 'state' } = {}) {
  const deadline = Date.now() + timeoutMs;
  let previous = JSON.stringify(read());

  for (;;) {
    // eslint-disable-next-line no-await-in-loop
    await waitTicks(bot, stableTicks);
    const current = JSON.stringify(read());
    if (current === previous) return read();
    previous = current;
    if (Date.now() > deadline) {
      throw new Error(`${label} never settled within ${timeoutMs}ms (last: ${current})`);
    }
  }
}

export function settledInventory(bot, options = {}) {
  return settled(bot, () => [...inventoryCounts(bot).entries()].sort(), {
    label: 'Inventory', ...options,
  });
}

export function settledExperience(bot, options = {}) {
  return settled(bot, () => [bot.experience.level, bot.experience.progress], {
    label: 'Experience', ...options,
  });
}

export function totalOf(counts) {
  let total = 0;
  for (const value of counts.values()) total += value;
  return total;
}

/** Merge item tallies from several sources into one Map. */
export function mergeCounts(...maps) {
  const merged = new Map();
  for (const map of maps) {
    for (const [name, count] of map) merged.set(name, (merged.get(name) ?? 0) + count);
  }
  return merged;
}

export function formatCounts(counts) {
  const entries = [...counts.entries()].sort(([a], [b]) => a.localeCompare(b));
  return entries.length ? entries.map(([k, v]) => `${v}x ${k}`).join(', ') : '(nothing)';
}
