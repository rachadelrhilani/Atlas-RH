# 🏢 HR Management API — Système de Gestion des Ressources Humaines

API REST moderne et robuste conçue avec **Spring Boot 3** et **Java 17**, dédiée à la gestion des ressources humaines, des contrats de travail, de la structure organisationnelle et du cycle complet des demandes de congés (workflow collaborateur / manager).

---

## 📋 Table des matières

- [Aperçu et Fonctionnalités](#-aperçu-et-fonctionnalités)
- [Stack Technique](#-stack-technique)
- [Architecture du Projet](#-architecture-du-projet)
- [Prérequis](#-prérequis)
- [Installation et Démarrage](#-installation-et-démarrage)
  - [Option 1 : Lancement complet avec Docker Compose (Recommandé)](#option-1--lancement-complet-avec-docker-compose-recommandé)
  - [Option 2 : Exécution locale avec Maven](#option-2--exécution-locale-avec-maven)
- [Configuration et Variables d'Environnement](#-configuration-et-variables-denvironnement)
- [Documentation de l'API (Swagger / OpenAPI)](#-documentation-de-lapi-swagger--openapi)
- [Récapitulatif des Endpoints](#-récapitulatif-des-endpoints)
  - [1. Départements](#1-départements-apiv1departments)
  - [2. Employés](#2-employés-apiv1employees)
  - [3. Contrats](#3-contrats-apiv1contracts)
  - [4. Espace Manager](#4-espace-manager-apiv1managermanagerid)
  - [5. Espace Collaborateur / Portail Employé](#5-espace-collaborateur--portail-employé-apiv1employee-portal)
- [Jeu de Données Initial (Seed Data)](#-jeu-de-données-initial-seed-data)
- [Tests](#-tests)

---

## 🚀 Aperçu et Fonctionnalités

### 🏢 1. Organisation & Départements
- **Gestion des départements** : Création, consultation, modification et pagination.
- **Règles métier** : Unicité du nom du département et interdiction de supprimer un département contenant encore des employés actifs.

### 👤 2. Gestion des Collaborateurs
- **Fiches employés complètes** : Données personnelles, matricule unique, e-mail professionnel unique, rôle (`EMPLOYE`, `MANAGER`, `RH`), poste, date d'embauche et département de rattachement.
- **Gestion hiérarchique** : Rattachement à un manager direct (avec validation empêchant l'auto-management).
- **Recherche avancée** : Filtrage multicritère (nom/prénom partiel, département) avec support complet de la pagination et du tri.
- **Équipes** : Consultation directe de l'ensemble des membres d'une équipe managée.

### 📄 3. Gestion des Contrats de Travail
- **Types de contrats pris en charge** : `CDI`, `CDD`, `STAGE`.
- **Contrôles stricts** : 
  - Un employé ne peut avoir qu'un seul contrat actif à la fois.
  - La date de fin est obligatoire pour les contrats temporaires (`CDD`, `STAGE`).
- **Cycle de vie** : Résiliation / clôture anticipée d'un contrat en cours et historique complet des contrats par collaborateur.

### 🏖️ 4. Gestion des Congés & Workflow d'Approbation
- **Types de congés configurables** : Congés Payés (22 j/an par défaut), Congé Maladie (10 j/an par défaut), etc.
- **Espace Employé** :
  - Dépôt de demande de congé avec contrôle de solde suffisant et vérification de non-chevauchement avec d'autres congés.
  - Annulation de ses propres demandes tant qu'elles sont au statut `EN_ATTENTE`.
  - Consultation en temps réel du solde de congés (alloués, consommés sur l'année en cours, restants).
  - Calcul automatique de l'ancienneté (en années et mois) depuis la date d'embauche.
- **Espace Manager** :
  - Consultation des demandes en attente déposées par les membres de son équipe.
  - Décision d'approbation (`APPROUVEE`) ou de refus (`REFUSEE`) assortie d'un commentaire justificatif.

### 🛡️ 5. Résilience & Qualité
- **Validation déclarative** : Utilisation de Jakarta Bean Validation (`@Valid`, contraintes de format, dates cohérentes).
- **Gestion centralisée des exceptions** : `GlobalExceptionHandler` standardisant les réponses d'erreur (`400 Bad Request`, `404 Not Found`, `409 Conflict`, `500 Internal Error`).
- **Migrations de schéma** : Évolution de la base de données versionnée et automatisée via **Liquibase**.

---

## 🛠 Stack Technique

| Domaine | Technologie / Outil | Version |
| :--- | :--- | :--- |
| **Langage** | Java | 17 |
| **Framework** | Spring Boot | 3.2.4 |
| **Persistance** | Spring Data JPA / Hibernate | 3.2.4 |
| **Base de données** | MySQL | 8.0 |
| **Migration BDD** | Liquibase | Core |
| **Mapping DTO/Entités** | MapStruct | 1.5.5.Final |
| **Productivité** | Lombok | 1.18.30 |
| **Documentation API** | SpringDoc OpenAPI (Swagger UI) | 2.5.0 |
| **Supervision & Healthcheck** | Spring Boot Actuator | 3.2.4 |
| **Conteneurisation** | Docker & Docker Compose | Multi-stage build |

---

## 📂 Architecture du Projet

Le projet adopte une architecture en couches (N-Tiers) standard Spring Boot :

```text
hr-management/
├── Dockerfile                             # Image conteneur multi-stage (Maven build -> Alpine JRE)
├── compose.yaml                           # Orchestration Docker (App + Base MySQL)
├── pom.xml                                # Dépendances Maven et configuration des plugins
├── src/
│   ├── main/
│   │   ├── java/com/wegrow/hr_management/
│   │   │   ├── HrManagementApplication.java  # Point d'entrée de l'application
│   │   │   ├── controller/               # Contrôleurs REST (Endpoints API)
│   │   │   ├── dto/
│   │   │   │   ├── request/              # DTOs entrants validés (@Valid)
│   │   │   │   └── response/             # DTOs sortants sérialisés
│   │   │   ├── exception/                # Exceptions personnalisées et GlobalExceptionHandler
│   │   │   ├── mapper/                   # Interfaces MapStruct (Entité <-> DTO)
│   │   │   ├── model/                    # Entités JPA et Énumérations
│   │   │   ├── repository/               # Repositories Spring Data JPA
│   │   │   └── service/                  # Interfaces et implémentations de la logique métier
│   │   └── resources/
│   │       ├── application.yaml          # Configuration de l'application
│   │       └── db/changelog/             # Scripts de migration Liquibase
│   │           ├── db.changelog-master.yaml
│   │           └── changes/
│   │               ├── 001-initial-schema.yaml # Définition des tables et contraintes
│   │               └── 002-demo-data.yaml      # Données initiales de test
│   └── test/                             # Tests unitaires et d'intégration
```

---

## ⚙️ Prérequis

- **Java JDK 17** ou version supérieure installée.
- **Maven 3.8+** (facultatif si vous utilisez le wrapper `./mvnw` inclus).
- **Docker** et **Docker Compose** (recommandé pour un déploiement rapide).

---

## 🚀 Installation et Démarrage

### Option 1 : Lancement complet avec Docker Compose (Recommandé)

Cette option instancie automatiquement le serveur de base de données MySQL 8.0, applique les migrations Liquibase et démarre l'application Spring Boot dans des conteneurs isolés :

```bash
# 1. Cloner ou se positionner dans le répertoire du projet
cd hr-management

# 2. Lancer les services en arrière-plan
docker compose up --build -d

# 3. Vérifier l'état des conteneurs
docker compose ps

# 4. Consulter les logs de l'application
docker compose logs -f app
```

L'application sera accessible sur : `http://localhost:8080`.  
La base de données MySQL sera exposée sur l'hôte sur le port `3308`.

Pour arrêter les conteneurs :
```bash
docker compose down
```

---

### Option 2 : Exécution locale avec Maven

Si vous souhaitez exécuter l'application en local (pour le développement ou le débogage) :

1. **Démarrer uniquement la base de données MySQL via Docker** :
   ```bash
   docker compose up -d db
   ```
   *(La base sera disponible sur `localhost:3308` avec l'utilisateur `hr_user` et le mot de passe `hr_password`).*

2. **Compiler et exécuter l'application** :
   - Sous Linux / macOS :
     ```bash
     ./mvnw spring-boot:run
     ```
   - Sous Windows (PowerShell / CMD) :
     ```cmd
     mvnw.cmd spring-boot:run
     ```

---

## 🔧 Configuration et Variables d'Environnement

L'application est configurable via `application.yaml` ou par injection de variables d'environnement :

| Variable | Description | Valeur par défaut |
| :--- | :--- | :--- |
| `DB_URL` | URL JDBC de connexion à MySQL | `jdbc:mysql://localhost:3308/hr_db?...` |
| `DB_USERNAME` | Nom d'utilisateur de la base de données | `hr_user` |
| `DB_PASSWORD` | Mot de passe de la base de données | `hr_password` |
| `DB_PORT` | Port exposé pour MySQL sur la machine hôte | `3308` |
| `APP_PORT` | Port d'écoute HTTP de l'API REST | `8080` |
| `MYSQL_ROOT_PASSWORD` | Mot de passe root du conteneur MySQL | `rootpassword` |
| `MYSQL_DATABASE` | Nom de la base de données créée | `hr_db` |

---

## 📖 Documentation de l'API (Swagger / OpenAPI)

Une fois l'application démarrée, l'interface interactive Swagger UI permet d'explorer et de tester directement l'ensemble des endpoints :

- **Interface Swagger UI** : [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **Spécification OpenAPI (JSON)** : [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)
- **Sonde de santé Actuator** : [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health)

---

## 📡 Récapitulatif des Endpoints

Tous les endpoints sont préfixés par `/api/v1`.

### 1. Départements (`/api/v1/departments`)
| Méthode | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/api/v1/departments` | Liste paginée de tous les départements |
| `GET` | `/api/v1/departments/{id}` | Détails d'un département par son ID |
| `POST` | `/api/v1/departments` | Créer un nouveau département (nom unique) |
| `PUT` | `/api/v1/departments/{id}` | Mettre à jour un département existant |
| `DELETE` | `/api/v1/departments/{id}` | Supprimer un département (interdit s'il contient des employés) |

### 2. Employés (`/api/v1/employees`)
| Méthode | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/api/v1/employees` | Recherche paginée avec filtres optionnels (`name`, `departmentId`) |
| `GET` | `/api/v1/employees/{id}` | Consulter la fiche complète d'un employé |
| `GET` | `/api/v1/employees/manager/{managerId}/team` | Consulter tous les collaborateurs rattachés à un manager |
| `POST` | `/api/v1/employees` | Enregistrer un nouvel employé (matricule et e-mail uniques) |
| `PUT` | `/api/v1/employees/{id}` | Modifier la fiche d'un employé |

### 3. Contrats (`/api/v1/contracts`)
| Méthode | Endpoint | Description |
| :--- | :--- | :--- |
| `POST` | `/api/v1/contracts` | Créer un contrat (`CDI`, `CDD`, `STAGE`) |
| `PUT` | `/api/v1/contracts/{id}/terminate` | Clôturer / résilier un contrat actif |
| `GET` | `/api/v1/contracts/employee/{employeeId}` | Consulter l'historique des contrats d'un collaborateur |

### 4. Espace Manager (`/api/v1/manager/{managerId}`)
| Méthode | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/api/v1/manager/{managerId}/pending-requests` | Lister les demandes de congés en attente pour son équipe |
| `PUT` | `/api/v1/manager/{managerId}/leave-requests/{requestId}/decision` | Approuver ou refuser une demande (`APPROUVEE` / `REFUSEE`) avec commentaire |

### 5. Espace Collaborateur / Portail Employé (`/api/v1/employee-portal`)
| Méthode | Endpoint | Description |
| :--- | :--- | :--- |
| `POST` | `/api/v1/employee-portal/leave-requests` | Déposer une nouvelle demande de congé |
| `GET` | `/api/v1/employee-portal/{employeeId}/leave-requests` | Consulter l'historique de ses demandes (filtre optionnel par `status`) |
| `PUT` | `/api/v1/employee-portal/{employeeId}/leave-requests/{requestId}/cancel` | Annuler une demande en attente (`EN_ATTENTE` -> `ANNULEE`) |
| `GET` | `/api/v1/employee-portal/{employeeId}/balances` | Consulter ses soldes de congés (alloués, consommés, restants) |
| `GET` | `/api/v1/employee-portal/{employeeId}/seniority` | Consulter son ancienneté précise (années et mois) |

---

## 🧪 Jeu de Données Initial (Seed Data)

Au premier lancement, le changelog Liquibase `002-demo-data.yaml` charge automatiquement un jeu d'essai prêt à l'emploi :

- **Départements** :
  - `ID: 1` : *Ingénierie & Développement*
  - `ID: 2` : *Ressources Humaines*
- **Types de Congés** :
  - *Congé Payé* : 22 jours / an
  - *Congé Maladie* : 10 jours / an
- **Collaborateurs** :
  - `ID: 1` — **Karim El Amrani** (`EMP001`), Rôle : `MANAGER`, Poste : *Engineering Manager*
  - `ID: 2` — **Youssef Bennani** (`EMP002`), Rôle : `EMPLOYE`, Poste : *Développeur FullStack*, rattaché au manager `ID: 1`
- **Contrat initial** :
  - Contrat `CDI` actif associé à l'employé `ID: 2`.

---

## 🧪 Tests

Pour exécuter la suite de tests automatisés :

```bash
# Lancement des tests avec Maven
./mvnw clean test

# Sous Windows :
mvnw.cmd clean test
```
