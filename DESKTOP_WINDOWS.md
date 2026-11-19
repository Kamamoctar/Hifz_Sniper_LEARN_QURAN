# Hifz-Sniper Desktop pour Windows

## Installation

La version desktop est un exécutable portable Windows 10/11 x64 :

1. Copier `Hifz-Sniper-Desktop-0.9.1-portable.exe` dans le dossier souhaité.
2. Double-cliquer sur l'exécutable.
3. Terminer l'onboarding local.

Aucun Java, Android Studio, BlueStacks, serveur ou compte utilisateur n'est nécessaire. Le premier lancement peut prendre quelques secondes, car l'application portable extrait son moteur dans le dossier temporaire Windows.

L'archive `Hifz-Sniper-Desktop-0.9.1-source.zip` fournit le module desktop, son verrou npm, ses tests et les ressources nécessaires à une reconstruction.

## Fonctions disponibles

- corpus Tanzil Uthmani complet : 114 sourates, 6 236 versets, 604 pages et 30 juz ;
- traduction française Rachid Maach/QuranEnc et notes ;
- lecteur Alafasy 128 kbps avec EveryAyah puis miroir Quran Foundation ;
- téléchargement local avec reprise HTTP, contrôle de taille et SHA-256 ;
- surlignage mot à mot lorsque les timings quran-align sont disponibles ;
- répétition du verset 1×, 3×, 5×, 10× ou infinie ;
- boucle du Set 1×, 3×, 5×, 10× ou infinie ;
- coordination sûre des boucles et retour au début en pause après le dernier cycle ;
- vitesse audio de 0,75× à 1,50× ;
- vue verset et vue Mushaf guidée par page ;
- protocole Hifz-Time en six phases, avec masquage, chaînage et enregistrement local de la récitation ;
- révisions espacées, points faibles, série, heatmap et historique ;
- export et restauration JSON de la progression.

## Raccourcis

- `Espace` : lecture ou pause ;
- `Flèche gauche` / `Flèche droite` : verset précédent ou suivant ;
- `Ctrl+D` : télécharger le verset courant.

## Données et confidentialité

Les données sont enregistrées dans :

```text
%APPDATA%\hifz-sniper-desktop
```

Ce dossier contient `hifz-data.json` et le cache audio validé. Aucune progression n'est envoyée vers un serveur. Les seules requêtes réseau concernent les pistes audio choisies par l'utilisateur et les liens externes ouverts explicitement.

## Construction depuis les sources

Prérequis : Node.js 24 ou version LTS compatible et npm.

```powershell
cd desktop
npm install
npm test
npm run test:live-audio
npm run dist:portable
```

Le résultat est créé dans `desktop\dist\`.

## Limites honnêtes

- l'exécutable fourni n'est pas signé par un certificat Authenticode commercial ; Windows peut donc afficher un avertissement SmartScreen ;
- la reconnaissance automatique de la récitation n'est pas incluse dans cette première version desktop : l'enregistrement et l'auto-évaluation restent disponibles ;
- les pistes audio ne sont pas incorporées à l'EXE pour respecter les droits du réciteur ; elles sont téléchargées à la demande puis restent disponibles hors ligne ;
- la vue Mushaf respecte les limites des 604 pages, mais n'est pas un fac-similé graphique 15 lignes du Mushaf imprimé.
