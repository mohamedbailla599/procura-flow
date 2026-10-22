# ProcuraFlow — Service Fournisseurs (`service-fournisseurs-mohamed`)

Microservice responsable de la **gestion du référentiel des fournisseurs** et de la **consultation de l'historique des commandes d'un fournisseur** au sein de la plateforme distribuée **ProcuraFlow**.

- **Responsable :** Mohamed Bailla
- **Stack technique :** Java 17/21, Spring Boot 3.3.5, Spring Web, Spring Data JPA (Hibernate), Jakarta Bean Validation, Spring Boot Actuator, PostgreSQL Driver, Springdoc OpenAPI 3 / Swagger UI, JUnit 5 & Mockito
- **Port par défaut :** `8081`
- **Base de données partagée :** Neon PostgreSQL 17 (`procura_flow`)
- **Schéma propriétaire :** `schema_fournisseurs` (table `schema_fournisseurs.fournisseurs`)

---

## 1. Périmètre fonctionnel et frontières du service

Le microservice **Fournisseurs** est l'unique propriétaire métier et technique du schéma `schema_fournisseurs` :

1. **Ce que fait ce service :**
   - Création d'un fournisseur avec validation stricte des entrées, normalisation de l'adresse e-mail (minuscules, suppression des espaces) et activation par défaut (`actif = true`).
   - Consultation de la liste des fournisseurs avec filtres paramétrés sécurisés (`actif`, `nom`, `email`, `includeInactive`).
   - Consultation d'un fournisseur par son identifiant (`id`), y compris s'il a été désactivé, afin de préserver l'intégrité des références historiques.
   - Mise à jour des informations modifiables d'un fournisseur (`nom`, `email`, `telephone`, `adresse`) avec mise à jour automatique de `updated_at`.
   - **Désactivation logique (soft deactivation)** d'un fournisseur (`actif = false`) sans suppression physique en base de données.
   - Récupération de l'historique des commandes d'un fournisseur via un appel REST sortant vers le microservice **Commandes** (`GET /api/commandes?fournisseurId={id}`).

2. **Ce que ce service ne fait jamais (respect strict des frontières) :**
   - N'accède jamais directement en SQL aux schémas des autres services (`schema_commandes`, `schema_receptions`, `schema_factures`).
   - N'introduit aucune clé étrangère SQL inter-schémas.
   - Ne crée ni ne modifie directement des commandes, réceptions ou factures.

---

## 2. Architecture et structure des packages

Le service suit une architecture en couches claire sous le package racine `com.procuraflow.fournisseurs` :

