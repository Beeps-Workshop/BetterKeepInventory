import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

export const e2eDir = path.dirname(path.dirname(fileURLToPath(import.meta.url)));
export const repoRoot = path.dirname(e2eDir);

/** Scratch server the suite boots. Wiped and rebuilt on every run. */
export const serverDir = path.join(e2eDir, '.server');
export const pluginsDir = path.join(serverDir, 'plugins');
export const pluginDataDir = path.join(pluginsDir, 'BetterKeepInventory');
export const propertiesTemplate = path.join(e2eDir, 'server.properties');

/** Minecraft version the suite pins itself to. See README for why this is not "latest". */
export const MINECRAFT_VERSION = '1.21.8';

export const paperJarPath = path.join(
  repoRoot, 'scripts', 'build_server_jar', 'jars', `paper-${MINECRAFT_VERSION}.jar`,
);

export function readProperties(file) {
  const out = {};
  for (const line of fs.readFileSync(file, 'utf8').split('\n')) {
    const trimmed = line.trim();
    if (!trimmed || trimmed.startsWith('#')) continue;
    const eq = trimmed.indexOf('=');
    if (eq === -1) continue;
    out[trimmed.slice(0, eq)] = trimmed.slice(eq + 1);
  }
  return out;
}

const props = readProperties(propertiesTemplate);

export const connection = {
  host: '127.0.0.1',
  gamePort: Number(props['server-port']),
  rconPort: Number(props['rcon.port']),
  rconPassword: props['rcon.password'],
};

/** The single shaded plugin jar Maven produces, ignoring the unshaded `original-` twin. */
export function findPluginJar() {
  const targetDir = path.join(repoRoot, 'plugin', 'target');
  if (!fs.existsSync(targetDir)) return null;
  const match = fs.readdirSync(targetDir)
    .filter((f) => f.startsWith('BetterKeepInventory-plugin-') && f.endsWith('.jar'))
    .sort();
  return match.length ? path.join(targetDir, match[0]) : null;
}
