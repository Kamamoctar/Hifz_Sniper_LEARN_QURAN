# SUPERPROMPT HIFZ-SNIPER v4

Tu dois exécuter ce cahier des charges comme une mission de développement complète. Tu ne dois pas seulement proposer une architecture, une maquette, des extraits de code ou des instructions générales : tu dois créer un projet Android fonctionnel, compilable, testé et livrable.

---

# 1. RÔLE, MISSION ET RÈGLES DE TRAVAIL

Tu es un développeur Android senior, architecte logiciel et ingénieur QA. Tu maîtrises notamment :

- Kotlin ;
- Jetpack Compose ;
- l’architecture Android moderne ;
- Room/SQLite ;
- DataStore ;
- Media3/ExoPlayer ;
- WorkManager ;
- Hilt ;
- Coroutines et Flow ;
- les applications offline-first ;
- les algorithmes de répétition espacée ;
- la synchronisation d’un texte mot par mot avec une piste audio ;
- les tests unitaires, les tests d’intégration et les tests d’interface ;
- Gradle et la production d’APK Android réellement installables.

Ta mission est de concevoir et développer une application Android de mémorisation du Coran appelée **Hifz-Sniper**.

Le besoin central n’est pas de créer une application de lecture généraliste. L’application doit aider une personne qui dispose de peu de temps à mémoriser durablement le Coran grâce à des séances courtes, ciblées et intelligemment planifiées.

Tu dois travailler de manière autonome. Ne pose une question que si une contrainte bloque réellement la compilation ou l’intégrité du contenu. Dans tous les autres cas :

1. choisis l’hypothèse la plus sûre ;
2. documente cette hypothèse ;
3. implémente-la ;
4. compile ;
5. teste ;
6. corrige les erreurs avant de poursuivre.

Ne prétends jamais qu’un APK, un test ou une fonctionnalité existe si tu ne l’as pas réellement produit ou exécuté.

---

# 2. RÉSULTAT FINAL OBLIGATOIRE

À la fin de la mission, tu dois fournir au minimum :

1. le projet Android complet ;
2. tous les fichiers Gradle nécessaires ;
3. un code source propre, cohérent et compilable ;
4. une base locale fonctionnelle ;
5. un jeu de données de démonstration licite permettant de tester l’application ;
6. un mécanisme documenté permettant d’intégrer les données coraniques complètes sans altérer le texte ;
7. des tests automatiques ;
8. un rapport des commandes exécutées et de leurs résultats ;
9. un **APK de débogage installable** ;
10. un APK de release si les paramètres de signature sont disponibles ;
11. une archive ZIP du projet source ;
12. un README précis expliquant l’installation, la compilation, les sources de données et les limites restantes.

Un fichier ZIP contenant uniquement le code source ne remplace pas l’APK. Le projet source et l’APK sont deux livrables distincts.

Le livrable minimal installable doit être généré par une commande réelle du type :

```bash
./gradlew clean testDebugUnitTest lintDebug assembleDebug
```

L’APK de débogage attendu doit normalement se trouver dans :

```text
app/build/outputs/apk/debug/app-debug.apk
```

Pour la release, prépare également :

```bash
./gradlew assembleRelease
```

Ne stocke aucune clé privée ni aucun mot de passe réel dans le dépôt. Fournis un fichier `keystore.properties.example`. Si tu génères une clé de développement temporaire afin de produire un APK signé, indique clairement qu’elle ne doit pas être utilisée pour une publication officielle sur le Play Store.

---

# 3. VISION DU PRODUIT

Hifz-Sniper doit réunir deux familles de méthodes :

## 3.1. Méthodes de mémorisation générale

- répétition espacée ou SRS ;
- rappel actif ;
- découpage en petites unités ;
- suivi précis des erreurs ;
- renforcement des éléments faibles ;
- ancrage sémantique ;
- séances courtes adaptées au temps disponible.

## 3.2. Pratiques adaptées à la mémorisation coranique

- écoute attentive d’un récitateur ;
- répétition après l’audio ;
- récitation lente et articulée ;
- continuité entre les mots et entre les versets ;
- création de « ponts de continuité » entre un passage et le suivant ;
- compréhension ponctuelle du sens ;
- révision régulière des passages nouvellement appris et des passages anciens.

L’application doit conserver le flux oral et la continuité du texte. Elle ne doit pas transformer le Coran en une simple collection de flashcards isolées.

---

# 4. UTILISATEUR CIBLE ET CONTRAINTE DE TEMPS

L’utilisateur principal :

- veut mémoriser le Coran ;
- ne dispose parfois que de 5, 10, 15 ou 20 minutes par jour ;
- a besoin que l’application choisisse automatiquement les passages prioritaires ;
- doit pouvoir apprendre, écouter ou réviser hors ligne après téléchargement ;
- doit voir immédiatement ses points faibles et les prochaines révisions ;
- ne veut pas être submergé par une interface complexe.

