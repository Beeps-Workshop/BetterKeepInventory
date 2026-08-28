#!/usr/bin/env node
/**
 * Suite runner.
 *
 * Boots one Paper server, runs every test file against it, then shuts it down. Paper
 * takes about half a minute to come up, so booting per test file is not viable -- tests
 * reset the world over RCON instead.
 */
import { spawn } from 'node:child_process';
import fs from 'node:fs';
import path from 'node:path';

import { writeConfig } from './src/config.js';
import { e2eDir } from './src/paths.js';
import { prepareServerDir, startServer, stopServer } from './src/server.js';

const args = new Set(process.argv.slice(2));
const serverOnly = args.has('--server-only');
const keepServer = args.has('--keep-server');

if (!fs.existsSync(path.join(e2eDir, 'node_modules'))) {
  console.error('Dependencies are not installed. Run `npm install` in e2e/ first.');
  process.exit(1);
}

const log = (message) => console.log(`[e2e] ${message}`);

let server;

async function shutdown(code) {
  if (server && !keepServer) {
    log('stopping server...');
    await stopServer(server);
  }
  process.exit(code);
}

process.on('SIGINT', () => { shutdown(130); });
process.on('SIGTERM', () => { shutdown(143); });

try {
  log('preparing server directory...');
  const { pluginJar, paperJar } = prepareServerDir();
  log(`plugin: ${path.basename(pluginJar)}`);
  log(`paper:  ${path.basename(paperJar)}`);

  // Boot with a known-neutral config so the first test does not inherit whatever the
  // previous run happened to leave in the plugin's data folder.
  writeConfig({ defaultBehavior: 'INHERIT' });

  log('booting paper (this takes ~30s the first time a world is generated)...');
  const started = Date.now();
  server = await startServer({ log });
  log(`ready in ${((Date.now() - started) / 1000).toFixed(1)}s`);

  if (serverOnly) {
    log('server-only mode: press ctrl-c to stop.');
    await new Promise(() => {});
  }

  // Enumerated rather than globbed: `node --test tests/` resolves the directory as a
  // module, and glob support in --test has moved around between Node versions.
  const testDir = path.join(e2eDir, 'tests');
  const testFiles = fs.readdirSync(testDir)
    .filter((file) => file.endsWith('.test.js'))
    .sort()
    .map((file) => path.join('tests', file));

  if (testFiles.length === 0) throw new Error('No test files found in e2e/tests.');

  const testProcess = spawn(
    process.execPath,
    // Serial: every test drives the same bot on the same server.
    ['--test', '--test-concurrency=1', `--test-reporter=${process.env.BKI_E2E_REPORTER ?? 'spec'}`,
      ...testFiles],
    { cwd: e2eDir, stdio: 'inherit' },
  );

  const code = await new Promise((resolve) => testProcess.on('exit', resolve));
  await shutdown(code ?? 1);
} catch (error) {
  console.error(`[e2e] ${error.message}`);
  await shutdown(1);
}
