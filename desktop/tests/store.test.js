'use strict';

const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
const { JsonStore, normalizeState } = require('../core/store');

test('un état incomplet reçoit les valeurs sûres par défaut', () => {
  const state = normalizeState({ settings: { dailyMinutes: 20 }, reviews: 'invalid' });
  assert.equal(state.settings.dailyMinutes, 20);
  assert.equal(state.settings.theme, 'dark');
  assert.deepEqual(state.reviews, []);
});

test('la sauvegarde JSON est atomique et relisible', () => {
  const folder = fs.mkdtempSync(path.join(os.tmpdir(), 'hifz-desktop-test-'));
  const store = new JsonStore(path.join(folder, 'data.json'));
  store.save({ onboarded: true, settings: { dailyMinutes: 10 } });
  const loaded = store.load();
  assert.equal(loaded.onboarded, true);
  assert.equal(loaded.settings.dailyMinutes, 10);
  assert.equal(fs.existsSync(path.join(folder, 'data.json.tmp')), false);
  fs.rmSync(folder, { recursive: true, force: true });
});