```text
service-fournisseurs-mohamed/
├── Dockerfile
├── pom.xml
├── mvnw / mvnw.cmd
├── .env.example
├── README.md
└── src/
    ├── main/
    │   ├── java/com/procuraflow/fournisseurs/
    │   │   ├── FournisseursApplication.java          # Point d'entrée Spring Boot
    │   │   ├── client/
    │   │   │   └── CommandesClient.java              # Client REST vers le service Commandes
    │   │   ├── config/
    │   │   │   ├── CommandesClientProperties.java    # Propriétés typées du client Commandes
    │   │   │   ├── DataSourceConfig.java             # Configuration HikariCP & schéma PostgreSQL
    │   │   │   ├── NeonJdbcUrlResolver.java          # Convertisseur d'URL Neon (postgresql:// -> jdbc:postgresql://)
    │   │   │   ├── OpenApiConfig.java                # Configuration Swagger / OpenAPI 3
    │   │   │   └── RestClientConfig.java             # Configuration RestClient avec timeouts HTTP
    │   │   ├── controller/
    │   │   │   └── FournisseurController.java        # Endpoints REST (/api/fournisseurs)
    │   │   ├── dto/
    │   │   │   ├── ApiErrorResponse.java             # Format d'erreur JSON standardisé
    │   │   │   ├── CommandeSummaryResponse.java      # DTO de commande reçu depuis Commandes
    │   │   │   ├── FournisseurRequest.java           # DTO d'entrée validé (POST / PUT)
    │   │   │   ├── FournisseurResponse.java          # DTO de sortie fournisseur
    │   │   │   └── LigneCommandeSummaryDto.java      # DTO de ligne de commande (optionnel)
    │   │   ├── entity/
    │   │   │   └── Fournisseur.java                  # Entité JPA mappée sur schema_fournisseurs.fournisseurs
    │   │   ├── exception/
    │   │   │   ├── DownstreamBadGatewayException.java
    │   │   │   ├── DownstreamServiceUnavailableException.java
    │   │   │   ├── DuplicateEmailException.java
    │   │   │   ├── FournisseurNotFoundException.java
    │   │   │   └── GlobalExceptionHandler.java       # Gestionnaire centralisé @RestControllerAdvice
    │   │   ├── repository/
    │   │   │   └── FournisseurRepository.java        # Repository Spring Data JPA + JpaSpecificationExecutor
    │   │   └── service/
    │   │       └── FournisseurService.java           # Logique métier et orchestration
    │   └── resources/
    │       ├── application.yml                       # Configuration principale
    │       └── db/migration/
    │           └── V1__init_schema_fournisseurs.sql  # Script SQL idempotent de référence
    └── test/
        ├── java/com/procuraflow/fournisseurs/
        │   ├── client/CommandesClientTest.java
        │   ├── config/NeonJdbcUrlResolverTest.java
        │   ├── controller/FournisseurControllerTest.java
        │   ├── integration/FournisseurIntegrationTest.java
        │   └── service/FournisseurServiceTest.java
        └── resources/
            ├── application-test.yml
            └── schema-test.sql
```

---

## 3. Modèle de données et configuration Neon PostgreSQL

Le service se connecte à la base **Neon PostgreSQL 17** existante (`procura_flow`, branche `main`) et utilise exclusivement la table `schema_fournisseurs.fournisseurs` :

| Colonne | Type SQL | Contraintes | Mapping JPA (`Fournisseur`) |
|---|---|---|---|
| `id` | `BIGINT` | `IDENTITY PRIMARY KEY` | `Long id` (`@GeneratedValue(IDENTITY)`) |
| `nom` | `VARCHAR(150)` | `NOT NULL`, `CHECK (length(btrim(nom)) > 0)` | `String nom` |
| `email` | `VARCHAR(150)` | `NOT NULL UNIQUE` | `String email` (normalisé en minuscules) |
| `telephone` | `VARCHAR(30)` | Optionnel (`NULL`) | `String telephone` |
| `adresse` | `TEXT` | Optionnel (`NULL`) | `String adresse` |
| `actif` | `BOOLEAN` | `NOT NULL DEFAULT TRUE` | `Boolean actif` |
| `created_at` | `TIMESTAMPTZ` | `NOT NULL DEFAULT NOW()` | `OffsetDateTime createdAt` (UTC) |
| `updated_at` | `TIMESTAMPTZ` | `NOT NULL DEFAULT NOW()` | `OffsetDateTime updatedAt` (UTC) |

### Règles de sécurité base de données
- Hibernate est configuré avec `spring.jpa.hibernate.ddl-auto=validate` et `hibernate.default_schema=schema_fournisseurs`.
- Le service ne supprime, ne tronque et ne recrée **jamais** les tables existantes.
- `NeonJdbcUrlResolver` accepte aussi bien une URL JDBC classique (`jdbc:postgresql://...`) qu'une URL Neon brute (`postgresql://user:pass@ep-...neon.tech/procura_flow?sslmode=require`), extrait les identifiants intégrés si `DATABASE_USERNAME` / `DATABASE_PASSWORD` ne sont pas définis séparément, et garantit la présence du paramètre `currentSchema=schema_fournisseurs`.

---

## 4. Variables d'environnement requises

Copiez `.env.example` vers `.env` (non versionné) ou configurez les variables suivantes dans votre environnement / IDE :