Au premier lancement, demande au minimum :

- le temps quotidien disponible : 5, 10, 15, 20 ou 30 minutes ;
- le niveau estimé : débutant, intermédiaire ou avancé ;
- le point de départ : sourate et verset ;
- l’objectif quotidien indicatif : nombre de versets ou durée d’apprentissage ;
- la préférence d’affichage des aides, toutes désactivées par défaut.

Ces paramètres doivent rester modifiables dans les réglages.

---

# 5. PÉRIMÈTRE FONCTIONNEL DU MVP

Le MVP doit comprendre les modules suivants.

## 5.1. Accueil du jour

Afficher :

- le temps disponible aujourd’hui ;
- le nombre de cartes ou passages à réviser ;
- la nouvelle portion proposée ;
- les points faibles prioritaires ;
- un bouton principal « Commencer ma séance » ;
- une estimation réaliste de la durée de la séance ;
- la progression de l’objectif quotidien.

L’utilisateur doit pouvoir lancer une séance en un seul geste.

## 5.2. Apprentissage ciblé

L’utilisateur sélectionne ou reçoit automatiquement une courte plage de versets.

Pour chaque verset :

1. écouter l’audio ;
2. répéter l’audio un nombre configurable de fois ;
3. découper le verset en groupes de mots cohérents ;
4. suivre le mot actuellement prononcé ;
5. masquer progressivement le texte pour favoriser le rappel actif ;
6. réciter sans assistance ;
7. indiquer s’il a récité correctement, hésité ou échoué ;
8. créer les cartes de révision correspondantes ;
9. créer un pont entre le passage actuel et le suivant.

L’apprentissage ne doit pas valider un verset uniquement parce que l’utilisateur a écouté l’audio. Il doit inclure au moins une tentative de rappel actif.

## 5.3. Lecteur coranique synchronisé

L’écran central doit afficher :

- le nom et le numéro de la sourate ;
- le numéro du verset ;
- le texte arabe en écriture uthmanie ou dans une police coranique adaptée ;
- le mot actuellement récité, clairement surligné ;
- une barre de progression audio ;
- lecture/pause ;
- verset précédent/suivant ;
- répétition du verset ;
- nombre de répétitions ;
- vitesse de lecture compatible avec la qualité de l’audio ;
- délai entre les répétitions ;
- téléchargement ou suppression de l’audio local ;
- un accès discret aux aides de compréhension.

## 5.4. Aides facultatives

Les trois aides suivantes doivent être activables séparément et être **désactivées par défaut** :

1. translittération ;
2. traduction française ;
3. tafsir ou commentaire explicatif.

L’activation de l’une ne doit pas forcer l’activation des autres.

L’application doit mémoriser la préférence de l’utilisateur. Le texte arabe doit rester visuellement prioritaire. Les aides ne doivent jamais masquer ni modifier le texte coranique.

## 5.5. Révision intelligente

La révision doit présenter en priorité :

- les cartes arrivées à échéance ;
- les passages récemment échoués ;
- les fragments fréquemment hésités ;
- les ponts de continuité fragiles ;
- les passages dont la stabilité décroît ;
- une petite portion d’ancien acquis pour vérifier la conservation à long terme.

Chaque carte doit proposer une évaluation simple :

- **Raté** ;
- **Hésitant** ;
- **Fluide**.

Dans un mode ultra-minimal, l’interface peut afficher seulement :

- « Je rate » ;
- « Je sais ».

Dans ce cas, « Je rate » équivaut à Raté et « Je sais » équivaut à Fluide. Le mode à trois niveaux reste recommandé pour mieux différencier une récitation hésitante d’une récitation réellement stable.

## 5.6. Points faibles

L’utilisateur doit pouvoir marquer précisément :

- un verset entier ;
- un groupe de mots ;
- un mot ;
- une transition entre deux versets.

Un point faible doit devenir une unité de révision indépendante sans perdre son contexte. L’écran de révision doit permettre d’afficher le verset complet sur demande.

## 5.7. Ponts de continuité

Créer des cartes spéciales de type `BRIDGE` reliant :

- la fin d’un verset au début du verset suivant ;
- la fin d’une page logique ou d’une portion d’apprentissage au début de la suivante ;
- éventuellement la fin d’une sourate au début de la suivante lorsque l’utilisateur le souhaite.

Le concept initial appelle ce mécanisme « pont Sadaqah ». Dans le code et l’interface, utilise de préférence le terme neutre et explicite **Pont de continuité**, avec l’identifiant technique `BRIDGE`.

Le pont ne doit pas se limiter arbitrairement à trois mots si la coupure produit un fragment incohérent. Par défaut, sélectionne environ trois mots avant et trois mots après la transition, mais autorise une adaptation manuelle ou algorithmique afin de conserver un groupe récitable et compréhensible.

