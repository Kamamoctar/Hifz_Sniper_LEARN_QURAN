'use strict';

(() => {
  const $ = selector => document.querySelector(selector);
  const screen = $('#screen');
  const audio = $('#mainAudio');
  const toastElement = $('#toast');

  let corpus;
  let state;
  let downloads = [];
  let appVersion = '';
  let verseMap = new Map();
  let surahMap = new Map();
  let currentScreen = 'home';
  let saveTimer;
  let toastTimer;
  let learnPhase = 0;
  let reviewReveal = false;
  let downloadingSet = false;
  let mediaRecorder;
  let recorderChunks = [];
  let recordingUrl = '';
  let recorderStatus = 'idle';
  let recordingSession = 0;
  let manualRecitation = '';
  let recitationAnalysis = null;
  let speechRecognizer = null;
  let speechRecognitionStatus = 'idle';
  const progressByKey = new Map();

  const player = {
    currentAyah: 1,
    repeatTarget: 1,
    setTarget: 1,
    repeatCount: 1,
    setCycle: 1,
    speed: 1,
    sources: [],
    sourceIndex: 0,
    loading: false,
    status: 'Lecture en ligne avec repli automatique',
    readyHandler: null,
    wordIndex: -1,
    view: 'verse'
  };

  const screenMeta = {
    home: ['Session du jour', 'Aujourd’hui'],
    player: ['Écoute ciblée', 'Lecteur'],
    learn: ['Protocole guidé', 'Hifz-Time'],
    review: ['Rappel actif', 'Révisions'],
    weak: ['Ciblage précis', 'Points faibles'],
    stats: ['Progression locale', 'Statistiques'],
    downloads: ['Écoute sans réseau', 'Hors ligne'],
    settings: ['Préférences et sauvegarde', 'Paramètres']
  };

  const escapeHtml = value => String(value ?? '')
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#039;');

  function toast(message, type = 'success') {
    clearTimeout(toastTimer);
    toastElement.textContent = message;
    toastElement.className = `toast show ${type === 'error' ? 'error' : ''}`;
    toastTimer = setTimeout(() => { toastElement.className = 'toast'; }, 3500);
  }

  function localDateKey(value = new Date()) {
    const date = new Date(value);
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

  function addDays(date, days) {
    const next = new Date(date);
    next.setDate(next.getDate() + days);
    return next;
  }

  function currentStreak() {
    const active = new Set(state.sessions.map(item => localDateKey(item.createdAt)));
    let cursor = new Date();
    if (!active.has(localDateKey(cursor))) cursor = addDays(cursor, -1);
    let count = 0;
    while (active.has(localDateKey(cursor))) {
      count += 1;
      cursor = addDays(cursor, -1);
    }
    return count;
  }

  function saveSoon() {
    clearTimeout(saveTimer);
    saveTimer = setTimeout(async () => {
      try { state = await window.hifz.saveState(state); }
      catch (error) { toast(`Sauvegarde impossible : ${error.message}`, 'error'); }
    }, 250);
  }

  function applyTheme() {
    document.body.classList.toggle('light', state.settings.theme === 'light');
  }

  function updateStreakBadge() {
    const streak = currentStreak();
    $('#streakBadge').textContent = `🔥 ${streak} jour${streak > 1 ? 's' : ''}`;
  }

  function getSurah(index = state.passage.surah) {
    return surahMap.get(Number(index)) || corpus.surahs[0];
  }

  function getVerse(surah = state.passage.surah, ayah = player.currentAyah) {
    return verseMap.get(`${Number(surah)}:${Number(ayah)}`);
  }

  function passageVerses() {
    const result = [];
    for (let ayah = state.passage.startAyah; ayah <= state.passage.endAyah; ayah += 1) {
      const verse = getVerse(state.passage.surah, ayah);
      if (verse) result.push(verse);
    }
    return result;
  }

  function setPassage(surahIndex, startAyah, endAyah) {
    const surah = getSurah(surahIndex);
    const start = Math.min(surah.ayahs, Math.max(1, Number(startAyah) || 1));
    const end = Math.min(surah.ayahs, Math.max(start, Number(endAyah) || start));
    state.passage = { surah: surah.index, startAyah: start, endAyah: end };
    player.currentAyah = start;
    player.repeatCount = 1;
    player.setCycle = 1;
    player.wordIndex = -1;
    resetRecitation();
    stopAudio();
    saveSoon();
  }

  function surahOptions(selected) {
    return corpus.surahs.map(item => `<option value="${item.index}" ${item.index === Number(selected) ? 'selected' : ''}>${item.index}. ${escapeHtml(item.transliteration)} — ${escapeHtml(item.name)}</option>`).join('');
  }

  function ayahOptions(surahIndex, selected) {
    const surah = getSurah(surahIndex);
    return Array.from({ length: surah.ayahs }, (_, index) => index + 1)
      .map(value => `<option value="${value}" ${value === Number(selected) ? 'selected' : ''}>${value}</option>`)
      .join('');
  }

  function formatTime(seconds) {
    if (!Number.isFinite(seconds)) return '0:00';
    const whole = Math.max(0, Math.floor(seconds));
    return `${Math.floor(whole / 60)}:${String(whole % 60).padStart(2, '0')}`;
  }

  function formatBytes(bytes) {
    if (bytes < 1024) return `${bytes} o`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} Ko`;
    return `${(bytes / 1024 / 1024).toFixed(1)} Mo`;
  }

  function render() {
    const [eyebrow, title] = screenMeta[currentScreen];
    $('#screenEyebrow').textContent = eyebrow;
    $('#screenTitle').textContent = title;
    document.querySelectorAll('.nav-item').forEach(button => button.classList.toggle('active', button.dataset.screen === currentScreen));
    updateStreakBadge();
    if (currentScreen === 'home') renderHome();
    if (currentScreen === 'player') renderPlayer();
    if (currentScreen === 'learn') renderLearn();
    if (currentScreen === 'review') renderReview();
    if (currentScreen === 'weak') renderWeakPoints();
    if (currentScreen === 'stats') renderStats();
    if (currentScreen === 'downloads') renderDownloads();
    if (currentScreen === 'settings') renderSettings();
  }

  function navigate(name) {
    currentScreen = name;
    render();
    screen.scrollTop = 0;
    screen.focus();
  }

  function renderHome() {
    const today = localDateKey();
    const sessionsToday = state.sessions.filter(item => localDateKey(item.createdAt) === today);
    const minutesToday = sessionsToday.reduce((sum, item) => sum + Number(item.minutes || 0), 0);
    const learnedToday = sessionsToday.reduce((sum, item) => sum + Number(item.learned || 0), 0);
    const due = state.reviews.filter(item => new Date(item.dueAt).getTime() <= Date.now()).length;
    const progress = Math.min(100, Math.round(minutesToday / state.settings.dailyMinutes * 100));
    const surah = getSurah();
    screen.innerHTML = `
      <section class="card hero">
        <div>
          <p class="eyebrow">${progress >= 100 ? 'Objectif accompli' : 'Prêt pour une session ciblée'}</p>
          <h2>${progress >= 100 ? 'Beau travail. Consolide maintenant.' : 'Quelques minutes peuvent ancrer un verset pour longtemps.'}</h2>
          <p>Set actuel : ${escapeHtml(surah.transliteration)}, versets ${state.passage.startAyah} à ${state.passage.endAyah}. Le lecteur conserve tes boucles et ta progression localement.</p>
          <div class="hero-actions">
            <button class="primary large" data-action="go-learn">Démarrer Hifz-Time</button>
            <button class="secondary large" data-action="go-player">Ouvrir le lecteur</button>
          </div>
        </div>
      </section>
      <div class="grid four" style="margin-top:18px">
        <article class="card metric"><div class="metric-value">${minutesToday}/${state.settings.dailyMinutes}</div><div class="metric-label">minutes aujourd’hui</div></article>
        <article class="card metric"><div class="metric-value">${learnedToday}/${state.settings.dailyAyahGoal}</div><div class="metric-label">versets travaillés</div></article>
        <article class="card metric"><div class="metric-value">${due}</div><div class="metric-label">révisions dues</div></article>
        <article class="card metric"><div class="metric-value">${state.weakPoints.length}</div><div class="metric-label">points faibles suivis</div></article>
      </div>
      <div class="section-heading"><div><p class="eyebrow">Priorités</p><h2>Prochaine action utile</h2></div></div>
      <div class="grid two">
        <article class="card flat">
          <h3>${due ? `${due} rappel${due > 1 ? 's' : ''} à effectuer` : 'Aucune révision en retard'}</h3>
          <p class="muted">Le rappel actif est le meilleur signal pour ton moteur SRS.</p>
          <button class="${due ? 'primary' : 'secondary'}" data-action="go-review">${due ? 'Réviser maintenant' : 'Voir les prochaines cartes'}</button>
        </article>
        <article class="card flat">
          <h3>Set ${surah.index}:${state.passage.startAyah}–${state.passage.endAyah}</h3>
          <p class="muted">${escapeHtml(surah.name)} · ${passageVerses().length} verset${passageVerses().length > 1 ? 's' : ''}</p>
          <button class="secondary" data-action="go-player">Écouter et répéter</button>
        </article>
      </div>`;
  }

  function renderWords(verse) {
    return verse.text.split(/\s+/).map((word, index) => `<span class="word ${index === player.wordIndex ? 'active' : ''}" data-word="${index}">${escapeHtml(word)}</span>`).join(' ');
  }

  function passageSelector() {
    return `<div class="card flat form-inline">
      <label>Sourate<select id="passageSurah">${surahOptions(state.passage.surah)}</select></label>
      <label>Début<select id="passageStart">${ayahOptions(state.passage.surah, state.passage.startAyah)}</select></label>
      <label>Fin<select id="passageEnd">${ayahOptions(state.passage.surah, state.passage.endAyah)}</select></label>
      <button class="secondary" data-action="apply-passage">Appliquer</button>
    </div>`;
  }

  function chipGroup(kind, selected) {
    return [1, 3, 5, 10, -1].map(value => `<button class="chip ${Number(selected) === value ? 'active' : ''}" data-${kind}="${value}">${value === -1 ? '∞' : `${value}×`}</button>`).join('');
  }

  function mushafMarkup(verse) {
    const pageVerses = corpus.verses.filter(item => item.page === verse.page);
    return `<div class="mushaf-page" aria-label="Page ${verse.page} du Mushaf">${pageVerses.map(item => `<span class="mushaf-ayah ${item.key === verse.key ? 'current' : ''}" data-open-ayah="${item.ayah}" data-open-surah="${item.surah}">${escapeHtml(item.text)} <span class="ayah-number">۝${item.ayah}</span></span> `).join('')}</div>`;
  }

  function renderPlayer() {
    const verse = getVerse();
    const surah = getSurah();
    if (!verse) return;
    const cached = downloads.some(item => item.key === verse.key);
    screen.innerHTML = `
      ${passageSelector()}
      <div class="player-layout" style="margin-top:18px">
        <div>
          <article class="card verse-card">
            <div class="verse-meta"><span>${escapeHtml(surah.name)} · ${surah.index}:${verse.ayah}</span><span>Page ${verse.page} · Juz ${verse.juz}</span></div>
            ${player.view === 'verse' ? `<div id="arabicWords" class="arabic">${renderWords(verse)}</div>${state.settings.showTranslation ? `<p class="translation">${escapeHtml(verse.translation)}</p>` : ''}` : mushafMarkup(verse)}
          </article>
          <div class="card flat player-controls">
            <div class="progress-row"><span id="elapsed">${formatTime(audio.currentTime)}</span><input id="seek" type="range" min="0" max="${Number.isFinite(audio.duration) ? audio.duration : 0}" step="0.05" value="${audio.currentTime || 0}" aria-label="Position audio"><span id="duration">${formatTime(audio.duration)}</span></div>
            <div class="transport">
              <button class="secondary" data-action="previous" aria-label="Verset précédent">◀</button>
              <button id="playButton" class="primary play" data-action="play" aria-label="Lecture ou pause">${audio.paused ? '▶' : 'Ⅱ'}</button>
              <button class="secondary" data-action="next" aria-label="Verset suivant">▶|</button>
            </div>
            <div id="audioStatus" class="status-line">${player.loading ? 'Chargement de la piste…' : cached && !audio.src ? 'Disponible hors ligne' : escapeHtml(player.status)}</div>
          </div>
        </div>
        <aside class="card flat">
          <div class="row between"><h3>Commandes de séance</h3><button class="ghost" data-action="toggle-view">${player.view === 'verse' ? 'Vue Mushaf' : 'Vue verset'}</button></div>
          <div class="control-group"><span>Répétition du verset · passage ${player.repeatCount}</span><div class="row">${chipGroup('repeat', player.repeatTarget)}</div></div>
          <div class="control-group"><span>Boucle du Set · cycle ${player.setCycle}</span><div class="row">${chipGroup('set', player.setTarget)}</div></div>
          <div class="control-group"><label>Vitesse<select id="speedSelect"><option value="0.75">0,75×</option><option value="0.9">0,90×</option><option value="1">1,00×</option><option value="1.25">1,25×</option><option value="1.5">1,50×</option></select></label></div>
          <div class="control-group row">
            <button class="secondary" data-action="download-current" ${cached ? 'disabled' : ''}>${cached ? '✓ Hors ligne' : '⇩ Télécharger'}</button>
            <button class="secondary" data-action="mark-weak">◎ Point faible</button>
          </div>
          <p class="muted small">Espace : lecture/pause · ←/→ : verset · Ctrl+D : télécharger.</p>
        </aside>
      </div>`;
    const speed = $('#speedSelect');
    if (speed) speed.value = String(player.speed);
  }

  function chunksForVerse(verse) {
    const words = verse.text.split(/\s+/);
    const size = Math.max(2, Math.ceil(words.length / Math.min(4, Math.max(1, Math.ceil(words.length / 4)))));
    const chunks = [];
    for (let index = 0; index < words.length; index += size) chunks.push(words.slice(index, index + size));
    return chunks;
  }

  function resetRecitation() {
    recordingSession += 1;
    if (mediaRecorder?.state === 'recording') {
      mediaRecorder.stream?.getTracks().forEach(track => track.stop());
      mediaRecorder.stop();
    }
    mediaRecorder = null;
    recorderChunks = [];
    if (speechRecognizer && speechRecognitionStatus === 'listening') speechRecognizer.abort();
    speechRecognizer = null;
    speechRecognitionStatus = 'idle';
    manualRecitation = '';
    recitationAnalysis = null;
    if (recordingUrl) URL.revokeObjectURL(recordingUrl);
    recordingUrl = '';
    recorderStatus = 'idle';
  }

  function speechRecognitionAvailable() {
    return Boolean(window.SpeechRecognition || window.webkitSpeechRecognition);
  }

  function recitationAnalysisMarkup() {
    if (!recitationAnalysis) return '';
    const result = recitationAnalysis;
    const labels = {
      fluent: 'Très proche du texte attendu',
      hesitant: 'Quelques éléments sont à consolider',
      missed: 'Réécoute et reprends les passages signalés'
    };
    const expected = result.expected.map(token => {
      const detail = token.status === 'correct'
        ? 'Correct'
        : token.status === 'missing'
          ? 'Mot non retrouvé'
          : `Entendu : ${token.heard}`;
      return `<span class="recitation-word ${token.status}" title="${escapeHtml(detail)}">${escapeHtml(token.original)}</span>`;
    }).join(' ');
    const unexpected = result.unexpected.length
      ? `<div class="recitation-extra">Mot(s) supplémentaire(s) détecté(s) : ${result.unexpected.map(token => escapeHtml(token.original)).join(' · ')}</div>`
      : '';
    return `<div class="recitation-analysis" aria-live="polite">
      <div class="recitation-summary"><div class="recitation-score">${result.score}%</div><div><strong>${labels[result.rating]}</strong><div class="muted small">${result.counts.correct} exact(s) · ${result.counts.close} proche(s) · ${result.counts.replaced} remplacé(s) · ${result.counts.missing} omis</div></div></div>
      <div class="recitation-words" aria-label="Comparaison mot à mot">${expected}</div>${unexpected}
      <p class="muted small">Cette comparaison textuelle est une aide pédagogique, pas une validation du tajwid ni de la prononciation.</p>
    </div>`;
  }

  function renderLearn() {
    const phases = ['Pré-écoute', 'Découpage', 'Masquage', 'Chaînage', 'Rappel', 'Validation'];
    const verse = getVerse(state.passage.surah, state.passage.startAyah);
    const chunks = chunksForVerse(verse);
    let body = '';
    if (learnPhase === 0) body = `<p class="muted">Écoute sans réciter. Observe le rythme, les arrêts et la structure.</p><div class="arabic">${escapeHtml(verse.text)}</div><p class="translation">${escapeHtml(verse.translation)}</p><button class="primary" data-action="learn-play">▶ Pré-écouter</button>`;
    if (learnPhase === 1) body = `<p class="muted">Travaille chaque groupe séparément avant de les réunir.</p><div class="arabic">${chunks.map((chunk, index) => `<span class="chunk"><small>${index + 1}</small> ${escapeHtml(chunk.join(' '))}</span>`).join('')}</div><button class="secondary" data-action="learn-play">▶ Réécouter le verset</button>`;
    if (learnPhase === 2) body = `<p class="muted">Récite les mots masqués, puis vérifie en cliquant sur le texte.</p><div id="maskedText" class="arabic">${verse.text.split(/\s+/).map((word, index) => `<span class="word ${index % 2 ? 'masked' : ''}">${escapeHtml(word)}</span>`).join(' ')}</div><button class="secondary" data-action="reveal-masked">Révéler tout</button>`;
    if (learnPhase === 3) body = `<p class="muted">Chaîne progressivement : 1, puis 1+2, puis 1+2+3…</p><div class="arabic">${chunks.map((chunk, index) => `<div style="font-size:${Math.max(22, 34 - index * 2)}px">${escapeHtml(chunks.slice(0, index + 1).flat().join(' '))}</div>`).join('')}</div>`;
    if (learnPhase === 4) body = `<p class="muted">Éloigne le texte, récite de mémoire, puis compare. L’enregistrement reste local.</p><div class="arabic masked" id="recallText">${escapeHtml(verse.text)}</div><div class="row" style="justify-content:center;margin:18px 0"><button class="secondary" data-action="reveal-recall">Afficher le texte</button><button class="${recorderStatus === 'recording' ? 'danger' : 'secondary'}" data-action="${recorderStatus === 'recording' ? 'stop-recording' : 'start-recording'}">${recorderStatus === 'recording' ? '■ Arrêter' : '● Enregistrer'}</button></div>${recordingUrl ? `<audio id="recorderPlayback" controls src="${recordingUrl}"></audio>` : ''}<div class="recitation-tools"><textarea id="manualRecitation" dir="rtl" lang="ar" placeholder="Saisis ta récitation, ou utilise la dictée arabe lorsqu’elle est disponible">${escapeHtml(manualRecitation)}</textarea><div class="row"><button class="secondary" data-action="${speechRecognitionStatus === 'listening' ? 'stop-speech-recognition' : 'start-speech-recognition'}" ${speechRecognitionAvailable() ? '' : 'disabled'}>${speechRecognitionStatus === 'listening' ? '■ Arrêter la dictée' : '🎙 Dictée arabe'}</button><button id="analyzeRecitation" class="primary" data-action="analyze-recitation" ${manualRecitation.trim() ? '' : 'disabled'}>Analyser localement</button><button class="ghost" data-action="clear-recitation" ${manualRecitation || recitationAnalysis ? '' : 'disabled'}>Effacer</button></div><p class="muted small">La comparaison mot à mot s’effectue entièrement sur cet ordinateur. La dictée dépend du moteur de reconnaissance fourni par Windows/Chromium et peut être indisponible ou nécessiter Internet.</p>${recitationAnalysisMarkup()}</div>`;
    if (learnPhase === 5) body = `<p class="muted">Évalue honnêtement le rappel. Le Set complet sera ajouté aux révisions espacées.</p><div class="arabic">${escapeHtml(verse.text)}</div><div class="rating-grid" style="margin-top:20px"><button class="rating missed" data-action="finish-learn" data-rating="missed">Raté<br><span class="muted small">Revoir demain</span></button><button class="rating" data-action="finish-learn" data-rating="hesitant">Hésitant<br><span class="muted small">Consolider vite</span></button><button class="rating fluent" data-action="finish-learn" data-rating="fluent">Fluide<br><span class="muted small">Espacer davantage</span></button></div>`;
    screen.innerHTML = `
      ${passageSelector()}
      <div class="phase-rail" style="margin-top:18px">${phases.map((name, index) => `<div class="phase-step ${index === learnPhase ? 'active' : index < learnPhase ? 'done' : ''}">${String.fromCharCode(65 + index)} · ${name}</div>`).join('')}</div>
      <article class="card" style="text-align:center;min-height:430px;display:flex;flex-direction:column;justify-content:center">${body}</article>
      <div class="row between" style="margin-top:16px"><button class="secondary" data-action="learn-prev" ${learnPhase === 0 ? 'disabled' : ''}>← Étape précédente</button>${learnPhase < 5 ? '<button class="primary" data-action="learn-next">Étape suivante →</button>' : ''}</div>`;
  }

  function dueReviews() {
    return state.reviews.filter(item => new Date(item.dueAt).getTime() <= Date.now()).sort((a, b) => new Date(a.dueAt) - new Date(b.dueAt));
  }

  function renderReview() {
    const due = dueReviews();
    if (!due.length) {
      const upcoming = [...state.reviews].sort((a, b) => new Date(a.dueAt) - new Date(b.dueAt)).slice(0, 6);
      screen.innerHTML = `<article class="card empty"><div class="empty-symbol">✓</div><h2>Aucune révision due</h2><p>Les prochains rappels apparaîtront ici au bon moment.</p><button class="secondary" data-action="add-current-review">Ajouter le Set actuel</button></article>${upcoming.length ? `<div class="section-heading"><h2>À venir</h2></div><div class="list">${upcoming.map(item => `<div class="list-item"><div><h3>${item.key}</h3><p>${new Date(item.dueAt).toLocaleString('fr-FR')}</p></div><span>${item.intervalDays || 1} j</span></div>`).join('')}</div>` : ''}`;
      return;
    }
    const item = due[0];
    const verse = verseMap.get(item.key);
    screen.innerHTML = `<article class="card verse-card">
      <div class="verse-meta"><span>Carte ${item.key}</span><span>${due.length} due${due.length > 1 ? 's' : ''}</span></div>
      ${reviewReveal ? `<div class="arabic">${escapeHtml(verse?.text || item.key)}</div>${state.settings.showTranslation ? `<p class="translation">${escapeHtml(verse?.translation || '')}</p>` : ''}` : `<div class="empty-symbol">؟</div><h2>Récite le verset avant de révéler</h2>`}
    </article>
    <div style="margin-top:18px">${reviewReveal ? `<div class="rating-grid"><button class="rating missed" data-action="rate-review" data-rating="missed">Raté</button><button class="rating" data-action="rate-review" data-rating="hesitant">Hésitant</button><button class="rating fluent" data-action="rate-review" data-rating="fluent">Fluide</button></div>` : '<button class="primary large" data-action="reveal-review">Révéler la réponse</button>'}</div>`;
  }

  function renderWeakPoints() {
    if (!state.weakPoints.length) {
      screen.innerHTML = `<article class="card empty"><div class="empty-symbol">◎</div><h2>Aucun point faible enregistré</h2><p>Depuis le lecteur, marque un verset à retravailler pour le retrouver ici.</p><button class="primary" data-action="go-player">Ouvrir le lecteur</button></article>`;
      return;
    }
    screen.innerHTML = `<div class="section-heading"><div><p class="eyebrow">${state.weakPoints.length} cible${state.weakPoints.length > 1 ? 's' : ''}</p><h2>À retravailler en priorité</h2></div></div><div class="list">${state.weakPoints.map((item, index) => {
      const verse = verseMap.get(item.key);
      return `<article class="list-item"><div><h3>${item.key} · ${escapeHtml(getSurah(verse?.surah).transliteration)}</h3><p class="arabic" style="font-size:21px;line-height:1.6">${escapeHtml(verse?.text || '')}</p><p>Sévérité ${item.severity || 2} · ajouté le ${new Date(item.createdAt).toLocaleDateString('fr-FR')}</p></div><div class="row"><button class="secondary" data-action="open-weak" data-index="${index}">Ouvrir</button><button class="danger" data-action="delete-weak" data-index="${index}">Supprimer</button></div></article>`;
    }).join('')}</div>`;
  }

  function renderStats() {
    const totalMinutes = state.sessions.reduce((sum, item) => sum + Number(item.minutes || 0), 0);
    const reviewCount = state.sessions.filter(item => item.type === 'review').length;
    const learned = state.sessions.reduce((sum, item) => sum + Number(item.learned || 0), 0);
    const dates = Array.from({ length: 28 }, (_, index) => addDays(new Date(), index - 27));
    const counts = new Map();
    state.sessions.forEach(item => counts.set(localDateKey(item.createdAt), (counts.get(localDateKey(item.createdAt)) || 0) + 1));
    screen.innerHTML = `<div class="grid four">
      <article class="card metric"><div class="metric-value">${currentStreak()}</div><div class="metric-label">jours de série</div></article>
      <article class="card metric"><div class="metric-value">${totalMinutes}</div><div class="metric-label">minutes cumulées</div></article>
      <article class="card metric"><div class="metric-value">${learned}</div><div class="metric-label">versets travaillés</div></article>
      <article class="card metric"><div class="metric-value">${reviewCount}</div><div class="metric-label">rappels évalués</div></article>
    </div>
    <div class="section-heading"><div><p class="eyebrow">28 derniers jours</p><h2>Régularité</h2></div></div>
    <article class="card"><div class="heatmap">${dates.map(date => { const count = counts.get(localDateKey(date)) || 0; return `<div class="heat-cell ${count >= 3 ? 'l3' : count === 2 ? 'l2' : count === 1 ? 'l1' : ''}" title="${date.toLocaleDateString('fr-FR')} · ${count} session(s)">${date.getDate()}</div>`; }).join('')}</div></article>
    <div class="section-heading"><h2>Historique récent</h2></div>
    <div class="list">${state.sessions.length ? [...state.sessions].reverse().slice(0, 12).map(item => `<div class="list-item"><div><h3>${item.type === 'review' ? 'Révision' : 'Hifz-Time'}</h3><p>${new Date(item.createdAt).toLocaleString('fr-FR')}</p></div><span>${item.minutes || 1} min</span></div>`).join('') : '<div class="card empty">Ta première session apparaîtra ici.</div>'}</div>`;
  }

  function renderDownloads() {
    const verses = passageVerses();
    const cachedKeys = new Set(downloads.map(item => item.key));
    const totalCached = downloads.reduce((sum, item) => sum + item.bytes, 0);
    const pendingBytes = verses.filter(item => !cachedKeys.has(item.key)).reduce((sum, item) => sum + Number(item.audio?.bytes || 0), 0);
    screen.innerHTML = `<div class="grid three">
      <article class="card metric"><div class="metric-value">${downloads.length}</div><div class="metric-label">pistes locales</div></article>
      <article class="card metric"><div class="metric-value">${formatBytes(totalCached)}</div><div class="metric-label">espace utilisé</div></article>
      <article class="card metric"><div class="metric-value">${formatBytes(pendingBytes)}</div><div class="metric-label">taille restante du Set</div></article>
    </div>
    <div class="section-heading"><div><p class="eyebrow">Set actuel</p><h2>${escapeHtml(getSurah().transliteration)} ${state.passage.startAyah}–${state.passage.endAyah}</h2></div><button class="primary" data-action="download-set" ${downloadingSet || verses.every(item => cachedKeys.has(item.key)) ? 'disabled' : ''}>${downloadingSet ? 'Téléchargement…' : 'Tout télécharger'}</button></div>
    <div class="list">${verses.map(verse => {
      const cached = cachedKeys.has(verse.key);
      const progress = progressByKey.get(verse.key);
      const percent = progress?.total ? Math.min(100, Math.round(progress.received / progress.total * 100)) : 0;
      return `<div class="list-item"><div><h3>${verse.key}</h3><p>${escapeHtml(verse.translation)}</p>${progress && !cached ? `<div class="download-progress"><span id="progress-${verse.key.replace(':','-')}" style="width:${percent}%"></span></div>` : ''}</div><div class="row">${cached ? `<span class="local-badge">✓ ${formatBytes(downloads.find(item => item.key === verse.key)?.bytes || 0)}</span><button class="danger" data-action="remove-download" data-key="${verse.key}">Supprimer</button>` : `<button class="secondary" data-action="download-one" data-key="${verse.key}">⇩ ${formatBytes(verse.audio?.bytes || 0)}</button>`}</div></div>`;
    }).join('')}</div>`;
  }

  function renderSettings() {
    screen.innerHTML = `<div class="grid two">
      <article class="card flat"><h2>Rythme quotidien</h2><div class="form-grid">
        <label>Minutes par jour<select id="settingMinutes">${[5,10,15,20,30].map(value => `<option ${value === Number(state.settings.dailyMinutes) ? 'selected' : ''}>${value}</option>`).join('')}</select></label>
        <label>Objectif de versets<select id="settingGoal">${[1,3,5,7,10].map(value => `<option ${value === Number(state.settings.dailyAyahGoal) ? 'selected' : ''}>${value}</option>`).join('')}</select></label>
        <label>Niveau<select id="settingLevel">${['Débutant','Intermédiaire','Avancé'].map(value => `<option ${value === state.settings.level ? 'selected' : ''}>${value}</option>`).join('')}</select></label>
        <label>Traduction<select id="settingTranslation"><option value="true" ${state.settings.showTranslation ? 'selected' : ''}>Afficher</option><option value="false" ${!state.settings.showTranslation ? 'selected' : ''}>Masquer</option></select></label>
      </div></article>
      <article class="card flat"><h2>Sauvegarde locale</h2><p class="muted">Exporte progression, révisions, points faibles et préférences dans un fichier JSON.</p><div class="row"><button class="primary" data-action="export-backup">Exporter</button><button class="secondary" data-action="import-backup">Restaurer</button><button class="ghost" data-action="open-data">Dossier des données</button></div></article>
    </div>
    <div class="section-heading"><h2>À propos</h2></div>
    <article class="card flat"><div class="grid two"><div><p><strong>Hifz Sniper Desktop ${escapeHtml(appVersion)}</strong></p><p class="muted">Corpus Tanzil Uthmani 1.1, traduction française Rachid Maach/QuranEnc, audio Mishary Rashid Al-Afasy 128 kbps.</p></div><div><p class="muted">Les pistes ne sont pas embarquées : téléchargement à la demande, miroir automatique et contrôle SHA-256.</p><button class="secondary" data-action="open-source-page">Voir les sources officielles</button></div></div></article>`;
  }

  function stopAudio() {
    if (player.readyHandler) audio.removeEventListener('canplay', player.readyHandler);
    player.readyHandler = null;
    audio.pause();
    audio.removeAttribute('src');
    audio.load();
    player.sources = [];
    player.loading = false;
    player.wordIndex = -1;
  }

  function setAudioSource(index, autoplay) {
    if (player.readyHandler) audio.removeEventListener('canplay', player.readyHandler);
    player.readyHandler = null;
    if (index >= player.sources.length) {
      player.loading = false;
      toast('Aucune source audio n’est accessible. Télécharge la piste lorsque le réseau revient.', 'error');
      refreshPlayerStatus('Piste indisponible');
      return;
    }
    player.sourceIndex = index;
    player.loading = true;
    audio.src = player.sources[index];
    audio.playbackRate = player.speed;
    audio.load();
    const ready = async () => {
      player.readyHandler = null;
      player.loading = false;
      refreshPlayerStatus(player.sources[index].startsWith('file:') ? 'Lecture hors ligne' : index > 0 ? 'Source de secours active' : 'Source principale active');
      if (autoplay) {
        try { await audio.play(); }
        catch (error) { toast(`Lecture bloquée : ${error.message}`, 'error'); }
      }
    };
    player.readyHandler = ready;
    audio.addEventListener('canplay', player.readyHandler, { once: true });
  }

  async function loadCurrentVerse(autoplay = false) {
    const verse = getVerse();
    if (!verse?.audio) return toast('Inventaire audio absent pour ce verset.', 'error');
    player.loading = true;
    refreshPlayerStatus('Recherche de la meilleure source…');
    try {
      player.sources = await window.hifz.audioSources({ key: verse.key, primaryUrl: verse.audio.remoteUrl });
      setAudioSource(0, autoplay);
    } catch (error) {
      player.loading = false;
      toast(error.message, 'error');
    }
  }

  async function togglePlayback() {
    if (!audio.src || !player.sources.length) return loadCurrentVerse(true);
    if (audio.paused) {
      try { await audio.play(); } catch (error) { toast(error.message, 'error'); }
    } else audio.pause();
  }

  async function moveVerse(delta, autoplay = false) {
    const next = Math.min(state.passage.endAyah, Math.max(state.passage.startAyah, player.currentAyah + delta));
    if (next === player.currentAyah) return;
    player.currentAyah = next;
    player.repeatCount = 1;
    player.setCycle = 1;
    player.wordIndex = -1;
    renderPlayerIfVisible();
    await loadCurrentVerse(autoplay);
  }

  async function handleTrackEnded() {
    const track = window.HifzLoop.afterTrackEnded(player.repeatCount, player.repeatTarget);
    if (track.type === 'repeat') {
      player.repeatCount = track.nextRepeatCount;
      audio.currentTime = 0;
      renderPlayerIfVisible();
      await audio.play();
      return;
    }
    player.repeatCount = 1;
    const set = window.HifzLoop.afterVerseCompleted(player.currentAyah, state.passage.startAyah, state.passage.endAyah, player.setCycle, player.setTarget);
    if (set.type === 'continue') {
      player.currentAyah = set.nextAyah;
      player.setCycle = set.nextSetCycle;
      renderPlayerIfVisible();
      await loadCurrentVerse(true);
      return;
    }
    player.currentAyah = state.passage.startAyah;
    player.setCycle = 1;
    renderPlayerIfVisible();
    await loadCurrentVerse(false);
    toast('Set terminé — retour au début en pause.');
  }

  function refreshPlayerStatus(text) {
    player.status = text;
    const status = $('#audioStatus');
    if (status) status.textContent = text;
  }

  function renderPlayerIfVisible() {
    if (currentScreen === 'player') renderPlayer();
  }

  function updatePlayerLive() {
    const play = $('#playButton');
    if (play) play.textContent = audio.paused ? '▶' : 'Ⅱ';
    const elapsed = $('#elapsed');
    const duration = $('#duration');
    const seek = $('#seek');
    if (elapsed) elapsed.textContent = formatTime(audio.currentTime);
    if (duration) duration.textContent = formatTime(audio.duration);
    if (seek && Number.isFinite(audio.duration)) {
      seek.max = String(audio.duration);
      seek.value = String(audio.currentTime);
    }
    const verse = getVerse();
    const position = audio.currentTime * 1000;
    const segment = verse?.timings?.find(item => position >= item[2] && position < item[3]);
    const nextIndex = segment ? Number(segment[0]) : -1;
    if (nextIndex !== player.wordIndex) {
      player.wordIndex = nextIndex;
      document.querySelectorAll('#arabicWords .word').forEach((element, index) => element.classList.toggle('active', index === nextIndex));
    }
  }

  async function downloadVerse(key, quiet = false) {
    if (downloads.some(item => item.key === key)) return;
    const verse = verseMap.get(key);
    if (!verse?.audio) throw new Error(`Audio absent pour ${key}`);
    try {
      const result = await window.hifz.downloadAudio({ key: verse.key, audio: verse.audio });
      downloads = result.downloads;
      progressByKey.delete(key);
      if (!quiet) toast(`${key} est maintenant disponible hors ligne.`);
      if (currentScreen === 'downloads') renderDownloads();
      if (currentScreen === 'player') renderPlayer();
    } catch (error) {
      progressByKey.delete(key);
      if (!quiet) toast(`Téléchargement ${key} : ${error.message}`, 'error');
      throw error;
    }
  }

  function addReviewsForPassage(rating = 'hesitant') {
    const now = new Date();
    for (const verse of passageVerses()) {
      const existing = state.reviews.find(item => item.key === verse.key);
      if (existing) continue;
      const delay = rating === 'fluent' ? 3 : 1;
      state.reviews.push({ key: verse.key, dueAt: addDays(now, delay).toISOString(), intervalDays: delay, repetitions: 0, ease: 2.3 });
    }
    saveSoon();
  }

  function finishLearn(rating) {
    addReviewsForPassage(rating);
    state.sessions.push({ type: 'learn', createdAt: new Date().toISOString(), minutes: Math.max(1, Math.min(state.settings.dailyMinutes, passageVerses().length * 2)), learned: passageVerses().length, rating });
    learnPhase = 0;
    resetRecitation();
    saveSoon();
    toast('Session enregistrée et révisions planifiées.');
    navigate('home');
  }

  function rateCurrentReview(rating) {
    const item = dueReviews()[0];
    if (!item) return;
    const days = Number(item.intervalDays || 1);
    if (rating === 'missed') {
      item.intervalDays = 1;
      item.repetitions = 0;
      item.ease = Math.max(1.3, Number(item.ease || 2.3) - .2);
    } else if (rating === 'hesitant') {
      item.intervalDays = Math.max(2, Math.round(days * 1.5));
      item.repetitions = Number(item.repetitions || 0) + 1;
      item.ease = Math.max(1.3, Number(item.ease || 2.3) - .05);
    } else {
      item.intervalDays = item.repetitions === 0 ? 3 : Math.max(4, Math.round(days * Number(item.ease || 2.3)));
      item.repetitions = Number(item.repetitions || 0) + 1;
      item.ease = Math.min(3, Number(item.ease || 2.3) + .05);
    }
    item.dueAt = addDays(new Date(), item.intervalDays).toISOString();
    state.sessions.push({ type: 'review', createdAt: new Date().toISOString(), minutes: 1, learned: 0, key: item.key, rating });
    reviewReveal = false;
    saveSoon();
    renderReview();
  }

  async function startRecording() {
    try {
      if (speechRecognizer && speechRecognitionStatus === 'listening') speechRecognizer.stop();
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
      const session = ++recordingSession;
      recorderChunks = [];
      mediaRecorder = new MediaRecorder(stream);
      mediaRecorder.addEventListener('dataavailable', event => { if (event.data.size) recorderChunks.push(event.data); });
      mediaRecorder.addEventListener('stop', () => {
        stream.getTracks().forEach(track => track.stop());
        if (session !== recordingSession) return;
        if (recordingUrl) URL.revokeObjectURL(recordingUrl);
        recordingUrl = URL.createObjectURL(new Blob(recorderChunks, { type: mediaRecorder.mimeType || 'audio/webm' }));
        recorderStatus = 'ready';
        if (currentScreen === 'learn') renderLearn();
      });
      mediaRecorder.start();
      recorderStatus = 'recording';
      renderLearn();
    } catch (error) {
      toast(`Microphone indisponible : ${error.message}`, 'error');
    }
  }

  function stopRecording() {
    if (mediaRecorder?.state === 'recording') mediaRecorder.stop();
  }

  function startSpeechRecognition() {
    const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
    if (!SpeechRecognition) return toast('Aucun moteur de dictée compatible n’est disponible sur ce Windows.', 'error');
    if (recorderStatus === 'recording') stopRecording();

    const baseText = manualRecitation.trim();
    const recognizer = new SpeechRecognition();
    speechRecognizer = recognizer;
    recognizer.lang = 'ar-SA';
    recognizer.continuous = true;
    recognizer.interimResults = true;
    recognizer.maxAlternatives = 1;
    speechRecognitionStatus = 'listening';

    recognizer.addEventListener('result', event => {
      let transcript = '';
      for (let index = 0; index < event.results.length; index += 1) transcript += ` ${event.results[index][0].transcript}`;
      const field = $('#manualRecitation');
      if (field) field.value = `${baseText} ${transcript}`.trim();
    });
    recognizer.addEventListener('error', event => {
      const messages = {
        'not-allowed': 'Accès au microphone refusé.',
        'audio-capture': 'Aucun microphone utilisable.',
        network: 'Le service de dictée n’est pas joignable.'
      };
      toast(messages[event.error] || `Dictée interrompue : ${event.error}`, 'error');
    });
    recognizer.addEventListener('end', () => {
      if (speechRecognizer !== recognizer) return;
      manualRecitation = $('#manualRecitation')?.value.trim() || manualRecitation;
      speechRecognitionStatus = 'idle';
      speechRecognizer = null;
      if (manualRecitation) recitationAnalysis = window.HifzRecitation.analyzeRecitation(getVerse(state.passage.surah, state.passage.startAyah).text, manualRecitation);
      if (currentScreen === 'learn') renderLearn();
    });

    try {
      recognizer.start();
      renderLearn();
    } catch (error) {
      speechRecognitionStatus = 'idle';
      speechRecognizer = null;
      toast(`Dictée impossible : ${error.message}`, 'error');
    }
  }

  function stopSpeechRecognition() {
    if (speechRecognizer && speechRecognitionStatus === 'listening') speechRecognizer.stop();
  }

  async function handleAction(action, element) {
    if (action === 'go-player') return navigate('player');
    if (action === 'go-learn') return navigate('learn');
    if (action === 'go-review') return navigate('review');
    if (action === 'play') return togglePlayback();
    if (action === 'previous') return moveVerse(-1, !audio.paused);
    if (action === 'next') return moveVerse(1, !audio.paused);
    if (action === 'toggle-view') { player.view = player.view === 'verse' ? 'mushaf' : 'verse'; return renderPlayer(); }
    if (action === 'apply-passage') {
      setPassage($('#passageSurah').value, $('#passageStart').value, $('#passageEnd').value);
      return render();
    }
    if (action === 'download-current') return downloadVerse(getVerse().key);
    if (action === 'mark-weak') {
      const verse = getVerse();
      if (!state.weakPoints.some(item => item.key === verse.key)) {
        state.weakPoints.push({ key: verse.key, severity: 2, createdAt: new Date().toISOString() });
        saveSoon();
        toast(`${verse.key} ajouté aux points faibles.`);
      } else toast(`${verse.key} est déjà suivi.`);
      return;
    }
    if (action === 'learn-play') { player.currentAyah = state.passage.startAyah; return loadCurrentVerse(true); }
    if (action === 'learn-prev') { learnPhase = Math.max(0, learnPhase - 1); return renderLearn(); }
    if (action === 'learn-next') { learnPhase = Math.min(5, learnPhase + 1); return renderLearn(); }
    if (action === 'reveal-masked') { document.querySelectorAll('#maskedText .masked').forEach(item => item.classList.remove('masked')); return; }
    if (action === 'reveal-recall') { $('#recallText')?.classList.remove('masked'); return; }
    if (action === 'finish-learn') return finishLearn(element.dataset.rating);
    if (action === 'start-recording') return startRecording();
    if (action === 'stop-recording') return stopRecording();
    if (action === 'start-speech-recognition') return startSpeechRecognition();
    if (action === 'stop-speech-recognition') return stopSpeechRecognition();
    if (action === 'analyze-recitation') {
      manualRecitation = $('#manualRecitation')?.value.trim() || '';
      if (!manualRecitation) return toast('Saisis ou dicte d’abord la récitation à comparer.', 'error');
      recitationAnalysis = window.HifzRecitation.analyzeRecitation(getVerse(state.passage.surah, state.passage.startAyah).text, manualRecitation);
      return renderLearn();
    }
    if (action === 'clear-recitation') {
      manualRecitation = '';
      recitationAnalysis = null;
      return renderLearn();
    }
    if (action === 'reveal-review') { reviewReveal = true; return renderReview(); }
    if (action === 'rate-review') return rateCurrentReview(element.dataset.rating);
    if (action === 'add-current-review') { addReviewsForPassage(); toast('Set ajouté aux révisions.'); return renderReview(); }
    if (action === 'open-weak') {
      const item = state.weakPoints[Number(element.dataset.index)];
      const [surah, ayah] = item.key.split(':').map(Number);
      setPassage(surah, ayah, ayah);
      return navigate('player');
    }
    if (action === 'delete-weak') {
      state.weakPoints.splice(Number(element.dataset.index), 1);
      saveSoon();
      return renderWeakPoints();
    }
    if (action === 'download-one') return downloadVerse(element.dataset.key);
    if (action === 'download-set') {
      downloadingSet = true;
      renderDownloads();
      let failures = 0;
      for (const verse of passageVerses()) {
        if (downloads.some(item => item.key === verse.key)) continue;
        try { await downloadVerse(verse.key, true); } catch { failures += 1; }
      }
      downloadingSet = false;
      renderDownloads();
      toast(failures ? `${failures} piste(s) n’ont pas pu être téléchargées.` : 'Le Set complet est disponible hors ligne.', failures ? 'error' : 'success');
      return;
    }
    if (action === 'remove-download') {
      await window.hifz.removeAudio(element.dataset.key);
      downloads = await window.hifz.listAudio();
      renderDownloads();
      return toast('Piste locale supprimée.');
    }
    if (action === 'export-backup') {
      const result = await window.hifz.exportBackup(state);
      if (!result.canceled) toast(`Sauvegarde exportée : ${result.filePath}`);
      return;
    }
    if (action === 'import-backup') {
      try {
        const result = await window.hifz.importBackup();
        if (!result.canceled) { state = result.state; applyTheme(); render(); toast('Sauvegarde restaurée.'); }
      } catch (error) { toast(`Restauration refusée : ${error.message}`, 'error'); }
      return;
    }
    if (action === 'open-data') return window.hifz.openDataFolder();
    if (action === 'open-source-page') return window.hifz.openExternal('https://tanzil.net/docs/Text_License');
  }

  screen.addEventListener('click', event => {
    const button = event.target.closest('[data-action]');
    if (button && !button.disabled) handleAction(button.dataset.action, button).catch(error => toast(error.message, 'error'));

    const repeat = event.target.closest('[data-repeat]');
    if (repeat) {
      player.repeatTarget = Number(repeat.dataset.repeat);
      player.repeatCount = 1;
      if (player.repeatTarget === -1) player.setTarget = 1;
      renderPlayer();
    }
    const set = event.target.closest('[data-set]');
    if (set) {
      player.setTarget = Number(set.dataset.set);
      player.setCycle = 1;
      if (player.repeatTarget === -1 && player.setTarget !== 1) player.repeatTarget = 1;
      renderPlayer();
    }
    const ayah = event.target.closest('[data-open-ayah]');
    if (ayah) {
      const surah = Number(ayah.dataset.openSurah);
      const verseNumber = Number(ayah.dataset.openAyah);
      const target = verseMap.get(`${surah}:${verseNumber}`);
      if (target && target.surah === state.passage.surah && target.ayah >= state.passage.startAyah && target.ayah <= state.passage.endAyah) {
        player.currentAyah = target.ayah;
        renderPlayer();
        loadCurrentVerse(false);
      }
    }
  });

  screen.addEventListener('change', event => {
    if (event.target.id === 'passageSurah') {
      const surah = getSurah(event.target.value);
      $('#passageStart').innerHTML = ayahOptions(surah.index, 1);
      $('#passageEnd').innerHTML = ayahOptions(surah.index, Math.min(surah.ayahs, 7));
    }
    if (event.target.id === 'speedSelect') {
      player.speed = Number(event.target.value);
      audio.playbackRate = player.speed;
    }
    if (event.target.id === 'settingMinutes') state.settings.dailyMinutes = Number(event.target.value);
    if (event.target.id === 'settingGoal') state.settings.dailyAyahGoal = Number(event.target.value);
    if (event.target.id === 'settingLevel') state.settings.level = event.target.value;
    if (event.target.id === 'settingTranslation') state.settings.showTranslation = event.target.value === 'true';
    if (event.target.id?.startsWith('setting')) saveSoon();
  });

  screen.addEventListener('input', event => {
    if (event.target.id === 'seek' && Number.isFinite(audio.duration)) audio.currentTime = Number(event.target.value);
    if (event.target.id === 'manualRecitation') {
      manualRecitation = event.target.value;
      recitationAnalysis = null;
      const analyze = $('#analyzeRecitation');
      if (analyze) analyze.disabled = !manualRecitation.trim();
    }
  });

  audio.addEventListener('play', updatePlayerLive);
  audio.addEventListener('pause', updatePlayerLive);
  audio.addEventListener('loadedmetadata', updatePlayerLive);
  audio.addEventListener('timeupdate', updatePlayerLive);
  audio.addEventListener('ended', () => handleTrackEnded().catch(error => toast(error.message, 'error')));
  audio.addEventListener('error', () => {
    if (!player.sources.length) return;
    const next = player.sourceIndex + 1;
    if (next < player.sources.length) setAudioSource(next, true);
    else {
      player.loading = false;
      refreshPlayerStatus('Toutes les sources audio ont échoué');
      toast('La piste est indisponible sur les deux sources.', 'error');
    }
  });

  document.querySelectorAll('.nav-item').forEach(button => button.addEventListener('click', () => navigate(button.dataset.screen)));
  $('#themeToggle').addEventListener('click', () => {
    state.settings.theme = state.settings.theme === 'light' ? 'dark' : 'light';
    applyTheme();
    saveSoon();
  });

  document.addEventListener('keydown', event => {
    const interactive = ['INPUT', 'TEXTAREA', 'SELECT'].includes(document.activeElement?.tagName);
    if (interactive) return;
    if (event.code === 'Space') { event.preventDefault(); togglePlayback(); }
    if (event.key === 'ArrowLeft') moveVerse(-1, !audio.paused);
    if (event.key === 'ArrowRight') moveVerse(1, !audio.paused);
    if (event.ctrlKey && event.key.toLowerCase() === 'd') { event.preventDefault(); downloadVerse(getVerse().key); }
  });

  window.hifz.onAudioProgress(progress => {
    progressByKey.set(progress.key, progress);
    const element = $(`#progress-${progress.key.replace(':', '-')}`);
    if (element && progress.total) element.style.width = `${Math.min(100, progress.received / progress.total * 100)}%`;
  });

  async function bootstrap() {
    try {
      const data = await window.hifz.bootstrap();
      corpus = data.corpus;
      state = data.state;
      downloads = data.downloads;
      appVersion = data.appVersion;
      verseMap = new Map(corpus.verses.map(item => [item.key, item]));
      surahMap = new Map(corpus.surahs.map(item => [item.index, item]));
      player.currentAyah = state.passage.startAyah;

      const fonts = document.createElement('style');
      fonts.textContent = `@font-face{font-family:'Amiri';src:url('${data.fontUrls.regular}')}@font-face{font-family:'Amiri';src:url('${data.fontUrls.bold}');font-weight:700}@font-face{font-family:'Amiri Quran';src:url('${data.fontUrls.quran}')}`;
      document.head.append(fonts);
      $('#versionLabel').textContent = `Version ${appVersion} · Windows x64`;
      $('#onboardingSurah').innerHTML = surahOptions(state.settings.startSurah);
      applyTheme();
      $('#loading').classList.add('hidden');
      $('#app').classList.remove('hidden');
      render();
      if (!state.onboarded) $('#onboarding').classList.remove('hidden');
    } catch (error) {
      $('#loading').innerHTML = `<div class="loading-mark">!</div><h2>Le corpus n’a pas pu être chargé</h2><p>${escapeHtml(error.message)}</p>`;
    }
  }

  $('#finishOnboarding').addEventListener('click', async () => {
    const surah = getSurah($('#onboardingSurah').value);
    const goal = Number($('#onboardingGoal').value);
    state.settings.dailyMinutes = Number($('#onboardingMinutes').value);
    state.settings.level = $('#onboardingLevel').value;
    state.settings.startSurah = surah.index;
    state.settings.startAyah = 1;
    state.settings.dailyAyahGoal = goal;
    state.passage = { surah: surah.index, startAyah: 1, endAyah: Math.min(surah.ayahs, goal) };
    state.onboarded = true;
    player.currentAyah = 1;
    state = await window.hifz.saveState(state);
    $('#onboarding').classList.add('hidden');
    render();
    toast('Ton espace Hifz est prêt.');
  });

  bootstrap();
})();