| Variable | Obligatoire | Valeur par défaut | Description |
|---|---|---|---|
| `DATABASE_URL` | **Oui** | — | URL de connexion PostgreSQL / Neon (`jdbc:postgresql://...` ou `postgresql://...`) |
| `DATABASE_USERNAME` | Oui (sauf si inclus dans l'URI) | — | Utilisateur PostgreSQL Neon |
| `DATABASE_PASSWORD` | Oui (sauf si inclus dans l'URI) | — | Mot de passe PostgreSQL Neon |
| `DATABASE_SCHEMA` | Non | `schema_fournisseurs` | Schéma PostgreSQL propriétaire du service |
| `JPA_DDL_AUTO` | Non | `validate` | Stratégie Hibernate (`validate` recommandé sur Neon) |
| `SERVER_PORT` | Non | `8081` | Port HTTP d'écoute du microservice |
| `COMMANDES_SERVICE_URL` | Non | `http://localhost:8082` | URL de base du microservice Commandes (`http://service-commandes:8082` sous Docker Compose) |
| `COMMANDES_CONNECT_TIMEOUT_MS` | Non | `3000` | Timeout de connexion HTTP vers Commandes (ms) |
| `COMMANDES_READ_TIMEOUT_MS` | Non | `5000` | Timeout de lecture HTTP vers Commandes (ms) |

---

## 5. Contrat REST API détaillé

### 5.1 Créer un fournisseur — `POST /api/fournisseurs`

**Requête (`application/json`) :**
```json
{
  "nom": "Atlas Fournitures",
  "email": "contact@atlas.example",
  "telephone": "+212600000000",
  "adresse": "Agadir, Maroc"
}
```

**Réponse `201 Created` (avec en-tête `Location: /api/fournisseurs/12`) :**
```json
{
  "id": 12,
  "nom": "Atlas Fournitures",
  "email": "contact@atlas.example",
  "telephone": "+212600000000",
  "adresse": "Agadir, Maroc",
  "actif": true,
  "createdAt": "2026-10-21T13:30:00Z",
  "updatedAt": "2026-10-21T13:30:00Z"
}
```

- **Codes d'erreur :** `400 Bad Request` (champ manquant, nom vide ou e-mail mal formé), `409 Conflict` (adresse e-mail déjà existante).

### 5.2 Lister et filtrer les fournisseurs — `GET /api/fournisseurs`

**Paramètres de requête optionnels :**
- `actif` (`true` | `false`) : filtre explicitement sur le statut actif/inactif.
- `nom` (`string`) : recherche partielle insensible à la casse sur le nom.
- `email` (`string`) : recherche partielle insensible à la casse sur l'adresse e-mail.
- `includeInactive` (`true` | `false`, défaut `false`) : lorsque `actif` n'est pas fourni, `GET /api/fournisseurs` retourne uniquement les fournisseurs actifs par défaut (conformément à la section 5.4 du contrat d'intégration). Passer `?includeInactive=true` retourne tous les fournisseurs (actifs et inactifs).

**Réponse `200 OK` :** Tableau JSON `FournisseurResponse[]`.

### 5.3 Consulter un fournisseur par ID — `GET /api/fournisseurs/{id}`

- Retourne `200 OK` avec la représentation du fournisseur (même si `"actif": false`, afin que le service Commandes et l'historique des achats puissent vérifier l'état du fournisseur).
- Retourne `404 Not Found` si l'identifiant n'existe pas.

### 5.4 Mettre à jour un fournisseur — `PUT /api/fournisseurs/{id}`

**Requête (`application/json`) :**
```json
{
  "nom": "Atlas Fournitures Maroc",
  "email": "contact@atlas.example",
  "telephone": "+212528840000",
  "adresse": "Zone Industrielle Tassila, Agadir"
}
```

- Met à jour `nom`, `email`, `telephone`, `adresse` et rafraîchit `updatedAt`.
- Préserve `id`, `createdAt` et `actif`.
- **Codes de réponse :** `200 OK`, `400 Bad Request`, `404 Not Found`, `409 Conflict`.

### 5.5 Désactiver logiquement un fournisseur (Soft Deactivation)

Afin d'assurer une compatibilité totale entre la proposition du contrat d'intégration (`DELETE /api/fournisseurs/{id}`) et la convention REST explicite (`PATCH /api/fournisseurs/{id}/desactiver`), **les deux endpoints sont supportés** et exécutent exactement la même désactivation logique (`actif = false`, aucune suppression physique) :

1. **`PATCH /api/fournisseurs/{id}/desactiver`**
   - Passe `actif` à `false`, met à jour `updatedAt`, et retourne **`200 OK`** avec l'objet `FournisseurResponse` mis à jour.
2. **`DELETE /api/fournisseurs/{id}`**
   - Passe `actif` à `false`, conserve l'enregistrement en base de données, et retourne **`204 No Content`** (conformément à la section 5.1 du contrat d'intégration).
