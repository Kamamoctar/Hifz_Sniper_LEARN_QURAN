'use strict';

const fs = require('node:fs');
const path = require('node:path');

const DEFAULT_STATE = Object.freeze({
  schemaVersion: 1,
  onboarded: false,
  settings: {
    dailyMinutes: 15,
    level: 'Intermédiaire',
    startSurah: 1,
    startAyah: 1,
    dailyAyahGoal: 3,
    theme: 'dark',
    showTranslation: true
  },
  passage: { surah: 1, startAyah: 1, endAyah: 7 },
  reviews: [],
  weakPoints: [],
  sessions: [],
  downloads: {}
});

function cloneDefault() {
  return JSON.parse(JSON.stringify(DEFAULT_STATE));
}

function normalizeState(value) {
  const base = cloneDefault();
  if (!value || typeof value !== 'object' || Array.isArray(value)) return base;
  const settings = value.settings && typeof value.settings === 'object' ? value.settings : {};
  const passage = value.passage && typeof value.passage === 'object' ? value.passage : {};
  return {
    ...base,
    ...value,
    schemaVersion: 1,
    settings: { ...base.settings, ...settings },
    passage: { ...base.passage, ...passage },
    reviews: Array.isArray(value.reviews) ? value.reviews.slice(0, 10000) : [],
    weakPoints: Array.isArray(value.weakPoints) ? value.weakPoints.slice(0, 10000) : [],
    sessions: Array.isArray(value.sessions) ? value.sessions.slice(0, 20000) : [],
    downloads: value.downloads && typeof value.downloads === 'object' && !Array.isArray(value.downloads)
      ? value.downloads
      : {}
  };
}

class JsonStore {
  constructor(file) {
    this.file = file;
    fs.mkdirSync(path.dirname(file), { recursive: true });
  }

  load() {
    try {
      return normalizeState(JSON.parse(fs.readFileSync(this.file, 'utf8')));
    } catch {
      return cloneDefault();
    }
  }

  save(value) {
    const normalized = normalizeState(value);
    const temp = `${this.file}.tmp`;
    fs.writeFileSync(temp, JSON.stringify(normalized, null, 2), 'utf8');
    fs.renameSync(temp, this.file);
    return normalized;
  }
}

module.exports = { JsonStore, normalizeState, DEFAULT_STATE };
