import { Rcon } from 'rcon-client';

import { connection } from './paths.js';

/**
 * All setup, teardown and world-state inspection goes through here.
 *
 * RCON runs commands as the console, which is permission level 4, so `/bki reload` and
 * every vanilla command the suite needs are available without opping the bot.
 */
export async function connectRcon() {
  return Rcon.connect({
    host: connection.host,
    port: connection.rconPort,
    password: connection.rconPassword,
    timeout: 15_000,
  });
}

/** Strip the section-sign colour codes the server sprinkles through command feedback. */
function plain(text) {
  return text.replace(/§[0-9a-fk-or]/gi, '');
}

/** Set BKI_E2E_RCON_TRACE=1 to see every command and its reply. */
const TRACE = process.env.BKI_E2E_RCON_TRACE === '1';

export async function run(rcon, command) {
  const response = plain((await rcon.send(command)) ?? '');
  if (TRACE) console.error(`[rcon] ${command}\n       -> ${JSON.stringify(response)}`);
  return response;
}

/**
 * A round trip that only returns once the server has ticked at least once.
 *
 * RCON commands are queued onto the main thread, so a reply proves the server processed
 * everything sent before it. Used as a barrier instead of sleeping.
 */
export async function tick(rcon, count = 1) {
  for (let i = 0; i < count; i += 1) {
    // eslint-disable-next-line no-await-in-loop
    await run(rcon, 'time query gametime');
  }
}

const MARKER = 'has the following entity data:';

/**
 * Pull the payloads out of a batched `data get` reply.
 *
 * `execute as @e[...] run data get ...` produces one message per matched entity, and the
 * server's RCON transport concatenates those messages with no separator at all -- the
 * reply reads `...count: 1}Cobblestone has the following entity data: {...}`. Splitting
 * on newlines silently returns only the first entity, which looks exactly like a world
 * that only contains one item. So instead: find each marker, then read one balanced,
 * quote-aware value from the position after it.
 */
export function extractEntityData(raw) {
  const values = [];
  let index = raw.indexOf(MARKER);

  while (index !== -1) {
    let start = index + MARKER.length;
    while (raw[start] === ' ') start += 1;

    const opener = raw[start];
    let end;
    if (opener === '{' || opener === '[') {
      end = matchBracket(raw, start);
    } else {
      // A scalar such as `12`, `20.0f` or `"minecraft:dirt"`. It butts straight up
      // against the next entity's display name, so take only the leading token.
      const token = /^(-?\d+(?:\.\d+)?[bslfdBSLFD]?|"(?:[^"\\]|\\.)*"|true|false)/.exec(raw.slice(start));
      end = token ? start + token[0].length : start;
    }

    values.push(raw.slice(start, end));
    index = raw.indexOf(MARKER, end);
  }

  return values;
}

/** Index just past the bracket that closes the one at `start`, ignoring quoted text. */
function matchBracket(text, start) {
  const open = text[start];
  const close = open === '{' ? '}' : ']';
  let depth = 0;
  let quoted = false;

  for (let i = start; i < text.length; i += 1) {
    const char = text[i];
    if (quoted) {
      if (char === '\\') i += 1;
      else if (char === '"') quoted = false;
      continue;
    }
    if (char === '"') quoted = true;
    else if (char === open) depth += 1;
    else if (char === close) {
      depth -= 1;
      if (depth === 0) return i + 1;
    }
  }

  return text.length;
}

/**
 * Read one top-level key out of an SNBT compound.
 *
 * Top-level only on purpose: an item's `components` can contain its own nested `id`, and
 * a plain regex over the whole blob happily returns that instead.
 */
export function nbtField(compound, key) {
  const body = compound.trim().replace(/^\{/, '').replace(/\}$/, '');
  let depth = 0;
  let quoted = false;
  let fieldStart = 0;

  const parts = [];
  for (let i = 0; i < body.length; i += 1) {
    const char = body[i];
    if (quoted) {
      if (char === '\\') i += 1;
      else if (char === '"') quoted = false;
      continue;
    }
    if (char === '"') quoted = true;
    else if (char === '{' || char === '[') depth += 1;
    else if (char === '}' || char === ']') depth -= 1;
    else if (char === ',' && depth === 0) {
      parts.push(body.slice(fieldStart, i));
      fieldStart = i + 1;
    }
  }
  parts.push(body.slice(fieldStart));

  for (const part of parts) {
    const colon = part.indexOf(':');
    if (colon === -1) continue;
    if (part.slice(0, colon).trim().replace(/^"|"$/g, '') === key) {
      return part.slice(colon + 1).trim();
    }
  }

  return null;
}

/**
 * Item entities that a player could actually pick up.
 *
 * Excludes entities with `PickupDelay: 32767s` -- the "never pick up" marker. A
 * successful `/give` spawns exactly such an entity for a single tick purely to play the
 * pickup animation, and counting it makes every setup look one item heavier than it is.
 */
const REAL_DROPS = 'type=item,nbt=!{PickupDelay:32767s}';

/** Every dropped item in the world, as a Map of material name to total count. */
export async function groundItems(rcon) {
  const raw = await run(rcon, `execute as @e[${REAL_DROPS}] run data get entity @s Item`);
  const counts = new Map();

  for (const blob of extractEntityData(raw)) {
    const id = nbtField(blob, 'id');
    if (!id) continue;
    // `count` is omitted when it is 1, so an absent field is not a parse failure.
    const count = nbtField(blob, 'count');

    const name = id.replace(/^"|"$/g, '').replace(/^minecraft:/, '');
    counts.set(name, (counts.get(name) ?? 0) + (count ? Number(count.replace(/[a-z]$/i, '')) : 1));
  }

  return counts;
}

/** Total experience points held by loose orbs in the world. */
export async function groundExperience(rcon) {
  const raw = await run(rcon, 'execute as @e[type=experience_orb] run data get entity @s Value');
  let total = 0;

  for (const value of extractEntityData(raw)) {
    // Values come back as shorts, e.g. `27s`.
    const number = /^(-?\d+)/.exec(value);
    if (number) total += Number(number[1]);
  }

  return total;
}

/** How many separate item entities are lying around, for failure diagnostics. */
export async function groundItemEntityCount(rcon) {
  const raw = await run(rcon, `execute as @e[${REAL_DROPS}] run data get entity @s Item`);
  return extractEntityData(raw).length;
}