3. **`PATCH /api/fournisseurs/{id}/reactiver`** *(extension proposée pour la décision ouverte O4)*
   - Réactive un fournisseur désactivé (`actif = true`) et retourne **`200 OK`** avec l'objet `FournisseurResponse`.

### 5.6 Historique des commandes d'un fournisseur — `GET /api/fournisseurs/{id}/commandes`

1. Vérifie d'abord dans `schema_fournisseurs.fournisseurs` que le fournisseur `{id}` existe (sinon retourne `404 Not Found`).
2. Appelle le microservice **Commandes** via REST :
   ```http
   GET ${COMMANDES_SERVICE_URL}/api/commandes?fournisseurId={id}
   ```
3. **Réponse `200 OK` :**
   ```json
   [
     {
       "id": 101,
       "fournisseurId": 12,
       "dateCommande": "2026-10-15T10:30:00Z",
       "statut": "VALIDEE",
       "montantTotal": 450.00
     }
   ]
   ```
4. **Gestion des pannes aval :**
   - Si le service Commandes est éteint, injoignable ou dépasse le délai d'attente (timeout) → retourne **`503 Service Unavailable`** avec un message contrôlé.
   - Si le service Commandes renvoie une réponse invalide ou une erreur serveur interne → retourne **`502 Bad Gateway`**.

### 5.7 Format standardisé des erreurs

Toutes les erreurs retournent un objet JSON conforme à la section 3.3 du contrat d'intégration (sans stack trace, ni requête SQL, ni nom d'hôte interne) :

```json
{
  "timestamp": "2026-10-21T13:30:00Z",
  "status": 404,
  "error": "NOT_FOUND",
  "message": "Fournisseur introuvable avec l'identifiant : 42",
  "path": "/api/fournisseurs/42"
}
```

Lors d'une erreur de validation (`400 BAD_REQUEST`), un champ additionnel `validationErrors` détaille les champs en erreur.

---

## 6. Commandes d'exécution, de test et de conteneurisation

### 6.1 Exécuter les tests automatisés

