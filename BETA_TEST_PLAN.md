# Plan de bêta physique — Hifz-Sniper 0.9.0

## Objectif et feu vert

Cette campagne couvre ce qu'un émulateur ne garantit pas : audio matériel, appels, Bluetooth, stockage réellement contraint, services vocaux des constructeurs et réseaux instables.

Feu vert publication si :

- aucun crash, ANR, blocage de navigation ou perte de progression sur les scénarios P0 ;
- la répétition de verset et la boucle de Set produisent le nombre exact de lectures sur chaque appareil ;
- aucun téléchargement déclaré terminé n'échoue au contrôle SHA-256 ;
- les interruptions audio mettent en pause sans reprise sonore intempestive ;
- les anomalies P1 sont corrigées ou explicitement acceptées avant diffusion.

## Matrice minimale

Tester au moins trois téléphones physiques :

1. Android 8 à 10, appareil d'entrée de gamme, 2 à 4 Go de RAM ;
2. Android 12 à 14, appareil intermédiaire ;
3. Android 15 ou 16, appareil récent.

Inclure si possible Samsung/One UI, Pixel/Android standard et un troisième constructeur. Exécuter une fois en français, taille de police normale, puis avec police à 150 % et navigation par gestes.

## Scénarios P0

### Installation et données

- Installer le build signé sur une installation vierge, terminer l'onboarding, fermer de force puis relancer.
- Apprendre un passage, créer un point faible, noter une révision, relancer et confirmer la persistance.
- Exporter une sauvegarde, modifier les données, restaurer et vérifier progression, préférences et sessions.
- Refuser le micro : la séance doit continuer avec l'évaluation manuelle. Accorder ensuite la permission et réessayer.

### Répétition et boucle de Set

- Verset unique : 1×, 3×, 10× puis ∞ ; compter les fins audibles et arrêter manuellement ∞.
- Set d'un verset : 3 cycles ; confirmer trois lectures et retour au début en pause.
- Set de deux versets : 2 répétitions par verset, 2 cycles ; séquence attendue A A B B / A A B B, puis retour à A en pause.
- Pendant une boucle : pause/reprise, passage manuel suivant/précédent, changement de vitesse et sortie/retour écran.
- Activer la répétition verset ∞ : la boucle de Set doit être désactivée explicitement.

### Audio matériel

- Haut-parleur, casque filaire si disponible, écouteurs Bluetooth et changement de sortie pendant la lecture.
- Débrancher le casque : arrêt immédiat, sans son transféré au haut-parleur.
- Recevoir un appel et une notification vocale/navigation : perte de focus, pause, puis reprise uniquement sur action attendue.
- Verrouiller l'écran cinq minutes pendant une répétition, puis déverrouiller et vérifier l'état.

### Réseau et hors ligne

- Télécharger un verset, une page et une sourate sur Wi-Fi stable ; lire ensuite en mode avion.
- Couper le réseau à 20–50 % d'un téléchargement, relancer l'app, rétablir le réseau et confirmer la reprise.
- Tester Wi-Fi très lent/pertes de paquets, puis bascule Wi-Fi vers données mobiles.
- Simuler l'indisponibilité de la source principale : le miroir doit prendre le relais et le fichier final doit être validé.
- Hors ligne sans piste locale : afficher un échec compréhensible, sans crash ni boucle infinie.

### Stockage et robustesse

- Avec moins d'espace libre que le téléchargement prévu : refus propre avant écriture complète.
- Interrompre l'app pendant un téléchargement : conserver `.part`, ne jamais déclarer la piste disponible prématurément.
- Supprimer un téléchargement depuis l'app, vérifier espace récupéré et absence de données d'apprentissage supprimées.
- Lancer une séance de 20 minutes, alterner lecteur/Hifz-Time/statistiques et surveiller chauffe, batterie et fluidité.

## Scénarios P1

- TalkBack : ordre de focus, libellés des commandes, boutons activables et lecture des états.
- Thème clair/sombre, rotation, petite/grande police, contraste et textes arabes longs.
- Reconnaissance vocale en ligne puis hors ligne selon le service installé ; le repli manuel doit toujours rester possible.
- Changement de langue ou de fuseau horaire entre deux sessions ; série et échéances doivent rester cohérentes.
- Mise à jour par-dessus une version contenant une base Room v2 ; aucune perte de données.

## Fiche de signalement

Pour chaque anomalie, noter :

- modèle, constructeur, version Android et niveau de correctif ;
- version de l'app et type de build ;
- réseau, sortie audio, espace libre et état de la batterie ;
- étapes exactes, résultat attendu, résultat observé et fréquence ;
- capture vidéo et journal `adb logcat` si possible ;
- sévérité : P0 bloquant, P1 important, P2 cosmétique.

## Sortie de campagne

Archiver la matrice remplie, les anomalies résolues/acceptées, le SHA-256 du build signé testé et une installation finale depuis le canal Google Play de test interne. Le build publié doit être exactement celui validé par cette campagne.
