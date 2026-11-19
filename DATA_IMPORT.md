# Import de contenu vérifié

## Flux

1. récupérer les ressources depuis les sources documentées ;
2. conserver leur version et leur attribution ;
3. calculer les SHA-256 ;
4. produire le manifeste versionné ;
5. construire le `ContentPackage` avec `BundledTanzilPackageLoader` ;
6. exécuter `ContentValidator` ;
7. insérer toutes les tables dans une transaction Room unique ;
8. initialiser les cartes SRS manquantes sans supprimer la progression.

Une erreur annule tout l’import. Une mise à niveau remplace uniquement les tables de contenu et les entrées de téléchargement associées.

## Paquet de contenu intégré

Version de contenu : `hifz-content-2-tanzil1.1-quranenc1.0.3-alafasy128`.

- `quran-uthmani.txt` — Tanzil Uthmani 1.1, 6 236 versets, SHA-256 `6933e133dd56db778c801bf738848454e43648105a151e8d84d86a7cae39ec5f` ;
- `quran-data.xml` — métadonnées Tanzil, SHA-256 `8867c1d88191472adec9db694b3cd9f135b1a2ef580574d32cf888dcb22c5c7a` ;
- `quranenc-french-rashid-1.0.3.tsv` — 6 236 traductions et notes, SHA-256 `2df906bf5819297deb8fb92ade4b0d654b959ed74e92b9a95485923ed54c44a3` ;
- `alafasy-128kbps-audio.tsv` — 6 236 URL, tailles et SHA-256, SHA-256 du manifeste `a1ff4f5eaf536cf82da657a0e2e9344ac896dca76874220dea9e5af74bf89cce` ;
- `alafasy-128kbps-timings.json` — quran-align, SHA-256 `55c1353848d8f3140abab72bb5d1d4533476a8219f6241079a425d232f001eb7` ;
- `quran-align-CC-BY-4.0-LICENSE.txt` — licence intégrale des timings.

`tools/prepare_extended_content.py` reconstruit les deux inventaires tabulaires depuis la base SQLite QuranEnc et le manifeste audio Maqra, et exige une couverture exacte des 6 236 clés de verset.

## Sources et licences

### Texte

Tanzil autorise la copie et la distribution verbatim du texte sous CC BY 3.0, interdit sa modification et exige l’attribution :

- https://tanzil.net/docs/Text_License
- https://tanzil.net/download/

### Traduction française

La traduction de Rachid Maach 1.0.3 et ses notes proviennent de QuranEnc. Les règles de republication imposent notamment de ne pas modifier le contenu, d’afficher la source et l’éditeur, de conserver la version et de suivre les corrections :

- https://quranenc.com/fr/browse/french_rashid/
- https://quranenc.com/fr/home/api/
- https://quranenc.com/nqo/home/about/terms

Les notes sont stockées dans la couche historique `tafsir` du schéma, mais l’interface les nomme correctement « notes explicatives ». Aucun tafsir français complet n’est revendiqué.

### Audio

L’APK ne contient aucun MP3. Il référence les fichiers Alafasy 128 kbps hébergés par EveryAyah et utilise le chemin Alafasy documenté par Quran Foundation comme miroir automatique. Chaque fichier est écrit dans un temporaire privé, peut reprendre via HTTP `Range` sur l’autre hôte, est contrôlé par SHA-256, puis déplacé vers son chemin final. Les droits des enregistrements restent en amont :

- https://everyayah.com/data/Alafasy_128kbps/
- https://verses.quran.foundation/Alafasy/mp3/
- https://api-docs.quran.foundation/docs/content_apis_versioned/4.0.0/recitation-audio-files/
- https://www.alafasy.co/terms-of-use

Un contrôle réseau réel du 15 novembre 2026 a comparé `001001.mp3`, `002255.mp3` et `114006.mp3` sur les deux hôtes : leurs tailles et leurs SHA-256 sont identiques. `001001.mp3` contient 146 830 octets et correspond au SHA-256 déclaré `8808f3fc313d9df87b6090ee9efc8c11859da58b0100ad92621c5ee9a93d7280`.

### Timings

Les timings quran-align sont distribués sous CC BY 4.0 :

- https://github.com/cpfair/quran-align
- https://github.com/cpfair/quran-align/releases/tag/release-2016-11-24

La segmentation est dérivée du texte Tanzil. Le chargeur n’accepte les timings mot à mot que si le nombre de segments correspond exactement, que chaque segment est individuel, positif, contigu et sans chevauchement. Les basmala supplémentaires de Tanzil sont décalées explicitement. Toute incompatibilité déclenche le fallback au verset.

## Validations bloquantes

- 114 sourates et exactement 6 236 clés contiguës ;
- texte non vide et bornes juz/hizb/page valides ;
- 77 881 positions de mots contiguës ;
- 6 236 traductions et 6 236 pistes rattachées à des versets existants ;
- récitant strictement égal à Mishary Rashid Al-Afasy ;
- URL HTTPS et SHA-256 hexadécimal pour chaque piste ;
- timings positifs, ordonnés, non chevauchants et liés au mot, au verset et à la version de piste ;
- source, licence et version obligatoires pour les couches textuelles.
