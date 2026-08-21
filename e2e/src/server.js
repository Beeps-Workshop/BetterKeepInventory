import { spawn } from 'node:child_process';
import fs from 'node:fs';
import path from 'node:path';

import {
  MINECRAFT_VERSION, e2eDir, findPluginJar, paperJarPath, pluginDataDir, pluginsDir,
  propertiesTemplate, repoRoot, serverDir,
} from './paths.js';

const BOOT_TIMEOUT_MS = 180_000;

/**
 * Lay down a fresh server directory.
 *
 * Everything here is disposable: the world, the plugin's data folder and the copy of
 * server.properties are all rebuilt from scratch, so a run can never inherit state from
 * the previous one. `e2e/server.properties` is the source of truth; the copy inside
 * .server/ is what the server rewrites on shutdown and is thrown away next run.
 */
export function prepareServerDir({ freshWorld = true } = {}) {
  const pluginJar = findPluginJar();
  if (!pluginJar) {
    throw new Error(
      'No plugin jar found in plugin/target.\n'
      + 'Build it first:  mvn -B package -DskipTests',
    );
  }
  if (!fs.existsSync(paperJarPath)) {
    throw new Error(
      `No Paper jar at ${paperJarPath}\n`
      + `Download it:  ./scripts/get_paper_jar.sh paper ${MINECRAFT_VERSION}`,
    );
  }

  fs.mkdirSync(pluginDataDir, { recursive: true });

  if (freshWorld) {
    for (const world of ['world', 'world_nether', 'world_the_end']) {
      fs.rmSync(path.join(serverDir, world), { recursive: true, force: true });
    }
  }

  fs.copyFileSync(propertiesTemplate, path.join(serverDir, 'server.properties'));
  fs.copyFileSync(pluginJar, path.join(pluginsDir, 'BetterKeepInventory.jar'));
  fs.copyFileSync(paperJarPath, path.join(serverDir, 'server.jar'));

  // Running Paper at all requires accepting https://aka.ms/MinecraftEULA. The suite writes
  // this because the whole point is an unattended boot; see the note in the README.
  fs.writeFileSync(path.join(serverDir, 'eula.txt'), 'eula=true\n');

  // bStats phones home on a timer. Off, so a test run makes no outbound metrics calls.
  const bstatsDir = path.join(pluginsDir, 'bStats');
  fs.mkdirSync(bstatsDir, { recursive: true });
  fs.writeFileSync(path.join(bstatsDir, 'config.yml'), 'enabled: false\nlogFailedRequests: false\n');

  return { pluginJar, paperJar: paperJarPath };
}

/**
 * Boot Paper and resolve once it reports "Done".
 *
 * Paper is started directly with `java` rather than through scripts/run_dev_server.sh:
 * that script goes via docker-compose, which is a much heavier dependency than this
 * suite needs and does not survive being pointed at an arbitrary directory.
 */
export async function startServer({ log = () => {} } = {}) {
  const logFile = path.join(e2eDir, '.server', 'console.log');
  const logStream = fs.createWriteStream(logFile, { flags: 'w' });

  const proc = spawn(
    'java',
    ['-Xms1G', '-Xmx2G', '-XX:+UseG1GC', '-Dcom.mojang.eula.agree=true',
      '-jar', 'server.jar', 'nogui'],
    { cwd: serverDir, stdio: ['pipe', 'pipe', 'pipe'] },
  );

  const server = {
    proc,
    logFile,
    stop: () => stopServer(server),
    exited: false,
    exitCode: null,
  };

  proc.on('exit', (code) => { server.exited = true; server.exitCode = code; });

  await new Promise((resolve, reject) => {
    const timer = setTimeout(
      () => reject(new Error(`Server did not finish booting within ${BOOT_TIMEOUT_MS}ms. See ${logFile}`)),
      BOOT_TIMEOUT_MS,
    );

    let buffered = '';
    const onChunk = (chunk) => {
      const text = chunk.toString();
      logStream.write(text);
      buffered += text;

      // Paper prints `Done (12.345s)! For help, type "help"` once the server is accepting
      // connections. Match on that rather than on a fixed delay.
      if (/Done \([0-9.]+s\)! For help/.test(buffered)) {
        clearTimeout(timer);
        proc.stdout.off('data', onChunk);
        proc.stderr.off('data', onChunk);
        proc.stdout.on('data', (c) => logStream.write(c));
        proc.stderr.on('data', (c) => logStream.write(c));
        resolve();
      }
      // Keep the buffer bounded; the marker never spans more than a line or two.
      if (buffered.length > 64_000) buffered = buffered.slice(-8_000);
    };

    proc.stdout.on('data', onChunk);
    proc.stderr.on('data', onChunk);
    proc.on('exit', (code) => {
      clearTimeout(timer);
      reject(new Error(`Server exited with code ${code} before finishing boot. See ${logFile}`));
    });
    proc.on('error', (err) => { clearTimeout(timer); reject(err); });
  });

  log(`server up (log: ${logFile})`);
  return server;
}

/** Ask nicely over stdin, then insist. */
export async function stopServer(server, { timeoutMs = 30_000 } = {}) {
  if (!server || server.exited) return;

  const exited = new Promise((resolve) => server.proc.once('exit', resolve));
  try {
    server.proc.stdin.write('stop\n');
  } catch {
    // stdin already gone; fall through to the kill below.
  }

  const timedOut = Symbol('timeout');
  const result = await Promise.race([
    exited,
    new Promise((resolve) => setTimeout(() => resolve(timedOut), timeoutMs)),
  ]);

  if (result === timedOut) {
    server.proc.kill('SIGKILL');
    await exited;
  }
}

export { repoRoot, serverDir, pluginDataDir };
