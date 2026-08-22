import {
  createBot, inventoryCounts, mergeCounts, settledExperience, settledInventory, totalOf,
  waitForEvent, waitUntil,
} from './bot.js';
import { connectRcon, groundExperience, groundItems, run, tick } from './rcon.js';

/**
 * Where the bot dies.
 *
 * Deliberately a long way from world spawn. The bot respawns at spawn, so its own drops
 * are left behind out of pickup range -- without that, a respawning bot hoovers up the
 * very items the test is trying to count and the conservation check becomes a race.
 */
export const ARENA = { x: 1000.5, y: 101, z: 1000.5 };

/**
 * An item reserved across the whole suite as a control: stackable, no durability, and a member
 * of no material group, so no rule can match it by group and no test should ever name it in a
 * filter.
 *
 * Its job is to be the thing that was supposed to be left alone. See "Assert absence, not just
 * presence" in the README.
 */
export const CANARY_ITEM = 'paper';
const PLATFORM_Y = 100;

/**
 * One-time world setup. Idempotent, so every test file can call it on the shared server.
 *
 * The gamerules here exist to remove sources of world state the tests do not control:
 * no mobs to kill the bot, no weather, no fire spread, no random ticks.
 */
export async function prepareWorld(rcon) {
  const gamerules = {
    doMobSpawning: false,
    doDaylightCycle: false,
    doWeatherCycle: false,
    doFireTick: false,
    mobGriefing: false,
    randomTickSpeed: 0,
    disableRaids: true,
    showDeathMessages: false,
    doImmediateRespawn: false,
    // Respawn exactly on the world spawn block, so "far from the arena" is not a guess.
    spawnRadius: 0,
  };

  for (const [rule, value] of Object.entries(gamerules)) {
    // eslint-disable-next-line no-await-in-loop
    await run(rcon, `gamerule ${rule} ${value}`);
  }

  await run(rcon, 'difficulty easy');
  await run(rcon, 'time set day');
  await run(rcon, 'weather clear');

  // Keep the arena ticking while the bot is away at spawn, otherwise its chunk unloads
  // and `@e[type=item]` stops seeing the drops we are trying to count.
  await run(rcon, 'forceload add 984 984 1016 1016');
  await run(rcon, `fill 990 ${PLATFORM_Y} 990 1010 ${PLATFORM_Y} 1010 minecraft:bedrock`);
}

/** Connect the RCON channel and a bot, with the world already prepared. */
export async function openSession() {
  const rcon = await connectRcon();
  await prepareWorld(rcon);
  const bot = await createBot();
  await run(rcon, `gamemode survival ${bot.username}`);

  return {
    rcon,
    bot,
    async close() {
      try { bot.quit(); } catch { /* already gone */ }
      try { await rcon.end(); } catch { /* already gone */ }
    },
  };
}

/**
 * Put the world and the bot back to a known state.
 *
 * Everything a previous test could have left behind is cleared here rather than in a
 * teardown, so a test that fails half way through cannot poison the next one.
 */
export async function resetPlayer(rcon, bot, { keepInventory = false } = {}) {
  const name = bot.username;

  await run(rcon, `gamerule keepInventory ${keepInventory}`);
  await run(rcon, `gamemode survival ${name}`);
  await run(rcon, `effect clear ${name}`);
  await run(rcon, `clear ${name}`);
  await run(rcon, `xp set ${name} 0 points`);
  await run(rcon, `xp set ${name} 0 levels`);
  await run(rcon, 'kill @e[type=item]');
  await run(rcon, 'kill @e[type=experience_orb]');
  await run(rcon, `tp ${name} ${ARENA.x} ${ARENA.y} ${ARENA.z}`);

  await waitUntil(bot, () => {
    const pos = bot.entity?.position;
    return pos && Math.abs(pos.x - ARENA.x) < 8 && Math.abs(pos.z - ARENA.z) < 8;
  }, { label: 'the bot to arrive in the arena' });

  await waitUntil(bot, () => totalOf(inventoryCounts(bot)) === 0, { label: 'inventory to clear' });
}

