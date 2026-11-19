;(function exposeLoopEngine(root, factory) {
  const api = factory();
  if (typeof module === 'object' && module.exports) module.exports = api;
  if (root) root.HifzLoop = api;
})(typeof globalThis !== 'undefined' ? globalThis : this, function createLoopEngine() {
  'use strict';

  const INFINITE = -1;
  const SUPPORTED = new Set([1, 3, 5, 10, INFINITE]);

  function safeTarget(value) {
    return SUPPORTED.has(Number(value)) ? Number(value) : 1;
  }

  function afterTrackEnded(currentRepeatCount, repeatTarget) {
    const count = Math.max(1, Number(currentRepeatCount) || 1);
    const target = safeTarget(repeatTarget);
    if (target === INFINITE || count < target) {
      return { type: 'repeat', nextRepeatCount: count + 1 };
    }
    return { type: 'verse-completed' };
  }

  function afterVerseCompleted(currentAyah, startAyah, endAyah, currentSetCycle, setRepeatTarget) {
    const start = Math.max(1, Number(startAyah) || 1);
    const end = Math.max(start, Number(endAyah) || start);
    const ayah = Math.min(end, Math.max(start, Number(currentAyah) || start));
    const cycle = Math.max(1, Number(currentSetCycle) || 1);
    const target = safeTarget(setRepeatTarget);

    if (ayah < end) return { type: 'continue', nextAyah: ayah + 1, nextSetCycle: cycle };
    if (target === INFINITE || cycle < target) {
      return { type: 'continue', nextAyah: start, nextSetCycle: cycle + 1 };
    }
    return { type: 'set-completed' };
  }

  return { INFINITE, afterTrackEnded, afterVerseCompleted };
});
