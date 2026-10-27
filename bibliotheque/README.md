# Gestion de bibliothèque — Java + MariaDB/MySQL

Application de gestion de bibliothèque avec :

- une **API REST** Spring Boot (Java 17) ;
- une **interface web moderne** (dégradé, responsive, mode sombre) avec un **tableau de bord** ;
- une **version console** Java (JDBC) ;
- une base **MariaDB/MySQL** (XAMPP sous Windows, MariaDB sous Termux).

## 1. Fonctionnalités

| Domaine | Opérations |
|---|---|
| Livres | ajouter, modifier, supprimer, lister, rechercher (titre/auteur), filtrer les disponibles |
| Adhérents | ajouter, modifier, supprimer, lister, rechercher (nom/téléphone) |
| Emprunts | emprunter (max. 3 livres par adhérent), retourner, lister les emprunts en cours |
| Tableau de bord | nombre de livres, livres disponibles, adhérents, emprunts en cours, retours, taux de disponibilité, derniers emprunts |

Règles de gestion : un livre emprunté ne peut pas être supprimé ; un livre ou un adhérent ayant un historique d'emprunts ne peut pas être supprimé (l'historique est conservé).

## 2. Architecture

```
Navigateur ──HTTP/JSON──► API Spring Boot ──JDBC──► MariaDB/MySQL (base "bibliotheque")
                                                        ▲
Console Java ───────────────────JDBC────────────────────┘
```

```
bibliotheque-final/
├── database/bibliotheque.sql          Script SQL (base, tables, données de test)
├── api-spring/                        API + interface web
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/bibliotheque/     entity / repository / service / controller
│       └── resources/
│           ├── application.properties
│           └── static/                Interface web (index.html, css/, js/)
├── console-jdbc/                      Version console
│   ├── lib/                           ← y placer mariadb-java-client-2.7.6.jar
│   ├── .vscode/settings.json          Configuration VS Code
│   └── src/                           Main, ConnexionBD, *Service, Livre, Adherent, Emprunt
├── tests/smoke-test.sh                Test de bout en bout de l'API
└── README.md
```

## 3. Configuration de la connexion (variables d'environnement)

Par défaut : base `bibliotheque` sur `localhost:3306`, utilisateur `root`, **sans** mot de passe (XAMPP). Pour changer, définir avant de lancer :

| Variable | Valeur par défaut |
|---|---|
| `DB_URL` | `jdbc:mariadb://localhost:3306/bibliotheque` |
| `DB_USER` | `root` |
| `DB_PASSWORD` | *(vide)* |
| `SERVER_PORT` | `8080` (API uniquement) |

## 4. Pilote de base de données

- **API (Spring Boot)** : le pilote MariaDB est dans `pom.xml`, Maven le télécharge automatiquement. Rien à installer à la main.
- **Console** : utilise `mariadb-java-client-2.7.6.jar` à placer dans `console-jdbc/lib/`.

> Le pilote « MySQL Connector/J » (versions 8.0.33 à 8.2.0) est incompatible avec MariaDB (erreur `Unknown system variable 'transaction_isolation'`). C'est pourquoi tout le projet utilise le pilote MariaDB.

## 5. Lancement sous Windows (XAMPP + VS Code)

**Prérequis :** XAMPP, JDK 17 ou plus, Maven (`mvn -v` doit fonctionner), VS Code avec « Extension Pack for Java ».

1. **Démarrer MySQL** dans le panneau XAMPP (bouton *Start* de MySQL, port 3306).
2. **Importer la base** (au choix) :
   - invite de commandes, dans le dossier du projet :
     `C:\xampp\mysql\bin\mysql.exe -u root < database\bibliotheque.sql`
   - ou phpMyAdmin (`http://localhost/phpmyadmin`) → onglet *Importer* → choisir `database\bibliotheque.sql`.
3. **Lancer l'API et l'interface web** :
   ```
   cd api-spring
   mvn spring-boot:run
   ```
   Ouvrir **http://localhost:8080** (tableau de bord).
