'use strict';

const test = require('node:test');
const assert = require('node:assert/strict');
const path = require('node:path');
const { loadCorpus } = require('../core/content-loader');

const contentDir = path.join(__dirname, '..', '..', 'app', 'src', 'main', 'assets', 'content', 'tanzil-uthmani-1.1');
const corpus = loadCorpus(contentDir);

test('le corpus desktop contient 114 sourates et 6236 versets', () => {
  assert.equal(corpus.surahs.length, 114);
  assert.equal(corpus.verses.length, 6236);
});

test('les pages et juz du Mushaf sont complets', () => {
  assert.equal(corpus.pageCount, 604);
  assert.equal(corpus.juzCount, 30);
  assert.equal(corpus.verses.at(-1).page, 604);
  assert.equal(corpus.verses.at(-1).juz, 30);
});

test('le texte, la traduction et l’inventaire audio sont liés', () => {
  const first = corpus.verses[0];
  assert.equal(first.key, '1:1');
  assert.ok(first.text.length > 5);
  assert.ok(first.translation.includes('Allah'));
  assert.equal(first.audio.bytes, 146830);
  assert.match(first.audio.remoteUrl, /^https:\/\/everyayah\.com\//);
  assert.match(first.audio.sha256, /^[a-f0-9]{64}$/);
});

test('les timings mot à mot connus sont chargés', () => {
  assert.ok(corpus.verses[0].timings.length >= 4);
});