## 5.8. Tableau de bord

Afficher au minimum :

- temps consacré aujourd’hui, cette semaine et ce mois ;
- nombre de versets appris ;
- nombre de versets révisés ;
- nombre de cartes arrivées à échéance ;
- taux de réussite ;
- nombre de points faibles ;
- évolution de la stabilité ;
- série de jours actifs, sans mécanisme culpabilisant ;
- estimation de la charge de révision des prochains jours.

Les graphiques doivent rester simples, lisibles et utiles à la décision.

---

# 6. PROTOCOLE DE MÉMORISATION « HIFZ-TIME »

Implémente un protocole concret adapté aux séances courtes.

## 6.1. Phase A — Pré-écoute

- écouter le verset une première fois ;
- suivre visuellement le surlignage mot par mot ;
- afficher la traduction ou le tafsir uniquement si l’utilisateur le demande ;
- permettre de répéter un mot ou un groupe de mots.

## 6.2. Phase B — Découpage

- découper le verset en petits groupes de mots ;
- ne jamais couper un mot ;
- éviter autant que possible les segments qui détruisent la continuité de la récitation ;
- conserver l’ordre exact du texte ;
- permettre à l’utilisateur de modifier les limites des groupes.

## 6.3. Phase C — Talaqqi assisté

Pour chaque groupe :

1. écouter ;
2. répéter après le récitateur ;
3. réécouter ;
4. réciter avec le texte partiellement visible ;
5. réciter avec le texte masqué ;
6. révéler la réponse ;
7. s’auto-évaluer.

## 6.4. Phase D — Chaînage

Après mémorisation des groupes :

- réciter groupe 1 + groupe 2 ;
- puis groupes 1 + 2 + 3 ;
- continuer jusqu’au verset complet ;
- relier le verset au verset précédent ;
- créer ou réviser le pont vers le verset suivant.

## 6.5. Phase E — Rappel différé

Avant de valider la nouvelle portion, insérer une courte interruption ou une autre carte, puis demander une nouvelle récitation sans texte. Cette étape doit éviter de confondre répétition immédiate et mémorisation réelle.

## 6.6. Phase F — Révision future

Créer les cartes suivantes selon le besoin :

- `AYAH` : verset complet ;
- `CHUNK` : fragment précis ;
- `BRIDGE` : transition ;
- `WEAK_WORD` : mot ou petite zone faible ;
- `LISTEN_ONLY` : carte d’écoute lorsqu’une révision orale n’est pas possible ;
- `SEMANTIC_CUE` : indice de sens facultatif, jamais utilisé pour modifier le texte.

---

# 7. ALGORITHME DE RÉPÉTITION ESPACÉE

Le SRS doit être déterministe, testable et documenté. Utilise une adaptation simple inspirée des principes de SM-2 ou FSRS, sans reproduire aveuglément un algorithme incompatible avec la récitation continue.

## 7.1. Données minimales d’une carte

Chaque carte doit contenir au minimum :

- `id` ;
- `type` ;
- `surahId` ;
- `ayahStart` ;
- `ayahEnd` ;
- `wordStart` ;
- `wordEnd` ;
- `createdAt` ;
- `lastReviewedAt` ;
- `nextReviewAt` ;
- `intervalDays` ;
- `easeFactor` ou paramètre équivalent ;
- `stability` ;
- `difficulty` ;
- `lapseCount` ;
- `successCount` ;
- `hesitationCount` ;
- `isWeak` ;
- `suspended` ;
- `sourceCardId` pour relier un fragment à son verset ;
- `userNote` facultative.

## 7.2. Évaluations

### Raté

- remettre la carte dans la séance en cours après quelques autres cartes ;
- réduire fortement l’intervalle ;
- augmenter le compteur d’échecs ;
- marquer la carte comme faible après un seuil configurable ;
- proposer une écoute et un découpage plus fin ;
- ne jamais supprimer l’historique.

### Hésitant

- conserver un intervalle court ;
- augmenter le compteur d’hésitations ;
- proposer le contexte ou le mot précédent comme indice ;
- maintenir la carte parmi les priorités.

### Fluide

- augmenter progressivement l’intervalle ;
- augmenter la stabilité ;
- ne pas éloigner brutalement une carte encore récente ;
- continuer à tester périodiquement la continuité avec les versets voisins.

## 7.3. Intervalles initiaux recommandés

Utilise des valeurs configurables et couvertes par des tests. Exemple de départ :

- première réussite : 10 minutes ou fin de séance ;
- deuxième réussite : 1 jour ;
- troisième réussite : 3 jours ;
- quatrième réussite : 7 jours ;
- ensuite : progression selon stabilité, difficulté et historique.

Une carte ratée ne doit pas repartir automatiquement de zéro si l’historique montre qu’elle était auparavant stable. Réduis sa stabilité de façon proportionnée.

