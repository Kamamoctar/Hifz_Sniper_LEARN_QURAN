# Rapport QA final — Hifz-Sniper 0.9.1

Date de validation : 18 novembre 2026 (UTC)

## Verdict

Le projet compile, les tests automatisés passent et l'APK debug a été installé puis lancé sur Android 15 (API 35) et Android 16 (API 36). Les 5 tests instrumentés ont aussi été rejoués sur API 36 en mode avion. Les parcours critiques couverts par les tests sont fonctionnels. Les limites qui ne peuvent pas être garanties par le code seul sont listées explicitement à la fin de ce rapport.

## Vérifications exécutées

### Construction complète

Commande :

```powershell
.\gradlew.bat clean testDebugUnitTest lintDebug assembleDebug assembleRelease assembleDebugAndroidTest --console=plain
```

Résultat : `BUILD SUCCESSFUL`, 133 tâches, code de sortie 0.

### Tests unitaires

- 82 tests exécutés ;
- 82 réussis ;
- 0 échec, 0 erreur, 0 ignoré ;
- 17 suites couvrant notamment SRS, Basmala, corpus embarqué, découpage en chunks, ponts, sauvegarde/restauration, reconnaissance de récitation, téléchargements, préférences, résolution des miroirs et transitions des boucles audio.

### Tests instrumentés

Commande :

```powershell
.\gradlew.bat connectedDebugAndroidTest --console=plain
```

Résultat sur `HifzSniper_API35`, Android 15 / API 35 :

- 5 tests exécutés ;
- 5 réussis ;
- 0 échec, 0 erreur, 0 ignoré ;
- migration Room vérifiée jusqu'au schéma 3 ;
- démarrage, import initial du corpus, onboarding et navigation vers le réglage du rythme quotidien vérifiés.
- répétition réelle d'une piste WAV locale trois fois par ExoPlayer, avec une seule transition après la troisième fin de piste.
- repli réel d'une première source locale absente vers une seconde source lisible ;
- Set réel de deux versets, deux répétitions par verset et deux cycles complets avec ExoPlayer.

Deuxième passage manuel via ADB sur `medium_phone`, Android 16 / API 36, réseau coupé (`airplane_mode_on=1`, Wi-Fi désactivé) :

- 5 tests exécutés en 87,334 s ;
- 5 réussis ;
- 0 échec, 0 erreur, 0 ignoré ;
- lancement à froid de `MainActivity` réussi hors ligne ;
- aucun événement dans le tampon Android `crash` après le passage.

### Android Lint

- 0 erreur ;
- 20 avertissements non bloquants ;
- catégories restantes : 17 `GradleDependency`, 2 `NewerVersionAvailable`, 1 `AndroidGradlePluginVersion` ;
- aucun avertissement fonctionnel, de permission ou d'accessibilité restant dans le code applicatif.

### Installation et lancement réels

- installation ADB de l'APK debug : succès ;
- activité résolue : `com.hifzsniper.app/.MainActivity` ;
- activité au premier plan confirmée ;
- onboarding affiché après la préparation locale du corpus ;
- aucun `FATAL EXCEPTION` ni ANR de l'application dans le journal contrôlé.

## Corrections majeures intégrées

- Permission micro demandée à l'exécution et repli manuel si elle est refusée.
- Politique de Basmala testée pour Al-Fatiha, At-Tawbah et les autres sourates, sans duplication visuelle.
- Lecture du Mushaf guidée par les 604 métadonnées de pages et le vrai numéro de juz de chaque verset.
- Protocole Hifz-Time avec pré-écoute confirmée, chunks audio, répétitions, distraction et rappel final.
- Cartes SRS réactivées après apprentissage, cartes de pont et historique de sessions persistés transactionnellement.
- Reprise des téléchargements par HTTP Range, fichiers partiels conservés, contrôle SHA-256, allocation d'espace, annulation et suppression sûres.
- Focus audio, gestion du branchement/débranchement audio, délai entre répétitions et repli au verset quand les timings mot à mot manquent.
- Moteur de boucle déterministe testé : compteurs finis/infinis, Set mono/multi-versets, retour au début, redémarrage après fin et coordination entre répétition du verset et boucle du Set.
- Double source audio EveryAyah/Quran Foundation pour le lecteur, les chunks et les téléchargements, avec reprise HTTP inter-hôtes et contrôle SHA-256 obligatoire.
- Sauvegarde JSON strictement validée, restauration des préférences et rollback compensatoire en cas d'échec.
- Tableau de bord enrichi : charge due, temps estimé, objectifs, stabilité et sessions.
- Points faibles isolables au verset ou à une plage de mots et ouvrables dans le lecteur.
- Migration Room 2 vers 3 et export/import des nouvelles données de session.

## APK vérifiés

### Debug installable (optimisé PC & BlueStacks)

- nom : `deliverables/Hifz-Sniper-bluestacks.apk` (ou `app-debug.apk` / `Hifz-Sniper-0.9.1-debug.apk`) ;
- taille : 18 135 905 octets ;
- SHA-256 : `69DFB5C65859E3F2E4FCC3446211ACFD8DA623D6738C54E67C972A5904E11666` ;
- package : `com.hifzsniper.app` ;
- version : `0.9.1` (`versionCode` 10) ;
- minSdk : 26 ; targetSdk : 35 (Android 15 stable) ;
- architectures : `arm64-v8a`, `armeabi-v7a`, `x86`, `x86_64` ;
- signature : validée double schéma **v1 (JAR/META-INF)** + **v2/v3 (APK Scheme)**, prête pour glisser-déposer immédiat dans BlueStacks.

### Release signé (certificat de test)

- nom : `deliverables/app-release.apk` (ou `Hifz-Sniper-0.9.1-release.apk`) ;
- taille : 13 718 953 octets ;
- SHA-256 : `BEE3CCDE31CCBA16C1F145BD0CD1D8A254CB20E2A1F1548CF21EFB3F1A59527A` ;
- version : `0.9.1` (`versionCode` 10) ;
- signature : signée et vérifiée avec schéma v1 + v2 + v3.

## Limites honnêtes

- L'affichage « Mushaf par page » suit les pages du corpus, mais n'est pas un fac-similé graphique 15 lignes du Mushaf imprimé de Médine.
- La reconnaissance vocale dépend du service de reconnaissance installé sur l'appareil ; certains fournisseurs nécessitent une connexion.
- Une panne simultanée d'EveryAyah et du miroir Quran Foundation empêche encore un premier téléchargement. Les pistes déjà validées localement restent lisibles hors ligne.
- La reprise HTTP, le Bluetooth, les interruptions téléphoniques, le manque réel de stockage et les réseaux fortement dégradés n'ont pas encore été validés sur une matrice de téléphones physiques. La campagne reproductible est définie dans `BETA_TEST_PLAN.md`.
- L'APK release est compilé mais non signé : aucune clé privée de publication n'est incluse dans le projet.

## Conclusion

La version 0.9.0 est cohérente, compilable et testée pour les flux critiques. L'APK debug fourni est le livrable directement installable. La publication en boutique exige encore la signature propriétaire du build release et une campagne sur appareils physiques.
