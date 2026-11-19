'use strict';

const crypto = require('node:crypto');
const fs = require('node:fs');
const path = require('node:path');
const { pathToFileURL } = require('node:url');
const http = require('node:http');
const https = require('node:https');

const EVERYAYAH_BASE = 'https://everyayah.com/data/Alafasy_128kbps/';
const QURAN_FOUNDATION_BASE = 'https://verses.quran.foundation/Alafasy/mp3/';

function fileNameForKey(key) {
  const match = /^(\d{1,3}):(\d{1,3})$/.exec(key || '');
  if (!match) throw new Error('Clé de verset invalide');
  const surah = Number(match[1]);
  const ayah = Number(match[2]);
  if (surah < 1 || surah > 114 || ayah < 1 || ayah > 286) throw new Error('Clé de verset hors limites');
  return `${String(surah).padStart(3, '0')}${String(ayah).padStart(3, '0')}.mp3`;
}

function sha256(file) {
  return new Promise((resolve, reject) => {
    const hash = crypto.createHash('sha256');
    const stream = fs.createReadStream(file);
    stream.on('data', chunk => hash.update(chunk));
    stream.on('error', reject);
    stream.on('end', () => resolve(hash.digest('hex')));
  });
}

function requestToFile(url, file, resumeAt, onProgress, redirects = 0) {
  return new Promise((resolve, reject) => {
    if (redirects > 5) return reject(new Error('Trop de redirections'));
    const client = url.startsWith('https:') ? https : http;
    const headers = resumeAt > 0 ? { Range: `bytes=${resumeAt}-` } : {};
    const request = client.get(url, { headers }, response => {
      if ([301, 302, 303, 307, 308].includes(response.statusCode) && response.headers.location) {
        response.resume();
        return resolve(requestToFile(new URL(response.headers.location, url).toString(), file, resumeAt, onProgress, redirects + 1));
      }
      if (response.statusCode !== 200 && response.statusCode !== 206) {
        response.resume();
        return reject(new Error(`HTTP ${response.statusCode}`));
      }
      const append = resumeAt > 0 && response.statusCode === 206;
      const initial = append ? resumeAt : 0;
      let received = initial;
      const total = initial + Number(response.headers['content-length'] || 0);
      const output = fs.createWriteStream(file, { flags: append ? 'a' : 'w' });
      response.on('data', chunk => {
        received += chunk.length;
        onProgress?.({ received, total });
      });
      response.on('error', reject);
      output.on('error', reject);
      output.on('close', resolve);
      response.pipe(output);
    });
    request.setTimeout(30000, () => request.destroy(new Error('Délai réseau dépassé')));
    request.on('error', reject);
  });
}

class AudioManager {
  constructor(cacheDir) {
    this.cacheDir = cacheDir;
    fs.mkdirSync(cacheDir, { recursive: true });
  }

  localPath(key) {
    return path.join(this.cacheDir, fileNameForKey(key));
  }

  candidates(key, primaryUrl) {
    const name = fileNameForKey(key);
    const local = this.localPath(key);
    const urls = [];
    if (fs.existsSync(local)) urls.push(pathToFileURL(local).toString());
    if (typeof primaryUrl === 'string' && primaryUrl.startsWith('https://')) urls.push(primaryUrl);
    urls.push(EVERYAYAH_BASE + name, QURAN_FOUNDATION_BASE + name);
    return [...new Set(urls)];
  }

  async download(verse, onProgress) {
    if (!verse?.key || !verse?.audio?.sha256 || !verse?.audio?.bytes) throw new Error('Inventaire audio incomplet');
    const target = this.localPath(verse.key);
    if (fs.existsSync(target)) {
      const current = await sha256(target);
      if (current === verse.audio.sha256.toLowerCase()) return { key: verse.key, path: target, cached: true };
      fs.rmSync(target, { force: true });
    }
    const partial = `${target}.part`;
    const remotes = this.candidates(verse.key, verse.audio.remoteUrl).filter(value => value.startsWith('https://'));
    let lastError;
    for (const remote of remotes) {
      try {
        const existing = fs.existsSync(partial) ? fs.statSync(partial).size : 0;
        const resumeAt = existing < verse.audio.bytes ? existing : 0;
        if (resumeAt === 0 && existing > 0) fs.truncateSync(partial, 0);
        await requestToFile(remote, partial, resumeAt, progress => onProgress?.({ key: verse.key, ...progress }));
        const size = fs.statSync(partial).size;
        const hash = await sha256(partial);
        if (size !== verse.audio.bytes || hash !== verse.audio.sha256.toLowerCase()) {
          fs.rmSync(partial, { force: true });
          throw new Error('Contrôle SHA-256 audio échoué');
        }
        fs.renameSync(partial, target);
        return { key: verse.key, path: target, cached: false };
      } catch (error) {
        lastError = error;
      }
    }
    throw lastError || new Error('Aucune source audio disponible');
  }

  list() {
    return fs.readdirSync(this.cacheDir)
      .filter(name => /^\d{6}\.mp3$/.test(name))
      .map(name => ({
        name,
        key: `${Number(name.slice(0, 3))}:${Number(name.slice(3, 6))}`,
        bytes: fs.statSync(path.join(this.cacheDir, name)).size
      }))
      .sort((a, b) => a.key.localeCompare(b.key, undefined, { numeric: true }));
  }

  remove(key) {
    fs.rmSync(this.localPath(key), { force: true });
    fs.rmSync(`${this.localPath(key)}.part`, { force: true });
    return true;
  }
}

module.exports = { AudioManager, fileNameForKey, EVERYAYAH_BASE, QURAN_FOUNDATION_BASE };