## 7.4. Priorisation quotidienne

Calcule un score de priorité combinant au minimum :

- retard par rapport à l’échéance ;
- faiblesse connue ;
- nombre d’échecs ;
- récence de l’apprentissage ;
- importance d’un pont de continuité ;
- risque d’oubli ;
- temps disponible ;
- durée estimée de la carte.

Pseudo-formule acceptable :

```text
priority = overdueWeight
         + weaknessWeight
         + lapseWeight
         + bridgeWeight
         + lowStabilityWeight
         - estimatedDurationPenalty
```

Documente les coefficients. N’utilise pas de nombres magiques dispersés dans le code : centralise-les dans une configuration testable.

## 7.5. Adaptation au temps disponible

La file quotidienne doit respecter le budget de temps. Elle doit :

1. réserver d’abord du temps aux cartes très en retard et faibles ;
2. réserver ensuite une petite part à l’ancien acquis ;
3. ajouter une nouvelle portion seulement si le budget le permet ;
4. reporter proprement le reste sans pénaliser l’utilisateur ;
5. afficher une estimation de durée.

Si l’utilisateur ne dispose que de cinq minutes, l’application doit proposer une séance utile de cinq minutes, et non afficher une liste irréalisable.

## 7.6. Tests obligatoires du SRS

Écris des tests couvrant :

- création d’une carte ;
- première réussite ;
- échec immédiat ;
- hésitation ;
- plusieurs réussites consécutives ;
- échec après une longue période stable ;
- carte en retard ;
- changement de fuseau horaire ;
- changement d’heure du téléphone ;
- limite de temps quotidienne ;
- sélection prioritaire des ponts faibles ;
- absence de doublons dans la file ;
- persistance après redémarrage de l’application.

---

# 8. AUDIO : RÉCITATEUR UNIQUE ET SYNCHRONISATION

## 8.1. Récitateur obligatoire

Le seul récitateur autorisé dans l’application est :

**Mishary Rashid Al-Afasy**.

Contraintes :

- aucun sélecteur de récitateur ;
- aucun autre nom de récitateur dans l’interface ;
- la base de données et les métadonnées peuvent rester extensibles, mais l’application livrée ne doit exposer qu’Al-Afasy ;
- vérifier l’orthographe du nom affiché ;
- documenter précisément la source des fichiers audio et les droits ou conditions d’utilisation.

## 8.2. Correspondance stricte audio/timestamps

Le surlignage mot par mot doit utiliser les timestamps correspondant exactement :

- au même récitateur ;
- au même enregistrement ;
- au même découpage du fichier audio ;
- à la même version de métadonnées.

Il est interdit :

- d’utiliser les timings d’un autre récitateur ;
- d’inventer des timings ;
- de répartir artificiellement la durée d’un verset entre les mots comme si cette approximation était exacte ;
- de présenter un surlignage approximatif comme une synchronisation réelle.

Si des timestamps mot par mot fiables ne sont pas disponibles pour un fichier exact :

1. utiliser un surlignage au niveau du verset ;
2. afficher clairement que le mode mot par mot est indisponible pour ce fichier ;
3. conserver l’architecture permettant d’importer ultérieurement les bons timings ;
4. ne jamais produire de faux alignement.

## 8.3. Lecteur Media3

Utilise Media3/ExoPlayer avec :

- lecture locale et distante ;
- mise en cache ;
- reprise après interruption ;
- gestion des écouteurs et du Bluetooth ;
- contrôle de la répétition ;
- possibilité de ralentir raisonnablement la lecture si Media3 le permet sans dégradation excessive ;
- gestion de la perte du réseau ;
- gestion de la suppression d’un fichier ;
- état de lecture observable par `Flow` ou mécanisme équivalent ;
- synchronisation avec la position en millisecondes.

## 8.4. Téléchargement hors ligne

L’utilisateur doit pouvoir télécharger :

- un verset ;
- une sourate ;
- une plage de versets ;
- éventuellement un lot prévu pour ses prochaines séances.

Afficher :

- taille estimée ;
- progression ;
- état terminé/échoué ;
- possibilité de reprendre ;
- espace occupé ;
- suppression contrôlée.

Vérifie l’intégrité des fichiers par taille, hash ou mécanisme équivalent lorsque la source le permet.

---

# 9. INTÉGRITÉ DU TEXTE CORANIQUE ET SOURCES DE CONTENU

Le texte coranique est une donnée sensible qui ne doit jamais être générée, reformulée, corrigée automatiquement ou reconstituée par un modèle de langage.

## 9.1. Règles absolues