4. **Lancer la console (facultatif)** :
   - copier `mariadb-java-client-2.7.6.jar` dans `console-jdbc\lib\` ;
   - ouvrir le dossier `console-jdbc` dans VS Code, ouvrir `src/Main.java`, cliquer sur *Run* ; ou en ligne de commande :
     ```
     cd console-jdbc
     chcp 65001
     javac -encoding UTF-8 -cp "lib/*" -d out src\*.java
     java -cp "out;lib/*" Main
     ```
     (`chcp 65001` affiche correctement les accents.)

## 6. Lancement sous Termux (Android)

1. **Installer les outils** :
   ```
   pkg update
   pkg install openjdk-17 maven mariadb unzip
   termux-setup-storage
   ```
2. **Décompresser le projet** (ex. zip dans Téléchargements) :
   ```
   cd ~
   unzip ~/storage/downloads/bibliotheque-final.zip
   cd bibliotheque-final
   ```
3. **Démarrer MariaDB** (laisser tourner en arrière-plan) :
   ```
   mysqld_safe &
   ```
   Au tout premier usage, si le démarrage échoue : `mysql_install_db` puis relancer `mysqld_safe &`.
4. **Importer la base et créer un utilisateur dédié** :
   ```
   mysql -u root < database/bibliotheque.sql
   mysql -u root -e "CREATE USER 'biblio'@'localhost' IDENTIFIED BY 'biblio123'; CREATE USER 'biblio'@'127.0.0.1' IDENTIFIED BY 'biblio123'; GRANT ALL ON bibliotheque.* TO 'biblio'@'localhost'; GRANT ALL ON bibliotheque.* TO 'biblio'@'127.0.0.1'; FLUSH PRIVILEGES;"
   ```
   (Si `mysql -u root` est refusé, essayer `mysql -u $(whoami)`.)
5. **Lancer l'API** :
   ```
   export DB_USER=biblio
   export DB_PASSWORD=biblio123
   cd api-spring
   mvn spring-boot:run
   ```
   Le premier lancement télécharge les dépendances (connexion Internet requise, quelques minutes). Ouvrir le navigateur du téléphone sur **http://localhost:8080**.
6. **Console (facultatif)**, dans un second onglet Termux :
   ```
   export DB_USER=biblio DB_PASSWORD=biblio123
   cd ~/bibliotheque-final/console-jdbc
   mkdir -p out
   cp ~/storage/downloads/mariadb-java-client-2.7.6.jar lib/
   javac -encoding UTF-8 -cp "lib/*" -d out src/*.java
   java -cp "out:lib/*" Main
   ```
   Sous Termux le séparateur de classpath est `:` (et `;` sous Windows).

## 7. Routes de l'API

| Méthode | Route | Rôle |
|---|---|---|
| GET | `/api/livres?recherche=&disponible=true` | lister / rechercher / filtrer |
| GET, PUT, DELETE | `/api/livres/{id}` | consulter / modifier / supprimer |
| POST | `/api/livres` | ajouter (`{"titre":"…","auteur":"…"}`) |
| GET | `/api/adherents?recherche=` | lister / rechercher |
| GET, PUT, DELETE | `/api/adherents/{id}` | consulter / modifier / supprimer |
| POST | `/api/adherents` | ajouter (`{"nom":"…","telephone":"…"}`) |
| GET | `/api/emprunts?enCours=true` | lister les emprunts |
| POST | `/api/emprunts` | emprunter (`{"livreId":1,"adherentId":1}`) |
| POST | `/api/retours` | retourner (`{"livreId":1}`) |

Codes : 200/201/204 succès, 400 données invalides, 404 introuvable, 409 règle de gestion non respectée.

## 8. Tester

Avec l'API démarrée :
```
bash tests/smoke-test.sh
```
(Git Bash sous Windows, Termux, Linux.) Le script vérifie ajout, modification, recherche, suppression, emprunt et retour pour les livres et les adhérents. Il laisse 1 livre et 1 adhérent de test dans la base.

## 9. Dépannage

| Problème | Solution |
|---|---|
| `Port 8080 was already in use` | arrêter l'autre programme, ou `SERVER_PORT=8081` avant de lancer |
| `Communications link failure` / connexion refusée | MySQL/MariaDB n'est pas démarré, ou port 3306 occupé |
| `Access denied for user` | vérifier `DB_USER` / `DB_PASSWORD` (Termux : utilisateur `biblio`) |
| `Unknown database 'bibliotheque'` | importer `database/bibliotheque.sql` |
| Console : « Pilote MariaDB introuvable » | placer `mariadb-java-client-2.7.6.jar` dans `console-jdbc/lib/` |
| Interface : « Impossible de joindre l'API » | l'API Spring Boot n'est pas lancée |
| Accents illisibles dans la console Windows | `chcp 65001` avant de lancer |
