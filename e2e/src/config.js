import fs from 'node:fs';
import path from 'node:path';

import { pluginDataDir } from './paths.js';
import { run } from './rcon.js';

/**
 * The plugin rewrites config.yml on load to stamp its own version and commit hash, and
 * treats the literal string "default" as "this is a pristine file, replace it". Writing
 * a concrete version keeps our config from being swapped for the bundled default.
 */
const CONFIG_VERSION = '3.0.0';

/**
 * Minimal YAML emitter for the shapes this plugin's config uses: nested maps, string
 * lists, and scalars. Small enough not to be worth a dependency, and keeping the
 * scenarios as plain JS objects makes the tests read as configuration rather than text.
 */
function toYaml(value, indent = 0) {
  const pad = '  '.repeat(indent);

  if (Array.isArray(value)) {
    if (value.length === 0) return `${pad}[]\n`;
    return value.map((entry) => `${pad}- ${scalar(entry)}\n`).join('');
  }

  if (value && typeof value === 'object') {
    const keys = Object.keys(value);
    if (keys.length === 0) return `${pad}{}\n`;
    return keys.map((key) => {
      const child = value[key];
      if (child && typeof child === 'object') {
        return `${pad}${key}:\n${toYaml(child, indent + 1)}`;
      }
      return `${pad}${key}: ${scalar(child)}\n`;
    }).join('');
  }

  return `${pad}${scalar(value)}\n`;
}

function scalar(value) {
  if (typeof value === 'string') return JSON.stringify(value);
  if (value === null || value === undefined) return '~';
  return String(value);
}

/**
 * Build a config.yml body for one scenario.
 *
 * `rules` is a plain object mirroring the plugin's rule tree, so a test can express a
 * scenario inline instead of maintaining a fixture file per case.
 */
export function renderConfig({ defaultBehavior = 'INHERIT', debug = false, rules = {} } = {}) {
  return toYaml({
    version: CONFIG_VERSION,
    hash: 'e2e',
    // No update checks: a test run should make no outbound requests.
    notify_channel: 'NONE',
    debug,
    default_behavior: defaultBehavior,
    rules,
  });
}

export function writeConfig(scenario) {
  fs.mkdirSync(pluginDataDir, { recursive: true });
  fs.writeFileSync(path.join(pluginDataDir, 'config.yml'), renderConfig(scenario));
}

/**
 * Write a scenario's config and make the running server adopt it.
 *
 * Going through `/bki reload` rather than restarting keeps the suite to a single boot,
 * and exercises the reload path itself on every single test as a side effect.
 */
export async function applyConfig(rcon, scenario) {
  writeConfig(scenario);
  const response = await run(rcon, 'bki reload');
  if (!/reloaded successfully/i.test(response)) {
    throw new Error(`/bki reload did not report success:\n${response}`);
  }
  return response;
}