- utiliser une source reconnue et documentée ;
- conserver les identifiants sourate/verset ;
- conserver le texte exact de la source ;
- distinguer clairement le texte arabe, la translittération, la traduction et le tafsir ;
- ne jamais mélanger traduction et texte arabe ;
- ne jamais modifier un verset pour faciliter l’affichage ;
- ne jamais tronquer silencieusement un verset ;
- ajouter des contrôles d’intégrité ;
- documenter la version du jeu de données ;
- afficher les attributions et licences requises.

## 9.2. Gestion de la basmala

Traite explicitement la basmala selon le modèle de données de la source. Ne la duplique pas et ne l’attribue pas arbitrairement à un verset. Ajoute des tests pour les sourates concernées, notamment Al-Fatiha et At-Tawbah.

## 9.3. Sources externes et secrets

Avant d’utiliser une API ou un jeu de données :

- vérifier ses conditions d’utilisation ;
- vérifier les droits de redistribution ;
- vérifier les quotas ;
- vérifier si une clé cliente peut légalement être embarquée ;
- ne jamais placer un secret serveur dans l’APK ;
- utiliser un proxy sécurisé uniquement si une API l’exige ;
- privilégier les données publiques, versionnées et autorisées pour un mode offline-first.

Si une source nécessite des identifiants confidentiels, crée une interface de dépôt et un mécanisme de configuration, mais n’insère jamais de fausses clés.

## 9.4. Données de démonstration

Le dépôt doit rester compilable sans secret externe. Il doit inclure un petit jeu de démonstration obtenu légalement, ou des fixtures non ambiguës permettant de tester l’architecture. Pour les tests purement techniques, tu peux utiliser des chaînes factices explicitement marquées comme telles ; elles ne doivent jamais être présentées dans l’application comme des versets réels.

Le mécanisme d’import des données complètes doit être reproductible et documenté.

---

# 10. EXPÉRIENCE UTILISATEUR ET DESIGN

## 10.1. Principes

- interface calme et minimaliste ;
- priorité au texte arabe ;
- gros boutons accessibles pendant la récitation ;
- contraste suffisant ;
- mode clair et sombre ;
- navigation prévisible ;
- aucune publicité ;
- aucune gamification agressive ;
- aucune notification culpabilisante ;
- animations discrètes ;
- prise en charge des tailles de texte système sans casser la mise en page.

## 10.2. Écran de récitation

Le texte arabe doit :

- être centré ou aligné de manière naturelle pour l’arabe ;
- respecter le sens droite-vers-gauche ;
- gérer correctement les diacritiques ;
- éviter les coupures de mots ;
- permettre un zoom ou un réglage de taille ;
- surligner un mot sans déplacer les autres mots ;
- conserver un contraste suffisant en mode sombre.

## 10.3. États à gérer

Chaque écran doit avoir des états explicites :

- chargement ;
- contenu disponible ;
- contenu vide ;
- erreur réseau ;
- audio indisponible ;
- téléchargement en cours ;
- données corrompues ;
- permission refusée ;
- stockage insuffisant ;
- mode hors ligne.

Les messages d’erreur doivent indiquer une action utile : réessayer, télécharger, libérer de l’espace, afficher le contenu disponible ou retourner à la séance.

## 10.4. Accessibilité

- descriptions pour les contrôles ;
- zones tactiles suffisantes ;
- navigation clavier et lecteur d’écran dans la mesure du possible ;
- ne pas communiquer un état uniquement par la couleur ;
- contraste accessible ;
- retour haptique facultatif pour l’évaluation d’une carte.

---

# 11. ARCHITECTURE ANDROID

Utilise une architecture maintenable, sans sur-ingénierie.

## 11.1. Stack recommandée

- Kotlin ;
- Jetpack Compose ;
- Material 3 ;
- Navigation Compose ;
- ViewModel ;
- Coroutines ;
- Flow/StateFlow ;
- Room ;
- DataStore ;
- Hilt ;
- Media3 ;
- WorkManager ;
- bibliothèque réseau maintenue telle que Retrofit/OkHttp ou Ktor ;
- sérialisation Kotlin ou solution équivalente ;
- version catalog Gradle.

Utilise les versions stables et mutuellement compatibles disponibles dans l’environnement de build. Évite les versions alpha ou expérimentales sauf nécessité clairement documentée.

## 11.2. Configuration Android

- `minSdk` raisonnable pour couvrir les téléphones Android encore utilisés, idéalement 26 ou inférieur si les dépendances le permettent ;
- `compileSdk` et `targetSdk` alignés sur la version stable disponible dans l’environnement ;
- support du mode portrait ;
- comportement correct lors de la rotation si elle est autorisée ;
- sauvegarde de l’état ;
- namespace cohérent, par exemple `com.hifzsniper.app` ;
- aucune permission inutile.

## 11.3. Couches

Sépare au minimum :

### Data

- entités Room ;
- DAO ;
- sources locales ;
- sources distantes ;
- cache audio ;
- implémentations des repositories ;
- import et validation des données.

### Domain

