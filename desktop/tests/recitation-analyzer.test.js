'use strict';

const test = require('node:test');
const assert = require('node:assert/strict');
const { normalizeArabic, analyzeRecitation } = require('../core/recitation-analyzer');

test('la normalisation retire les signes coraniques sans perdre les lettres', () => {
  assert.equal(normalizeArabic('ٱلرَّحْمَـٰنِ ٱلرَّحِيمِ'), 'الرحمن الرحيم');
  assert.equal(normalizeArabic('إِيَّاكَ نَعْبُدُ'), 'اياك نعبد');
});

test('une récitation non vocalisée correspond exactement au texte Uthmani', () => {
  const result = analyzeRecitation('ٱلْحَمْدُ لِلَّهِ رَبِّ ٱلْعَـٰلَمِينَ', 'الحمد لله رب العالمين');
  assert.equal(result.score, 100);
  assert.equal(result.rating, 'fluent');
  assert.equal(result.counts.correct, 4);
});

test('un mot omis est localisé sans décaler les mots suivants', () => {
  const result = analyzeRecitation('الحمد لله رب العالمين', 'الحمد لله العالمين');
  assert.equal(result.counts.missing, 1);
  assert.equal(result.expected.find(token => token.status === 'missing').normalized, 'رب');
  assert.equal(result.counts.correct, 3);
});

test('les mots remplacés et ajoutés sont signalés', () => {
  const replaced = analyzeRecitation('مالك يوم الدين', 'ملك يوم الدين');
  assert.equal(replaced.counts.close, 1);
  assert.equal(replaced.expected[0].heard, 'ملك');

  const extra = analyzeRecitation('مالك يوم الدين', 'مالك هذا يوم الدين');
  assert.equal(extra.counts.extra, 1);
  assert.equal(extra.unexpected[0].normalized, 'هذا');
});

test('une saisie vide ne produit pas de faux positif', () => {
  const result = analyzeRecitation('بسم الله الرحمن الرحيم', '');
  assert.equal(result.score, 0);
  assert.equal(result.rating, 'missed');
  assert.equal(result.counts.missing, 4);
});
