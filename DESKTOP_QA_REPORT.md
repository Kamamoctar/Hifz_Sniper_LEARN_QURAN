# Rapport QA — Hifz-Sniper Desktop 0.9.1

Date : 19 novembre 2026 (UTC)

## Verdict

La version Windows x64 est fonctionnelle, autonome et packagée en exécutable portable. Le corpus est embarqué, le lecteur réseau fonctionne, l'auto-avance est réelle et les écrans principaux ont été vérifiés dans la fenêtre Windows.

## Tests automatisés

Commande :

```powershell
cd desktop
npm test
```

Résultat : 13 tests exécutés, 13 réussis, 0 échec.

Couverture :

- corpus de 114 sourates et 6 236 versets ;
- 604 pages et 30 juz ;
- texte, traduction, inventaire audio et timings liés ;
- répétitions finies et infinies ;
- Set mono-verset et multi-versets ;
- retour au début et fin en pause ;
- valeur de boucle invalide traitée en un passage sûr ;
- normalisation et sauvegarde JSON atomique.

Audit npm de production : 0 vulnérabilité signalée.

Test réseau réel `npm run test:live-audio` : le verset 1:1 a été téléchargé (146 830 octets), contrôlé avec l'empreinte attendue `8808f3fc313d9df87b6090ee9efc8c11859da58b0100ad92621c5ee9a93d7280`, puis le cache temporaire a été supprimé.

## Vérification visuelle

- onboarding affiché et validé ;
- accueil et objectifs affichés ;
- lecteur avec texte arabe, traduction et progression ;
- lecture Alafasy réelle depuis la source principale ;
- surlignage mot à mot observé ;
- auto-avance 1:1 → 1:2 → 1:3 ;
- retour à 1:1 en pause après la fin du Set ;
- répétition de verset 3× et boucle de Set 3× activées ensemble ;
- Hifz-Time affiché et passage Pré-écoute → Découpage vérifié.

## Exécutable

- nom : `Hifz-Sniper-Desktop-0.9.1-portable.exe` ;
- taille : 111 555 776 octets ;
- SHA-256 : `8FFC696ABA585DFC16CD0468F32EC3C50B1C77D1E28DEDD513CB3B58A7F6D682` ;
- format : NSIS portable x64 ;
- test d'intégrité 7-Zip : `Everything is Ok` ;
- lancement depuis l'EXE portable final : réussi ;
- processus et fenêtre : réactifs ;
- signature Authenticode : absente.

## Limite de publication

Pour supprimer l'avertissement SmartScreen et distribuer publiquement à grande échelle, il reste à signer l'exécutable avec un certificat Authenticode appartenant au propriétaire du projet.