- modèles métier ;
- use cases ;
- moteur SRS ;
- planificateur de séance ;
- calcul des statistiques ;
- règles de ponts et points faibles.

### Presentation

- écrans Compose ;
- ViewModels ;
- états d’interface immuables ;
- événements utilisateur ;
- navigation ;
- composants réutilisables.

Utilise un flux de données unidirectionnel. Évite d’exposer directement les entités Room à l’interface.

## 11.4. Modules

Pour un MVP, une structure multi-modules légère est acceptable :

```text
:app
:core:database
:core:domain
:core:audio
:core:data
:core:ui
:feature:onboarding
:feature:home
:feature:learn
:feature:review
:feature:dashboard
:feature:settings
```

Si cette modularisation ralentit inutilement le développement, utilise un seul module `app` avec des packages clairement séparés. La priorité est un projet compilable et maintenable.

---

# 12. MODÈLE DE DONNÉES ROOM

Crée des migrations et des tests de migration.

Tables minimales recommandées :

## 12.1. `surah`

- `id` ;
- `nameArabic` ;
- `nameTransliterated` ;
- `nameFrench` ;
- `ayahCount` ;
- `revelationType` facultatif ;
- `dataVersion`.

## 12.2. `ayah`

- `id` interne ;
- `surahId` ;
- `ayahNumber` ;
- `verseKey` sous la forme `sourate:verset` ;
- `textUthmani` ;
- `textSimple` facultatif ;
- `pageNumber` facultatif ;
- `juzNumber` ;
- `hizbNumber` facultatif ;
- `dataVersion` ;
- contrainte d’unicité sur `verseKey`.

## 12.3. `word`

- `id` ;
- `verseKey` ;
- `positionInAyah` ;
- `textUthmani` ;
- `transliteration` facultative ;
- `translationFrench` facultative ;
- identifiant stable provenant de la source lorsque disponible.

## 12.4. `word_timing`

- `id` ;
- `audioTrackId` ;
- `wordId` ;
- `startMs` ;
- `endMs` ;
- `sourceVersion` ;
- contrainte empêchant une durée négative ;
- index par piste et position.

## 12.5. `audio_track`

- `id` ;
- `reciterId` ;
- `reciterName` ;
- `verseKey` ou référence de sourate selon le découpage ;
- `remoteUrl` ;
- `localPath` ;
- `durationMs` ;
- `checksum` ;
- `downloadStatus` ;
- `sourceVersion` ;
- `timingVersion`.

## 12.6. `translation`

- `id` ;
- `verseKey` ;
- `languageCode` ;
- `translatorName` ;
- `text` ;
- `source` ;
- `license` ;
- `version`.

## 12.7. `tafsir`

- `id` ;
- `verseKey` ;
- `languageCode` ;
- `authorOrCollection` ;
- `text` ;
- `source` ;
- `license` ;
- `version`.

## 12.8. `learning_card`

Reprend tous les champs SRS définis plus haut et ajoute :

- `promptMode` ;
- `contextBeforeCount` ;
- `contextAfterCount` ;
- `estimatedDurationSeconds` ;
- `lastResult` ;
- `algorithmVersion`.

## 12.9. `review_log`

- `id` ;
- `cardId` ;
- `reviewedAt` ;
- `rating` ;
- `responseTimeMs` facultatif ;
- `previousInterval` ;
- `newInterval` ;
- `previousStability` ;
- `newStability` ;
- `sessionId` ;
- `wasOffline`.

## 12.10. `study_session`

- `id` ;
- `startedAt` ;
- `endedAt` ;
- `plannedMinutes` ;
- `actualSeconds` ;
- `newCardsCount` ;
- `reviewCardsCount` ;
- `successCount` ;
- `failureCount` ;
- `sessionType`.

## 12.11. `weak_point`

- `id` ;
- `verseKey` ;
- `wordStart` ;
- `wordEnd` ;
- `reason` ;
- `severity` ;
- `createdAt` ;
- `resolvedAt` ;
- `linkedCardId`.

## 12.12. `download_item`

- `id` ;
- `resourceType` ;
- `resourceKey` ;
- `remoteUrl` ;
- `localPath` ;
- `bytesDownloaded` ;
- `totalBytes` ;
- `status` ;
- `checksum` ;
- `errorMessage` ;
- `updatedAt`.

---

# 13. ÉCRANS ET NAVIGATION

Crée au minimum :

1. écran de démarrage ;
2. onboarding ;
3. accueil du jour ;
4. choix de la sourate et des versets ;
5. écran d’apprentissage ;
6. lecteur synchronisé ;
7. écran de rappel actif ;
8. écran des points faibles ;
9. écran des téléchargements ;
10. tableau de bord ;
11. réglages ;
12. écran des sources et attributions.

Navigation principale recommandée :

- Aujourd’hui ;
- Apprendre ;
- Réviser ;
- Progression ;
- Réglages.

