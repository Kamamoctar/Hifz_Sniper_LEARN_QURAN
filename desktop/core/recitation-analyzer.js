;(function exposeRecitationAnalyzer(root, factory) {
  const api = factory();
  if (typeof module === 'object' && module.exports) module.exports = api;
  if (root) root.HifzRecitation = api;
})(typeof globalThis !== 'undefined' ? globalThis : this, function createRecitationAnalyzer() {
  'use strict';

  function normalizeArabicForm(value, expandDaggerAlif) {
    return String(value ?? '')
      .normalize('NFKD')
      .replace(/\u0670/g, expandDaggerAlif ? 'ا' : '')
      .replace(/\p{M}/gu, '')
      .replace(/ـ/g, '')
      .replace(/[ٱأإآ]/g, 'ا')
      .replace(/ى/g, 'ي')
      .replace(/ؤ/g, 'و')
      .replace(/ئ/g, 'ي')
      .replace(/[^ء-غف-ي\s]/g, ' ')
      .replace(/\s+/g, ' ')
      .trim();
  }

  function normalizeArabic(value) {
    return normalizeArabicForm(value, false);
  }

  function normalizationVariants(value) {
    return [...new Set([
      normalizeArabicForm(value, false),
      normalizeArabicForm(value, true)
    ].filter(Boolean))];
  }

  function tokenize(value) {
    const tokens = [];
    for (const original of String(value ?? '').trim().split(/\s+/).filter(Boolean).slice(0, 500)) {
      const baseWords = normalizeArabicForm(original, false).split(/\s+/).filter(Boolean);
      const expandedWords = normalizeArabicForm(original, true).split(/\s+/).filter(Boolean);
      for (let index = 0; index < baseWords.length; index += 1) {
        const normalized = baseWords[index];
        const variants = [...new Set([normalized, expandedWords[index]].filter(Boolean))];
        tokens.push({ original, normalized, variants });
      }
    }
    return tokens;
  }

  function characterDistance(left, right) {
    if (left === right) return 0;
    if (!left.length) return right.length;
    if (!right.length) return left.length;
    let previous = Array.from({ length: right.length + 1 }, (_, index) => index);
    for (let row = 1; row <= left.length; row += 1) {
      const current = [row];
      for (let column = 1; column <= right.length; column += 1) {
        current[column] = Math.min(
          previous[column] + 1,
          current[column - 1] + 1,
          previous[column - 1] + (left[row - 1] === right[column - 1] ? 0 : 1)
        );
      }
      previous = current;
    }
    return previous[right.length];
  }

  function wordSimilarity(left, right) {
    if (left === right) return 1;
    const longest = Math.max(left.length, right.length);
    return longest ? Math.max(0, 1 - characterDistance(left, right) / longest) : 1;
  }

  function tokenSimilarity(left, right) {
    let best = 0;
    for (const leftVariant of left.variants || normalizationVariants(left.normalized)) {
      for (const rightVariant of right.variants || normalizationVariants(right.normalized)) {
        best = Math.max(best, wordSimilarity(leftVariant, rightVariant));
      }
    }
    return best;
  }

  function analyzeRecitation(expectedText, actualText) {
    const expected = tokenize(expectedText);
    const actual = tokenize(actualText);
    const rows = expected.length + 1;
    const columns = actual.length + 1;
    const costs = Array.from({ length: rows }, () => Array(columns).fill(0));
    const choices = Array.from({ length: rows }, () => Array(columns).fill(''));

    for (let row = 1; row < rows; row += 1) {
      costs[row][0] = row;
      choices[row][0] = 'missing';
    }
    for (let column = 1; column < columns; column += 1) {
      costs[0][column] = column;
      choices[0][column] = 'extra';
    }

    for (let row = 1; row < rows; row += 1) {
      for (let column = 1; column < columns; column += 1) {
        const similarity = tokenSimilarity(expected[row - 1], actual[column - 1]);
        const aligned = costs[row - 1][column - 1] + (1 - similarity);
        const missing = costs[row - 1][column] + 1;
        const extra = costs[row][column - 1] + 1;
        const lowest = Math.min(aligned, missing, extra);
        costs[row][column] = lowest;
        choices[row][column] = lowest === aligned ? 'aligned' : lowest === missing ? 'missing' : 'extra';
      }
    }

    const alignedExpected = [];
    const unexpected = [];
    let row = expected.length;
    let column = actual.length;
    while (row > 0 || column > 0) {
      const choice = choices[row][column];
      if (row > 0 && column > 0 && choice === 'aligned') {
        const similarity = tokenSimilarity(expected[row - 1], actual[column - 1]);
        alignedExpected.push({
          ...expected[row - 1],
          heard: actual[column - 1].original,
          similarity,
          status: similarity === 1 ? 'correct' : similarity >= 0.67 ? 'close' : 'replaced'
        });
        row -= 1;
        column -= 1;
      } else if (row > 0 && (choice === 'missing' || column === 0)) {
        alignedExpected.push({ ...expected[row - 1], heard: '', similarity: 0, status: 'missing' });
        row -= 1;
      } else {
        unexpected.push(actual[column - 1]);
        column -= 1;
      }
    }

    alignedExpected.reverse();
    unexpected.reverse();
    const distance = costs[expected.length][actual.length];
    const denominator = Math.max(expected.length, actual.length, 1);
    const score = actual.length ? Math.max(0, Math.min(100, Math.round((1 - distance / denominator) * 100))) : 0;
    const counts = alignedExpected.reduce((result, token) => {
      result[token.status] += 1;
      return result;
    }, { correct: 0, close: 0, replaced: 0, missing: 0, extra: unexpected.length });

    return {
      score,
      rating: score >= 90 ? 'fluent' : score >= 65 ? 'hesitant' : 'missed',
      expected: alignedExpected,
      unexpected,
      counts,
      normalizedExpected: expected.map(token => token.normalized).join(' '),
      normalizedActual: actual.map(token => token.normalized).join(' ')
    };
  }

  return { normalizeArabic, analyzeRecitation };
});
