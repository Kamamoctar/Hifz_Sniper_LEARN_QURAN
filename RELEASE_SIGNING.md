# Signature de publication

La configuration Gradle signe automatiquement la variante `release` si un fichier privé `keystore.properties` complet existe à la racine. Sans ce fichier, `assembleRelease` produit volontairement un APK non signé.

1. Créer ou conserver une clé d’upload Android dans un emplacement sauvegardé et privé.
2. Copier `keystore.properties.example` vers `keystore.properties`.
3. Renseigner le chemin absolu, l’alias et les mots de passe.
4. Exécuter `./gradlew assembleRelease` ou `./gradlew bundleRelease`.
5. Vérifier l'APK avec `apksigner verify --verbose <apk>` et l'AAB avec `jarsigner -verify -verbose -certs <aab>`.
6. Pour Google Play, conserver la clé d'upload hors du projet et activer Play App Signing ; ne jamais réutiliser une clé de débogage.

`keystore.properties`, `*.jks` et `*.keystore` sont ignorés. Ne jamais placer leurs mots de passe dans le dépôt, les livrables source ou une capture d’écran.
