'use strict';

const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
const { loadCorpus } = require('../core/content-loader');
const { AudioManager } = require('../core/audio-manager');

async function main() {
  const contentDir = path.join(__dirname, '..', '..', 'app', 'src', 'main', 'assets', 'content', 'tanzil-uthmani-1.1');
  const verse = loadCorpus(contentDir).verses[0];
  const cacheDir = fs.mkdtempSync(path.join(os.tmpdir(), 'hifz-desktop-audio-check-'));
  try {
    const manager = new AudioManager(cacheDir);
    const result = await manager.download(verse);
    const item = fs.statSync(result.path);
    if (item.size !== verse.audio.bytes) throw new Error(`Taille inattendue : ${item.size}`);
    process.stdout.write(`AUDIO_OK ${verse.key} ${item.size} ${verse.audio.sha256}\n`);
  } finally {
    const resolved = path.resolve(cacheDir);
    const expectedPrefix = path.resolve(os.tmpdir(), 'hifz-desktop-audio-check-');
    if (!resolved.startsWith(expectedPrefix)) throw new Error(`Dossier temporaire inattendu : ${resolved}`);
    fs.rmSync(resolved, { recursive: true, force: true });
  }
}

main().catch(error => {
  process.stderr.write(`${error.stack || error.message}\n`);
  process.exitCode = 1;
});
