'use strict';

const fs = require('node:fs');
const path = require('node:path');

function readUtf8(file) {
  return fs.readFileSync(file, 'utf8').replace(/^\uFEFF/, '');
}

function decodeEntities(value) {
  return value
    .replace(/&amp;/g, '&')
    .replace(/&quot;/g, '"')
    .replace(/&apos;/g, "'")
    .replace(/&lt;/g, '<')
    .replace(/&gt;/g, '>');
}

function parseAttributes(tag) {
  const result = {};
  for (const match of tag.matchAll(/([A-Za-z]+)="([^"]*)"/g)) {
    result[match[1]] = decodeEntities(match[2]);
  }
  return result;
}

function parseStarts(xml, element) {
  const items = [];
  const pattern = new RegExp(`<${element}\\s+([^>]+)\\/>`, 'g');
  for (const match of xml.matchAll(pattern)) {
    const attrs = parseAttributes(match[1]);
    items.push({
      index: Number(attrs.index),
      surah: Number(attrs.sura),
      ayah: Number(attrs.aya)
    });
  }
  return items;
}

function loadCorpus(contentDir) {
  const quranLines = readUtf8(path.join(contentDir, 'quran-uthmani.txt'))
    .split(/\r?\n/)
    .filter(line => line && !line.startsWith('#'));
  const metadataXml = readUtf8(path.join(contentDir, 'quran-data.xml'));
  const translationLines = readUtf8(path.join(contentDir, 'quranenc-french-rashid-1.0.3.tsv'))
    .split(/\r?\n/)
    .filter(line => line && !line.startsWith('#'));
  const audioLines = readUtf8(path.join(contentDir, 'alafasy-128kbps-audio.tsv'))
    .split(/\r?\n/)
    .filter(line => line && !line.startsWith('#'));

  const translations = new Map();
  for (const line of translationLines) {
    const [key, encodedText = '', encodedNotes = ''] = line.split('|');
    translations.set(key, {
      translation: Buffer.from(encodedText, 'base64').toString('utf8'),
      footnotes: encodedNotes ? Buffer.from(encodedNotes, 'base64').toString('utf8') : ''
    });
  }

  const audio = new Map();
  for (const line of audioLines) {
    const [key, bytes, sha256, remoteUrl] = line.split('|');
    audio.set(key, { bytes: Number(bytes), sha256, remoteUrl });
  }

  const surahs = [];
  for (const match of metadataXml.matchAll(/<sura\s+([^>]+)\/>/g)) {
    const attrs = parseAttributes(match[1]);
    surahs.push({
      index: Number(attrs.index),
      ayahs: Number(attrs.ayas),
      name: attrs.name,
      transliteration: attrs.tname,
      englishName: attrs.ename,
      revelation: attrs.type
    });
  }
  surahs.sort((a, b) => a.index - b.index);

  const pageStarts = parseStarts(metadataXml, 'page');
  const juzStarts = parseStarts(metadataXml, 'juz');
  const order = new Map();
  let absolute = 0;
  for (const surah of surahs) {
    for (let ayah = 1; ayah <= surah.ayahs; ayah += 1) {
      order.set(`${surah.index}:${ayah}`, absolute++);
    }
  }
  const startsWithOrder = entries => entries
    .map(entry => ({ ...entry, absolute: order.get(`${entry.surah}:${entry.ayah}`) ?? 0 }))
    .sort((a, b) => a.absolute - b.absolute);
  const pages = startsWithOrder(pageStarts);
  const juzs = startsWithOrder(juzStarts);

  let timingEntries = [];
  const timingPath = path.join(contentDir, 'alafasy-128kbps-timings.json');
  if (fs.existsSync(timingPath)) {
    timingEntries = JSON.parse(readUtf8(timingPath));
  }
  const timings = new Map(timingEntries.map(item => [`${item.surah}:${item.ayah}`, item.segments]));

  let pageCursor = 0;
  let juzCursor = 0;
  const verses = quranLines.map((line, index) => {
    const first = line.indexOf('|');
    const second = line.indexOf('|', first + 1);
    const surah = Number(line.slice(0, first));
    const ayah = Number(line.slice(first + 1, second));
    const text = line.slice(second + 1);
    const key = `${surah}:${ayah}`;
    while (pageCursor + 1 < pages.length && pages[pageCursor + 1].absolute <= index) pageCursor += 1;
    while (juzCursor + 1 < juzs.length && juzs[juzCursor + 1].absolute <= index) juzCursor += 1;
    return {
      key,
      surah,
      ayah,
      text,
      translation: translations.get(key)?.translation || '',
      footnotes: translations.get(key)?.footnotes || '',
      page: pages[pageCursor]?.index || 1,
      juz: juzs[juzCursor]?.index || 1,
      audio: audio.get(key) || null,
      timings: timings.get(key) || []
    };
  });

  return {
    version: 'hifz-content-2-tanzil1.1-quranenc1.0.3-alafasy128',
    surahs,
    verses,
    pageCount: pages.length,
    juzCount: juzs.length
  };
}

module.exports = { loadCorpus };