Les tests unitaires et d'intégration s'exécutent de manière totalement hermétique (sans dépendre de la base Neon de production ni nécessiter d'identifiants réels) :

```bash
cd service-fournisseurs-mohamed
./mvnw clean test
```

### 6.2 Compiler et générer le JAR exécutable

```bash
cd service-fournisseurs-mohamed
./mvnw clean package
```

### 6.3 Démarrer le service localement avec Neon PostgreSQL

Configurez vos variables d'environnement locales (sans les commiter dans Git), puis lancez l'application :

```bash
cd service-fournisseurs-mohamed
export DATABASE_URL="jdbc:postgresql://<votre-hote-neon>:5432/procura_flow?sslmode=require&currentSchema=schema_fournisseurs"
export DATABASE_USERNAME="<votre-utilisateur-neon>"
export DATABASE_PASSWORD="<votre-mot-de-passe-neon>"
export SERVER_PORT=8081
export COMMANDES_SERVICE_URL="http://localhost:8082"

./mvnw spring-boot:run
```

### 6.4 Vérifier l'endpoint de santé et Swagger UI

Une fois le service démarré :

- **Health Check Actuator :**
  ```bash
  curl -i http://localhost:8081/actuator/health
  ```
- **Documentation interactive Swagger UI :**
  Ouvrez `http://localhost:8081/swagger-ui.html` (ou `http://localhost:8081/v3/api-docs` pour la spécification OpenAPI JSON).

### 6.5 Construire et exécuter avec Docker

**Construire l'image Docker :**
```bash
cd service-fournisseurs-mohamed
docker build -t procuraflow/service-fournisseurs:1.0.0 .
```

**Lancer le conteneur Docker avec vos variables Neon :**
```bash
docker run --rm -p 8081:8081 \
  -e SERVER_PORT=8081 \
  -e DATABASE_URL="jdbc:postgresql://<votre-hote-neon>:5432/procura_flow?sslmode=require&currentSchema=schema_fournisseurs" \
  -e DATABASE_USERNAME="<votre-utilisateur-neon>" \
  -e DATABASE_PASSWORD="<votre-mot-de-passe-neon>" \
  -e COMMANDES_SERVICE_URL="http://host.docker.internal:8082" \
  procuraflow/service-fournisseurs:1.0.0
```

**Ou via le fichier `docker-compose.yml` à la racine du projet :**
```bash
# Après avoir renseigné le fichier .env à la racine du dépôt
docker compose up --build service-fournisseurs
```

---

## 7. Dépannage (Troubleshooting)

1. **`Missing required database configuration: set DATABASE_URL` au démarrage :**
   - Vérifiez que la variable d'environnement `DATABASE_URL` est bien exportée dans votre terminal ou configurée dans la configuration d'exécution de votre IDE (IntelliJ / Eclipse / VS Code).
2. **Erreur SSL de connexion à Neon (`SSL error` ou `connection refused`) :**
   - Assurez-vous que votre URL Neon contient bien `?sslmode=require`.
   - `NeonJdbcUrlResolver` accepte directement l'URL copiée depuis la console Neon (`postgresql://user:pass@ep-...neon.tech/procura_flow?sslmode=require`).
3. **Erreur Hibernate `Schema-validation: missing table [fournisseurs]` :**
   - Vérifiez que vous ciblez bien la base `procura_flow` (branche `main`) et que `DATABASE_SCHEMA` vaut `schema_fournisseurs`.
4. **Réponse `503 Service Unavailable` sur `GET /api/fournisseurs/{id}/commandes` :**
   - Ce comportement est **normal** lorsque le microservice `Commandes` (port `8082`) n'est pas démarré. Tous les autres endpoints de gestion des fournisseurs continuent de fonctionner normalement.

---

## 8. Hypothèses et décisions ouvertes soumises à validation d'équipe

Conformément à la section 12 du contrat d'intégration (`integration-contract.md`), les points suivants ont été implémentés et doivent être confirmés avec Yassine, Ali et Hamza :

- **O1 / Désactivation logique :** Le service supporte à la fois `DELETE /api/fournisseurs/{id}` (`204 No Content`, proposé dans la section 5.1 du contrat) et `PATCH /api/fournisseurs/{id}/desactiver` (`200 OK` avec le fournisseur mis à jour).
- **O2 / Type d'ID et horodatage :** Les identifiants sont mappés en `Long` (`BIGINT` en base Neon) et sérialisés en nombres JSON ; les dates `createdAt` et `updatedAt` sont sérialisées en ISO-8601 UTC (`yyyy-MM-dd'T'HH:mm:ssXXX`, ex. `2026-10-21T13:30:00Z`).
- **O3 / Mapping des erreurs aval :** Une indisponibilité ou un timeout du service Commandes retourne `503 Service Unavailable` ; une réponse mal formée ou une erreur 500 aval retourne `502 Bad Gateway` ; un `404` aval sur la liste des commandes retourne une liste vide `[]`.
- **O4 / Réactivation et filtrage des inactifs :** `GET /api/fournisseurs` exclut les fournisseurs inactifs par défaut mais accepte `?actif=false` ou `?includeInactive=true`. Un endpoint `PATCH /api/fournisseurs/{id}/reactiver` est fourni en extension.
- **O12 / Variables d'environnement :** Le service Fournisseurs utilise exclusivement les noms canoniques `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`, `SERVER_PORT` et `COMMANDES_SERVICE_URL`.