Évite d’afficher trop d’onglets. Les écrans secondaires peuvent être accessibles depuis ces sections.

---

# 14. FONCTIONNEMENT HORS LIGNE, SYNCHRONISATION ET PERFORMANCE

## 14.1. Offline-first

La base locale est la source de vérité pour :

- progression ;
- cartes SRS ;
- historique ;
- préférences ;
- métadonnées déjà téléchargées ;
- fichiers audio locaux.

L’application doit rester utilisable sans réseau pour tout contenu déjà téléchargé.

## 14.2. Import et mise à jour

Les mises à jour de contenu doivent être versionnées. Prévois :

- un manifeste de données ;
- une version de schéma ;
- une version de contenu ;
- une validation avant remplacement ;
- un retour à la version précédente en cas d’échec ;
- aucune suppression de la progression utilisateur lors d’une mise à jour.

## 14.3. Performance

- pagination ou chargement paresseux ;
- index Room pertinents ;
- absence de requêtes lourdes sur le thread principal ;
- lecteur audio stable ;
- surlignage fluide sans recomposer tout l’écran à chaque milliseconde ;
- limitation de la consommation de batterie ;
- cache maîtrisé ;
- comportement correct sur un appareil de capacité moyenne.

---

# 15. SÉCURITÉ, CONFIDENTIALITÉ ET FIABILITÉ

- aucune collecte de données personnelle n’est nécessaire pour le MVP ;
- aucun compte obligatoire ;
- aucune publicité ;
- aucune télémétrie non consentie ;
- aucune clé secrète dans le dépôt ou l’APK ;
- logs sans texte sensible inutile ;
- validation des URL ;
- protection contre les chemins de fichiers invalides ;
- gestion sûre des téléchargements ;
- sauvegarde locale ou export facultatif de la progression ;
- import/export validé avant écrasement ;
- pas de perte de progression lors d’une mise à jour ou d’une migration.

Prévois une fonction d’export de progression dans un format documenté, par exemple JSON, sans inclure les fichiers audio. L’import doit vérifier la version et afficher un résumé avant de remplacer les données.

---

# 16. TESTS, QA ET CRITÈRES D’ACCEPTATION

## 16.1. Tests unitaires

Couvre au minimum :

- moteur SRS ;
- planificateur de séance ;
- calcul de priorité ;
- création des ponts ;
- détection des doublons ;
- calcul des statistiques ;
- validation des données ;
- conversion des timestamps ;
- préférences ;
- import/export.

## 16.2. Tests Room

- DAO ;
- contraintes d’unicité ;
- relations ;
- migrations ;
- requêtes des cartes dues ;
- conservation de l’historique ;
- transaction de mise à jour d’une carte et création du journal.

## 16.3. Tests du lecteur

Lorsque les tests instrumentés le permettent :

- lecture/pause ;
- répétition ;
- passage au mot suivant selon le timestamp ;
- changement de verset ;
- absence de faux surlignage sans timings ;
- reprise après perte du réseau ;
- lecture d’un fichier local.

## 16.4. Tests Compose

- onboarding ;
- lancement d’une séance ;
- activation/désactivation indépendante des trois aides ;
- évaluation Raté/Hésitant/Fluide ;
- affichage d’un état hors ligne ;
- affichage d’une erreur de téléchargement ;
- navigation vers le tableau de bord ;
- accessibilité des actions principales.

## 16.5. Critères d’acceptation fonctionnels

L’application est acceptable uniquement si :

1. elle se compile sans erreur ;
2. l’APK de débogage existe réellement ;
3. l’application se lance ;
4. l’utilisateur peut choisir une plage de versets ;
5. le texte arabe est affiché correctement ;
6. l’audio d’Al-Afasy peut être lu à partir d’une ressource de démonstration valide ;
7. le surlignage utilise de vrais timings ou bascule honnêtement au niveau du verset ;
8. translittération, traduction et tafsir sont indépendants et désactivés par défaut ;
9. une carte est créée après apprentissage ;
10. les cartes dues apparaissent en révision ;
11. Raté, Hésitant et Fluide modifient réellement le calendrier ;
12. un fragment faible peut être isolé ;
13. un pont de continuité peut être créé ;
14. la progression survit au redémarrage ;
15. une séance de cinq minutes respecte approximativement ce budget ;
16. le contenu téléchargé reste disponible hors ligne ;
17. aucun secret n’est présent dans l’APK ou le dépôt ;
18. les sources et licences sont documentées ;
19. les tests critiques passent ;
20. le README permet à une autre personne de reconstruire l’APK.

## 16.6. Vérifications manuelles

Teste au minimum :

- appareil ou émulateur Android récent ;
- petit écran ;
- grand texte système ;
- mode sombre ;
- réseau lent ;
- mode avion ;
- stockage presque plein ;
- interruption audio par appel ou autre média ;
- redémarrage de l’application pendant un téléchargement ;
- reprise d’une séance interrompue.

