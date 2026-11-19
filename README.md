# Hifz-Sniper

Hifz-Sniper est une application de mémorisation du Coran (*Hifz*) conçue pour des séances courtes, ciblées et hautement efficaces. Le projet fournit désormais une application Android native et une version Windows x64 autonome, sans émulateur.

## Version Windows autonome

Le dossier `desktop/` contient l'application Windows dédiée. Elle reprend le corpus complet, le lecteur audio, la répétition de verset, la boucle de Set, Hifz-Time, les révisions espacées, les points faibles, les statistiques, les téléchargements hors ligne et les sauvegardes locales.

- Exécutable portable : `deliverables/Hifz-Sniper-Desktop-0.9.1-portable.exe`
- Sources desktop reproductibles : `deliverables/Hifz-Sniper-Desktop-0.9.1-source.zip`
- Aucun Java, Android Studio, BlueStacks ou installation préalable requis.
- Données privées dans `%APPDATA%\hifz-sniper-desktop`.
- Guide complet : `DESKTOP_WINDOWS.md`.
- Rapport de validation : `DESKTOP_QA_REPORT.md`.

La version 0.9.0 implémente l'ensemble des modules prévus par le cahier des charges :
- **Onboarding & Configuration initiale** : choix du rythme (5 à 30 min/jour), niveau, sourate/verset de départ et objectif quotidien.
- **Accueil du jour** : séance en un clic, temps restant, progression de l'objectif, points faibles prioritaires et série active.
- **Sélection de passage & Set de versets** : sélection par sourate, versets début/fin, presets rapides (1, 3, 5, 7, 10 versets, toute la sourate) et navigation par page de Mushaf (1 à 604).
- **Lecteur synchronisé & Mushaf par page** :
  - Mode Verset avec surlignage mot à mot synchronisé (quran-align / Mishary Rashid Al-Afasy 128 kbps).
  - Mode Mushaf par page : lecture continue droite-à-gauche guidée par les 604 métadonnées de pages, en-têtes avec Juz & Hizb, bannières de sourates et médaillons ` ۝ ` de fin de verset. Il ne s'agit pas d'un fac-similé graphique 15 lignes du Mushaf imprimé.
  - Résolution de la Basmala selon la règle médinoise authentique : bannière d'en-tête centrée pour les sourates 2 à 114 (sauf 9), texte du verset 1 épuré de la Basmala préfixée, aucune duplication.
  - Répétition de verset (1×, 3×, 5×, 10×, ∞) et boucle de Set complet (1×, 3×, 5×, 10×, ∞) avec auto-advance ⏭ fluide.
  - Coordination sûre des deux boucles : un Set repart toujours de sa borne initiale, un cycle terminé revient au début en pause, et le mode verset infini désactive explicitement l'enchaînement du Set.
  - Vitesse variable (0.75× à 1.5×), gestion automatique du focus audio (interruption lors d'un appel) et coupure lors du débranchement des écouteurs (`AudioBecomingNoisy`).
- **Protocole Hifz-Time** :
  - Phase A : Pré-écoute guidée.
  - Phases B & C : Découpage en groupes cohérents (`AyahChunker`), écoute ciblée par fragment (`SegmentAudioPlayer`), masquage progressif (texte complet → masqué partiel → masqué total) et auto-évaluation.
  - Phase D : Chaînage cumulatif des fragments (groupes 1+2, 1+2+3...).
  - Phase E : Interlude / délai de rappel actif, puis récitation au micro (`VoiceRecitationCard`) avec permission dynamique à l'exécution.
  - Phase F : Création automatique du pont de continuité (`BRIDGE`) reliant la fin du verset au début du verset suivant.
- **Moteur SRS & Révision intelligente** :
  - Algorithme déterministe inspiré de SM-2/FSRS avec évaluations *Raté*, *Hésitant*, *Fluide*.
  - Calcul de priorité d'échéance sous contrainte stricte du budget de temps quotidien (séances calibrées pour 5, 10, 15 ou 20 minutes).
  - Gestion des types de cartes : `AYAH`, `CHUNK`, `BRIDGE`, `WEAK_WORD`.
- **Points faibles précis** :
  - Isolation possible au mot, à la plage de mots (ex: mots 3 à 7), au fragment ou au verset complet avec niveau de sévérité.
  - Consultation dans l'écran dédié et réouverture directe dans le lecteur.
- **Tableau de bord & Statistiques** :
  - Suivi de la série active (*Streak* 🔥) et record personnel.
  - Heatmap d'assiduité sur 28 jours (4 semaines).
  - Progression de l'objectif quotidien, temps estimé aujourd'hui / 7 jours / 30 jours, charge future (sous 24h, sous 7j).
- **Téléchargements hors ligne** :
  - Téléchargement à la demande par verset, par page ou par sourate.
  - Reprise HTTP réelle (`Range: bytes=...`), contrôle d'espace disque avant écriture, fichier temporaire `.part` et validation SHA-256.
  - Bascule automatique EveryAyah → miroir Quran Foundation pour la lecture, les chunks Hifz-Time et les téléchargements ; les fichiers partiels restent réutilisables entre les deux hôtes.
  - Calcul de l'espace occupé et suppression sécurisée.
- **Sauvegarde & Restauration locale** :
  - Export JSON documenté de la progression (cartes, révisions, séances `study_session`, points faibles, préférences).
  - Validation exhaustive à l'import (schéma, plages, intégrité référentielle, tailles) avec restauration atomique et rollback compensatoire.

---

## Contenu intégré & Licences

Le paquet de corpus intégré est vérifié par empreinte SHA-256 avant tout import local :
- **Texte coranique Tanzil Uthmani 1.1** : 114 sourates et 6 236 versets, sous [Tanzil Text License](https://tanzil.net/docs/Text_License).
- **Traduction française et notes explicatives** : Rachid Maach (QuranEnc), version 1.0.3, sous [conditions de republication QuranEnc](https://quranenc.com/nqo/home/about/terms).
- **Inventaire audio** : Mishary Rashid Al-Afasy 128 kbps, source EveryAyah et miroir Quran Foundation. Aucun fichier MP3 n'est embarqué dans l'APK ; chaque téléchargement est accepté uniquement si son SHA-256 correspond à l'inventaire.
- **Timings mot à mot** : corpus `quran-align`, CC BY 4.0, avec repli honnête au verset lorsque les timings ne sont pas disponibles.

---

## Architecture

- `com.hifzsniper.app.domain` : modèles métier, validateur de corpus, moteur SRS, protocole Hifz-Time, alignement vocal et calcul des séries.
- `com.hifzsniper.app.data.local` : base de données Room (`AppDatabase`), entités (`Entities.kt`), DAO (`ContentDao`, `LearningDao`, `DownloadDao`).
- `com.hifzsniper.app.data.content` : parseur sécurisé du corpus Tanzil, manifestes SHA-256 et métadonnées de page/juz.
- `com.hifzsniper.app.data.download` : planificateur WorkManager et worker sécurisé avec reprise HTTP et validation SHA-256.
- `com.hifzsniper.app.presentation` : ViewModels, états immuables Compose, Navigation Compose (12 destinations) et thème Material 3.

---

## Compilation

Prérequis : JDK 17 et Android SDK 36.

```powershell
.\gradlew.bat clean testDebugUnitTest lintDebug assembleDebug assembleRelease assembleDebugAndroidTest
```

- **Tests unitaires :** **82 tests exécutés avec succès (0 échec, 0 erreur)**.
- **Lint Android :** 0 erreur ; 20 avertissements limités aux versions d'outils et dépendances.
- **Tests instrumentés :** **5/5 tests exécutés avec succès sur Android 15 / API 35, puis 5/5 sur Android 16 / API 36 en mode avion** (migration Room, onboarding/navigation, répétition ExoPlayer, repli de source locale et Set multi-versets/multi-cycles).

---

## Livrables disponibles

Les fichiers prêts à l'emploi sont situés dans le dossier `deliverables/` :
- `deliverables/app-debug.apk` : APK de débogage signé et installable (SHA-256 : `69DFB5C65859E3F2E4FCC3446211ACFD8DA623D6738C54E67C972A5904E11666`)
- `deliverables/Hifz-Sniper-bluestacks.apk` : APK optimisé pour BlueStacks (glisser-déposer immédiat avec double signature v1 JAR + v2/v3 et support x86_64)
- `deliverables/Hifz-Sniper-0.9.1-debug.apk` : copie versionnée identique
- `deliverables/app-release.apk` : APK release signé (SHA-256 : `BEE3CCDE31CCBA16C1F145BD0CD1D8A254CB20E2A1F1548CF21EFB3F1A59527A`)
- `deliverables/Hifz-Sniper-source.zip` : archive ZIP portable et complète des sources du projet
- `deliverables/lint-results-debug.html` : rapport HTML d'analyse statique Lint
- `deliverables/unit-test-report-0.9.0/` : rapport HTML complet des tests unitaires
- `deliverables/android-test-report-0.9.0/` : rapport HTML des tests instrumentés
- `deliverables/QA_REPORT-0.9.0.md` : rapport de validation détaillé
- `deliverables/BETA_TEST_PLAN-0.9.0.md` : matrice de bêta physique, critères de feu vert et modèle de signalement
- `deliverables/CHECKSUMS-0.9.0.txt` : tailles et empreintes SHA-256 des quatre artefacts principaux

---

## Installation sur appareil Android

Avec un téléphone Android connecté en débogage USB :
```powershell
adb install -r deliverables\app-debug.apk
```
Ou copier directement `app-debug.apk` sur l'appareil et autoriser l'installation des sources inconnues.

---

## Limites connues

- La reconnaissance vocale dépend du service présent sur l'appareil et peut nécessiter Internet.
- L'audio hors ligne nécessite d'abord un téléchargement réussi ; l'application essaie automatiquement EveryAyah puis le miroir Quran Foundation et valide le fichier par SHA-256.
- Le build release est volontairement non signé ; aucune clé privée de publication n'est stockée dans le projet.
- Une campagne sur plusieurs téléphones physiques reste recommandée pour le Bluetooth, les appels, le stockage saturé et les réseaux dégradés.