/**
 * Give the bot items and wait until the client agrees it has them.
 *
 * @param items array of `{ name, count }`, using plain material names ("diamond").
 */
export async function give(rcon, bot, items) {
  const before = totalOf(inventoryCounts(bot));

  for (const { name, count } of items) {
    // eslint-disable-next-line no-await-in-loop
    const response = await run(rcon, `give ${bot.username} minecraft:${name} ${count}`);
    if (/^No player was found|Unknown item/i.test(response.trim())) {
      throw new Error(`give failed for ${count}x ${name}: ${response.trim()}`);
    }
  }

  const expected = new Map();
  for (const { name, count } of items) expected.set(name, (expected.get(name) ?? 0) + count);
  const expectedTotal = totalOf(expected);

  // Wait on the increase rather than the total, so this works on an inventory that already
  // holds something -- handing the bot a second, pristine copy of an item it is already
  // carrying is the whole technique behind the side-effect tests.
  await waitUntil(bot, () => totalOf(inventoryCounts(bot)) >= before + expectedTotal, {
    label: `bot to receive ${expectedTotal} more items`,
  });

  return expected;
}

/**
 * Put the bot on an exact experience level with no partial progress.
 *
 * The points are zeroed first because `xp set ... levels` only moves the level and leaves
 * the progress bar alone. Landing exactly on a level boundary also keeps these tests
 * clear of the partial-progress behaviour that is due to change.
 */
export async function setLevels(rcon, bot, levels) {
  await run(rcon, `xp set ${bot.username} 0 points`);
  await run(rcon, `xp set ${bot.username} ${levels} levels`);
  await waitUntil(bot, () => bot.experience.level === levels && bot.experience.progress === 0, {
    label: `the bot to reach level ${levels}`,
  });
}

/**
 * Kill the bot and return once it is alive again and its inventory has stopped changing.
 *
 * @param how `{ kind: 'kill' }` for `/kill`, or `{ kind: 'damage', type, amount }` to use
 *            `/damage` so the death has a specific cause.
 */
export async function killAndRespawn(rcon, bot, how = { kind: 'kill' }) {
  const death = waitForEvent(bot, 'death', 20_000, 'the bot to die');
  const respawn = waitForEvent(bot, 'spawn', 20_000, 'the bot to respawn');

  if (how.kind === 'damage') {
    const response = await run(
      rcon, `damage ${bot.username} ${how.amount ?? 10_000} ${how.type ?? 'minecraft:generic'}`,
    );
    if (/^Unable to|Unknown|Incorrect/i.test(response.trim())) {
      throw new Error(`damage command failed: ${response.trim()}`);
    }
  } else {
    await run(rcon, `kill ${bot.username}`);
  }

  await death;

  // mineflayer respawns on its own, but only once it has processed the death packet.
  // Nudge it if the respawn has not landed, rather than assuming either behaviour.
  const nudge = setTimeout(() => { try { bot.respawn(); } catch { /* already respawned */ } }, 1_000);
  try {
    await respawn;
  } finally {
    clearTimeout(nudge);
  }

  // A round trip through RCON proves the server has ticked past the death, including the
  // one-tick-later work the plugin's effects schedule.
  await tick(rcon, 2);
  await settledInventory(bot);
  await settledExperience(bot);
}

/**
 * Everything the world knows about where the bot's stuff ended up.
 *
 * Inventory comes from the bot, ground items from entity NBT over RCON. Both are
 * observable world state; nothing here reaches into the plugin.
 */
export async function snapshot(rcon, bot) {
  const inventory = inventoryCounts(bot);
  const ground = await groundItems(rcon);
  const combined = mergeCounts(inventory, ground);

  return {
    inventory,
    ground,
    combined,
    total: totalOf(combined),
    experience: {
      level: bot.experience.level,
      // Fraction of the way through the current level. `bot.experience.points` is the
      // server's totalExperience field, which `xp set ... levels` does not recompute, so
      // it is not a trustworthy measure here.
      progress: bot.experience.progress,
      orbs: await groundExperience(rcon),
    },
  };
}
