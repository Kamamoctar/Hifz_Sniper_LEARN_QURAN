'use strict';

const test = require('node:test');
const assert = require('node:assert/strict');
const { INFINITE, afterTrackEnded, afterVerseCompleted } = require('../core/loop-engine');

test('la répétition finie avance seulement après le compte demandé', () => {
  assert.deepEqual(afterTrackEnded(1, 3), { type: 'repeat', nextRepeatCount: 2 });
  assert.deepEqual(afterTrackEnded(2, 3), { type: 'repeat', nextRepeatCount: 3 });
  assert.deepEqual(afterTrackEnded(3, 3), { type: 'verse-completed' });
});

test('la répétition infinie conserve un compteur exact', () => {
  assert.deepEqual(afterTrackEnded(41, INFINITE), { type: 'repeat', nextRepeatCount: 42 });
});

test('le set avance dans le cycle courant', () => {
  assert.deepEqual(afterVerseCompleted(3, 2, 5, 2, 3), { type: 'continue', nextAyah: 4, nextSetCycle: 2 });
});

test('le set fini repart de sa borne initiale', () => {
  assert.deepEqual(afterVerseCompleted(5, 2, 5, 1, 3), { type: 'continue', nextAyah: 2, nextSetCycle: 2 });
  assert.deepEqual(afterVerseCompleted(5, 2, 5, 3, 3), { type: 'set-completed' });
});

test('le set infini repart et incrémente le cycle', () => {
  assert.deepEqual(afterVerseCompleted(5, 2, 5, 11, INFINITE), { type: 'continue', nextAyah: 2, nextSetCycle: 12 });
});

test('un set mono-verset peut boucler et se terminer', () => {
  assert.deepEqual(afterVerseCompleted(7, 7, 7, 1, 3), { type: 'continue', nextAyah: 7, nextSetCycle: 2 });
  assert.deepEqual(afterVerseCompleted(7, 7, 7, 3, 3), { type: 'set-completed' });
});

test('une cible non supportée échoue de façon sûre en un passage', () => {
  assert.deepEqual(afterTrackEnded(1, 99), { type: 'verse-completed' });
  assert.deepEqual(afterVerseCompleted(1, 1, 1, 1, 99), { type: 'set-completed' });
});
