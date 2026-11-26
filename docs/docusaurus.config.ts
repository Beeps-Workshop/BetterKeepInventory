import fs from 'fs';
import path from 'path';
import type {Config} from '@docusaurus/types';
import type * as Preset from '@docusaurus/preset-classic';

// This runs in Node.js - Don't use client-side code here (browser APIs, JSX...)

// Docs for BetterKeepInventory, served at workshop.beepsterr.com/bki/. The workshop site
// (Beeps-Workshop/docs) builds this folder as its own Docusaurus instance with WORKSHOP_SHARED set to
// its shared/ folder, which adds the workshop's navbar, footer and styles. Without it (npm start
// here) the docs get a plain navbar and footer.
const shared = process.env.WORKSHOP_SHARED;

let withWorkshop: (config: Config, opts: {projectId?: string}) => Config = (config) => ({
  ...config,
  url: 'https://workshop.beepsterr.com',
  baseUrl: process.env.DOCS_BASE_URL ?? '/',
  themeConfig: {
    ...config.themeConfig,
    navbar: {
      title: 'BetterKeepInventory',
      items: [{type: 'docsVersionDropdown', position: 'right'}],
    },
    footer: {style: 'dark', copyright: `Copyright © ${new Date().getFullYear()} Beep's Workshop.`},
  } satisfies Preset.ThemeConfig,
});
if (shared) {
  ({withWorkshop} = require(shared));
}

// The last released version is served by default; the editable current docs are the unreleased
// "Snapshots". Cut a new version with:  npm run docusaurus docs:version <version>
const lastVersion = '2.3.6';

// Only the default (last released) version is indexed. Every other version is a near-identical copy
// of the same page at a different URL, so leaving them crawlable splits ranking signals and risks an
// old version outranking the current one.
const frozen: string[] = JSON.parse(fs.readFileSync(path.join(__dirname, 'versions.json'), 'utf8'));

export default withWorkshop(
  {
    // Page titles end in the workshop's name, like the rest of the site.
    title: "Beep's Workshop",
    favicon: undefined,

    future: {v4: true},

    // Required by the type; withWorkshop sets both (baseUrl from DOCS_BASE_URL).
    url: 'https://workshop.beepsterr.com',
    baseUrl: '/',

    onBrokenLinks: 'throw',

    i18n: {defaultLocale: 'en', locales: ['en']},

    presets: [
      [
        'classic',
        {
          docs: {
            routeBasePath: '/',
            sidebarPath: './sidebars.ts',
            // Points at the real file, whether it's the current version (docs/...) or a frozen one
            // (versioned_docs/version-x/...).
            editUrl: ({versionDocsDirPath, docPath}) =>
              `https://github.com/Beeps-Workshop/BetterKeepInventory/edit/master/docs/${versionDocsDirPath}/${docPath}`,
            lastVersion,
            versions: {
              current: {label: 'Snapshots', banner: 'unreleased', noIndex: true},
              ...Object.fromEntries(frozen.filter((v) => v !== lastVersion).map((v) => [v, {noIndex: true}])),
            },
          },
          blog: false,
        } satisfies Preset.Options,
      ],
    ],
  },
  {projectId: 'bki'},
);