---

# 17. MÉTHODE D’EXÉCUTION ET FORMAT DE TA RÉPONSE FINALE

## 17.1. Ordre d’exécution obligatoire

Travaille dans cet ordre :

### Phase 1 — Cadrage technique

- résumer les décisions ;
- identifier les dépendances ;
- définir les sources de données ;
- signaler les contraintes de licence ;
- créer l’arborescence du projet.

### Phase 2 — Socle compilable

- créer le projet ;
- configurer Gradle ;
- créer la navigation minimale ;
- compiler immédiatement ;
- corriger toute erreur.

### Phase 3 — Données locales

- créer Room ;
- DAO ;
- repositories ;
- fixtures ;
- tests ;
- compiler et tester.

### Phase 4 — Moteur SRS

- implémenter les règles ;
- écrire les tests ;
- exécuter les tests ;
- corriger.

### Phase 5 — Lecteur audio et surlignage

- intégrer Media3 ;
- charger les timings ;
- implémenter le fallback verset ;
- tester ;
- compiler.

### Phase 6 — Parcours d’apprentissage et révision

- créer les écrans ;
- relier les actions aux données réelles ;
- tester le parcours complet.

### Phase 7 — Hors ligne et téléchargements

- implémenter WorkManager/cache ;
- gérer erreurs et reprise ;
- vérifier le mode avion.

### Phase 8 — Tableau de bord, réglages et accessibilité

- statistiques ;
- préférences ;
- toggles indépendants ;
- mode sombre ;
- accessibilité.

### Phase 9 — QA finale

Exécuter au minimum :

```bash
./gradlew clean
./gradlew testDebugUnitTest
./gradlew lintDebug
./gradlew assembleDebug
```

Exécuter également les tests instrumentés si l’environnement contient un émulateur ou un appareil :

```bash
./gradlew connectedDebugAndroidTest
```

Ne masque pas un échec. Fournis le message utile, corrige lorsque possible, puis relance.

### Phase 10 — Livrables

- vérifier l’existence de l’APK ;
- calculer son hash SHA-256 ;
- créer le ZIP du projet ;
- vérifier le ZIP ;
- produire le README ;
- présenter les chemins exacts.

## 17.2. Interdictions

Tu ne dois pas :

- t’arrêter après une maquette ;
- livrer uniquement trois fichiers de code ;
- remplacer une fonctionnalité par un commentaire `TODO` sans l’indiquer ;
- inventer un résultat de compilation ;
- inventer une source de données ;
- générer du texte coranique ;
- cacher une dépendance à une clé secrète ;
- annoncer un surlignage mot par mot si les timings ne correspondent pas ;
- fournir uniquement une archive ZIP en affirmant qu’il s’agit d’un APK ;
- demander à l’utilisateur de reconstituer lui-même le projet à partir d’extraits.

## 17.3. Format de la réponse finale

Ta réponse finale doit contenir exactement les rubriques suivantes :

### A. Résultat

Indiquer clairement ce qui a été réalisé et ce qui reste éventuellement limité.

### B. Fonctionnalités disponibles

Lister les fonctionnalités réellement opérationnelles.

### C. Architecture

Résumer les modules, les dépendances et les décisions principales.

### D. Sources de contenu

Indiquer :

- texte ;
- audio ;
- timings ;
- traduction ;
- tafsir ;
- licences et attributions ;
- éventuelles limites de redistribution.

### E. Tests exécutés

Pour chaque commande :

- commande exacte ;
- résultat ;
- nombre de tests si disponible ;
- avertissements importants.

### F. Livrables

Donner les chemins exacts vers :

- APK de débogage ;
- APK de release si produit ;
- ZIP du projet ;
- README ;
- rapport de tests.

### G. Vérification de l’APK

Fournir :

- taille ;
- hash SHA-256 ;
- nom du package ;
- `versionCode` ;
- `versionName` ;
- version Android minimale.

### H. Installation

Donner les deux méthodes :

```bash
adb install -r app-debug.apk
```

et installation manuelle sur le téléphone après autorisation des applications provenant de cette source.

### I. Limites honnêtes

Énumérer uniquement les limites réelles et expliquer comment les traiter.

---

# INSTRUCTION FINALE

Commence immédiatement par analyser ce cahier des charges, établir les décisions techniques nécessaires et créer le projet complet.

La priorité absolue est la suivante :

1. intégrité du texte coranique ;
2. exactitude de la correspondance audio/timestamps ;
3. efficacité de la mémorisation en temps limité ;
4. fonctionnement hors ligne ;
5. simplicité d’utilisation ;
6. compilation et production d’un véritable APK installable.

Ne livre pas une simple proposition. Exécute la mission, vérifie les fichiers produits et rends compte avec précision.
