# ProcuraFlow — Integration Contract

**Version:** 0.2 (draft for team review)  
**Status:** Proposed decisions; not yet approved by all four service owners  
**Project:** Gestion des fournisseurs et des achats  
**Last updated:** 2026-10-21

> **Purpose:** This document defines the proposed REST interfaces and integration rules for the four ProcuraFlow services. It is a shared starting point for implementation, not a replacement for the teacher's assignment. Requirements explicitly stated by the teacher take precedence. Items marked **OPEN** require a team decision; items marked **PROPOSED** are recommendations to review.

---

## 1. Project scope and constraints

ProcuraFlow is an academic collaborative project with four independently owned services:

1. **Fournisseurs** — Mohamed Bailla
2. **Commandes** — Yassine
3. **Réceptions** — Ali
4. **Factures** — Hamza

### Requirements provided for the project

- Services communicate through **REST**.
- Each module exposes at least one web service and consumes a web service from another module.
- Each module has its own `Dockerfile`.
- A root-level Docker Compose file starts the application modules together.
- API interfaces are documented and tested.
- The team plans to use **Neon PostgreSQL** as the shared hosted database.
- APIs exchange **JSON**.
- Postman is used for testing.

The project brief/scoping notes should be consulted if any requirement in this draft appears inconsistent with the assignment.

### Team's current technology plan

| Service | Owner | Proposed technology | Container port |
|---|---|---|---:|
| Fournisseurs | Mohamed | Java / Spring Boot | `8081` |
| Commandes | Yassine | Java / Spring Boot | `8082` |
| Réceptions | Ali | Java / Spring Boot | `8083` |
| Factures | Hamza | Node.js (framework **OPEN**) | `8084` |

**PROPOSED:** Use the same basic REST conventions across all services even though Factures uses a different runtime. The HTTP/JSON contract, not the programming language, is the integration boundary.

---

## 2. Architecture and ownership

### 2.1 Service boundaries

Each service owns its business logic and its database schema. With one Neon PostgreSQL database, the proposed schema names are:

- `schema_fournisseurs`
- `schema_commandes`
- `schema_receptions`
- `schema_factures`

**PROPOSED ownership rule:** each service is the only service allowed to write to its own schema. Other services must use the owning service's REST API to read or validate that service's data; they must not query or update another service's tables directly.

This is a team architecture decision, not a confirmed teacher requirement.

### 2.2 High-level interactions

```text
Postman / future client
       |
       +--> Fournisseurs (8081) ---- REST ----> Commandes (8082)
       |                                      |
       |                                      +---- REST ----> Fournisseurs (8081)
       |
       +--> Réceptions (8083) -------- REST --> Commandes (8082)
       |
       +--> Factures (8084) ---------- REST --> Commandes (8082)
                                \------ REST --> Réceptions (8083)

All services connect to the shared Neon PostgreSQL database,
but each service owns its schema and writes only to that schema.
```

**PROPOSED:** the arrows represent likely business interactions and must be confirmed by the relevant owners. The required outbound call from Fournisseurs to Commandes is defined in Section 5.2.

### 2.3 Hosting and deployment

- Run the application services locally through root-level Docker Compose.
- Use Neon as the hosted PostgreSQL database.
- The Compose network should allow service-to-service calls by service name.
- From Postman running on the host, use `http://localhost:8081`, `http://localhost:8082`, `http://localhost:8083`, and `http://localhost:8084`.
- From one container to another, use Compose service DNS names, e.g. `http://service-commandes:8082`, not `localhost`.

**OPEN:** confirm the exact Compose service names, environment-variable names, readiness checks, and Neon SSL connection settings once the actual service projects are set up.

---

## 3. Common REST and JSON conventions

These are **PROPOSED shared conventions** for team approval.

### 3.1 Base paths and content type

- Use unversioned paths under `/api/...` for this academic project.
- Use `Content-Type: application/json` for JSON request bodies and `Accept: application/json` when a JSON response is expected.
- Use `camelCase` JSON property names, e.g. `fournisseurId`, `dateCommande`, `montantTotal`.
- IDs are integers in the first version, matching the initial database proposal. **OPEN:** confirm that all four services use the same ID type.
- Dates/timestamps use ISO 8601. **PROPOSED:** use UTC timestamps such as `2026-10-21T13:30:00Z`.
- Use decimal database types for monetary values; avoid floating-point representations for persisted money calculations.
- List endpoints return a JSON array in the initial version. Add pagination later if list sizes require it.
- Do not expose stack traces, database credentials, SQL errors, or other internal details in API responses.

### 3.2 HTTP status codes

| Status | Proposed use |
|---|---|
| `200 OK` | Successful read or update with a response body |
| `201 Created` | Resource created; response contains the created representation and ID |
| `204 No Content` | Successful operation with no response body, including soft deactivation if the team chooses this convention |
| `400 Bad Request` | Invalid/malformed input or validation failure |
| `404 Not Found` | Resource does not exist |
| `409 Conflict` | Duplicate unique value or business-rule conflict |
| `502 Bad Gateway` | A required downstream service returned an invalid/unusable response |
| `503 Service Unavailable` | A required downstream service is unavailable |
| `500 Internal Server Error` | Unexpected failure |

**OPEN:** confirm exact mapping for downstream `404` responses, timeouts, and connection failures. Do not translate a genuine “resource not found” into `500`.

### 3.3 Proposed common error body

```json
{
  "timestamp": "2026-10-21T13:30:00Z",
  "status": 404,
  "error": "NOT_FOUND",
  "message": "Fournisseur not found",
  "path": "/api/fournisseurs/42"
}
```

**PROPOSED:** Java and Node.js services should use the same fields and meaning. The exact format is not yet approved.

### 3.4 Authentication

**CURRENT PLAN:** no authentication for the academic demonstration.

- This is acceptable only if the team and teacher agree that authentication is not required.
- Treat the demo APIs as unauthenticated and do not expose them publicly with real or sensitive data.
- Keep database credentials in environment variables or an uncommitted local `.env` file.
- Never commit Neon connection strings or passwords to GitHub.

**OPEN:** confirm whether the teacher expects any authentication or authorization features.

---

## 4. Service API summary

The endpoint list below is a **proposed initial contract**. Each service owner must confirm it before implementation. Do not assume every listed operation is explicitly required by the assignment.

| Service | Proposed endpoints |
|---|---|
| Fournisseurs | `POST /api/fournisseurs`; `GET /api/fournisseurs`; `GET /api/fournisseurs/{id}`; `PUT /api/fournisseurs/{id}`; `DELETE /api/fournisseurs/{id}`; `GET /api/fournisseurs/{id}/commandes` |
| Commandes | `POST /api/commandes`; `GET /api/commandes`; `GET /api/commandes/{id}`; `PUT /api/commandes/{id}`; `DELETE /api/commandes/{id}` |
| Réceptions | `POST /api/receptions`; `GET /api/receptions`; `GET /api/receptions/{id}`; `GET /api/receptions/commande/{commandeId}`; `PUT /api/receptions/{id}`; `DELETE /api/receptions/{id}` |
| Factures | `POST /api/factures`; `GET /api/factures`; `GET /api/factures/{id}`; `PUT /api/factures/{id}`; `DELETE /api/factures/{id}` |

**OPEN:** confirm whether `PUT` and `DELETE` are needed for all entities, or whether the assignment only requires create/read/update operations. For orders, receipts, and invoices, logical cancellation/status changes may be safer than physical deletion.

---

## 5. Fournisseurs service — Mohamed

**Proposed stack:** Java / Spring Boot  
**Schema:** `schema_fournisseurs`  
**Container port:** `8081`

### 5.1 Supplier endpoints

| Method and path | Purpose | Proposed response |
|---|---|---|
| `POST /api/fournisseurs` | Create supplier | `201 Created` with supplier representation |
| `GET /api/fournisseurs` | List suppliers | `200 OK` with JSON array; optional filters may be added |
| `GET /api/fournisseurs/{id}` | Retrieve supplier by ID | `200 OK` or `404 Not Found` |
| `PUT /api/fournisseurs/{id}` | Update supplier | `200 OK`, `400`, or `404` |
| `DELETE /api/fournisseurs/{id}` | Soft-deactivate supplier | **PROPOSED:** `204 No Content` |

### 5.2 Consume Commandes to retrieve supplier order history

This is the agreed **proposed integration use case**: when a client opens a supplier's details, it can request the supplier's order history through Fournisseurs.

Fournisseurs endpoint:

```http
GET /api/fournisseurs/{id}/commandes
```

Commandes must expose this filtering endpoint:

```http
GET /api/commandes?fournisseurId={id}
```

Example internal call from Fournisseurs to Commandes:

```text
GET ${COMMANDES_URL}/api/commandes?fournisseurId=12
```

- In Docker Compose, `COMMANDES_URL` may be set to `http://service-commandes:8082`.
- When running services directly on the host, it may be set to `http://localhost:8082`.
- The actual environment-variable name is **PROPOSED**; team approval is needed to standardize it.

Example response from Commandes:

```json
[
  {
    "id": 101,
    "fournisseurId": 12,
    "dateCommande": "2026-10-15T10:30:00Z",
    "statut": "VALIDEE",
    "montantTotal": 450.00
  },
  {
    "id": 108,
    "fournisseurId": 12,
    "dateCommande": "2026-10-19T14:00:00Z",
    "statut": "EN_ATTENTE",
    "montantTotal": 1250.00
  }
]
```

The example values are illustrative, not real records.

**PROPOSED behavior:**

1. Fournisseurs verifies that the supplier exists.
2. Fournisseurs calls Commandes to retrieve orders filtered by `fournisseurId`.
3. Fournisseurs returns the JSON result to the client.
4. If Commandes is unavailable, the team must choose whether this endpoint returns `503` or a documented partial response. **Recommended for a dedicated `/commandes` endpoint:** return a clear `503` error, while ordinary supplier endpoints continue to work.
5. The endpoint should not directly read `schema_commandes`.

**OPEN:** confirm whether `GET /api/fournisseurs/{id}/commandes` returns only an order array or a wrapper object containing supplier and order information. The current proposal returns the order array.

### 5.3 Proposed supplier representation

```json
{
  "id": 12,
  "nom": "Atlas Fournitures",
  "email": "contact@atlas.example",
  "telephone": "+212600000000",
  "adresse": "Agadir, Maroc",
  "actif": true,
  "createdAt": "2026-10-21T13:30:00Z"
}
```

Creation requests should omit server-generated fields such as `id` and `createdAt`.

### 5.4 Soft deactivation and history

**PROPOSED TEAM DECISION:** supplier deletion is a soft deactivation, preserving the record and historical orders.

- Add `actif BOOLEAN NOT NULL DEFAULT TRUE` to `schema_fournisseurs.fournisseurs`.
- `DELETE /api/fournisseurs/{id}` sets `actif = false`; it does not physically delete the row.
- `GET /api/fournisseurs/{id}` may still return an inactive supplier with `"actif": false`, so historical references remain understandable.
- `GET /api/fournisseurs` excludes inactive suppliers by default.
- Commandes must reject creation of new orders for inactive suppliers.
- Email uniqueness remains enforced even for inactive suppliers unless the team explicitly chooses a different policy.

**OPEN:** confirm whether a deactivated supplier may be reactivated, and whether the list API needs an `includeInactive` filter.

### 5.5 Proposed supplier validation

- `nom`: required; max length must match the agreed database column.
- `email`: required, valid email format, unique.
- `telephone`: optional.
- `adresse`: optional.
- `actif`: server-managed on creation and deactivation; clients should not be allowed to bypass the lifecycle rules by setting it arbitrarily.

The current database proposal uses `VARCHAR(150)` for name and email, `VARCHAR(30)` for telephone, and `TEXT` for address. Confirm these lengths before implementing DTO validation.

---

## 6. Commandes service — Yassine

**Proposed stack:** Java / Spring Boot  
**Schema:** `schema_commandes`  
**Container port:** `8082`

### 6.1 Proposed endpoints

| Method and path | Purpose |
|---|---|
| `POST /api/commandes` | Create an order and its lines |
| `GET /api/commandes` | List orders; optional filters |
| `GET /api/commandes/{id}` | Retrieve an order |
| `PUT /api/commandes/{id}` | Update an order, subject to lifecycle rules |
| `DELETE /api/commandes/{id}` | **OPEN:** physical delete versus logical cancellation |

### 6.2 Required supplier validation

When creating an order, Commandes calls:

```http
GET ${FOURNISSEURS_URL}/api/fournisseurs/{fournisseurId}
```

It should reject creation if the supplier does not exist or is inactive. This prevents a new order from referencing a supplier that is unavailable for new business.

### 6.3 Proposed create request

```json
{
  "fournisseurId": 12,
  "lignes": [
    {
      "designation": "Papier A4",
      "quantite": 10,
      "prixUnitaire": 45.00
    }
  ]
}
```

### 6.4 Proposed order representation

```json
{
  "id": 101,
  "fournisseurId": 12,
  "dateCommande": "2026-10-15T10:30:00Z",
  "statut": "EN_ATTENTE",
  "montantTotal": 450.00,
  "lignes": [
    {
      "id": 501,
      "designation": "Papier A4",
      "quantite": 10,
      "prixUnitaire": 45.00
    }
  ]
}
```

**PROPOSED:** Commandes calculates `montantTotal` from its lines rather than trusting a client-supplied total.

### 6.5 Proposed lifecycle

```text
EN_ATTENTE -> VALIDEE -> RECEPTIONNEE -> FACTUREE
     \             \            \
      +------------+-------------> ANNULEE (only where allowed)
```

This diagram is only a starting point. It does **not** define all valid transitions, and it must support partial receipts without prematurely marking an order fully received.

**OPEN — TEAM DECISION REQUIRED:**

- Which roles/actions validate an order?
- Can an order be edited after validation?
- Can an order be cancelled after any receipt or invoice exists?
- Does `RECEPTIONNEE` mean fully received, or does a separate partial-receipt status exist?
- Can an order have multiple invoices?
- Should `DELETE` cancel an order instead of physically deleting it?

---

## 7. Réceptions service — Ali

**Proposed stack:** Java / Spring Boot  
**Schema:** `schema_receptions`  
**Container port:** `8083`

### 7.1 Proposed endpoints

| Method and path | Purpose |
|---|---|
| `POST /api/receptions` | Record a receipt for an order |
| `GET /api/receptions` | List receipts; optional filters |
| `GET /api/receptions/{id}` | Retrieve a receipt |
| `GET /api/receptions/commande/{commandeId}` | Retrieve receipts for an order |
| `PUT /api/receptions/{id}` | Update receipt details/status |
| `DELETE /api/receptions/{id}` | **OPEN:** define deletion/cancellation behavior |

### 7.2 Multiple and partial receipts

**TEAM'S PROPOSED DECISION:** one order may have multiple receipts, supporting partial deliveries.

Réceptions should call Commandes to validate the order before creating a receipt:

```http
GET ${COMMANDES_URL}/api/commandes/{commandeId}
```

### 7.3 Proposed receipt request

```json
{
  "commandeId": 101,
  "dateReception": "2026-10-21T13:30:00Z",
  "remarques": "Livraison partielle",
  "statutReception": "PARTIELLE"
}
```

**OPEN:** the initial schema proposal does not include received quantities or receipt lines. If the team needs to determine whether an order is partially or fully received, it may need line-level receipt quantities or another explicit representation. Do not assume that a status field alone is enough to calculate partial delivery.

### 7.4 Proposed receipt statuses

`CONFORME`, `PARTIELLE`, `NON_CONFORME`

These values are suggestions based on the current project notes and require Ali/team approval. The team must decide whether an invoice may refer to a partial receipt and how multiple receipts affect the order lifecycle.

**Important ownership rule:** Réceptions should not update `schema_commandes` directly. If the order status must change after a receipt, define an endpoint/action in Commandes or agree on a different status-management design.

---

## 8. Factures service — Hamza

**Proposed stack:** Node.js (**framework OPEN**)  
**Schema:** `schema_factures`  
**Container port:** `8084`

### 8.1 Proposed endpoints

| Method and path | Purpose |
|---|---|
| `POST /api/factures` | Create an invoice |
| `GET /api/factures` | List invoices; optional filters |
| `GET /api/factures/{id}` | Retrieve an invoice |
| `PUT /api/factures/{id}` | Update allowed invoice/payment fields |
| `DELETE /api/factures/{id}` | **OPEN:** define deletion/cancellation behavior |

### 8.2 Invoice eligibility

**TEAM'S PROPOSED DECISION:** an invoice requires an eligible receipt.

Before creating an invoice, Factures calls:

```http
GET ${COMMANDES_URL}/api/commandes/{commandeId}
GET ${RECEPTIONS_URL}/api/receptions/{receptionId}
```

Factures must verify that:

- the order exists;
- the receipt exists;
- the receipt belongs to the same order referenced by the invoice;
- the receipt's status is eligible under the team's final business rules.

The exact rule for whether `PARTIELLE` is eligible remains **OPEN**.

### 8.3 Proposed invoice request

```json
{
  "commandeId": 101,
  "receptionId": 58,
  "montantFacture": 450.00,
  "dateFacture": "2026-10-21T13:30:00Z"
}
```

### 8.4 Proposed payment statuses

`NON_PAYEE`, `PAYEE`

**OPEN — TEAM DECISION REQUIRED:**

- Must invoice amount equal the order total, the receipt value, or a separately recorded supplier invoice amount?
- Can several invoices reference one order or one receipt?
- Can an invoice be edited after payment?
- Is physical deletion permitted, or should invoices be retained for history?
- Which receipt statuses are eligible?

---

## 9. Shared database contract

### 9.1 Proposed schema ownership

| Schema | Owner | Proposed tables |
|---|---|---|
| `schema_fournisseurs` | Mohamed | `fournisseurs` |
| `schema_commandes` | Yassine | `commandes`, `lignes_commande` |
| `schema_receptions` | Ali | `receptions` |
| `schema_factures` | Hamza | `factures` |

These table names reflect the current project notes and need confirmation by their owners.

### 9.2 Cross-service references

| Field | Refers to | Proposed validation |
|---|---|---|
| `commandes.fournisseur_id` | Supplier ID | Commandes calls Fournisseurs |
| `receptions.commande_id` | Order ID | Réceptions calls Commandes |
| `factures.commande_id` | Order ID | Factures calls Commandes |
| `factures.reception_id` | Receipt ID | Factures calls Réceptions |

**PROPOSED:** these are logical references across service boundaries. Do not add cross-schema SQL foreign keys if the team adopts strict schema ownership and REST-based cross-service validation.

### 9.3 Local database constraints

The owning service should enforce its own local invariants, including:

- primary keys;
- required fields and appropriate maximum lengths;
- unique supplier email;
- positive order-line quantity;
- non-negative unit prices and amounts;
- valid local status values where practical.

### 9.4 Neon initialization and migrations

**OPEN — NOT DECIDED:** the team has not selected the database initialization/migration approach.

Options to discuss:

1. A shared SQL initialization script, executed once against Neon.
2. Separate migrations maintained by each service for its own schema.

Whichever option is chosen, schema changes must be coordinated and reproducible. Do not run a destructive initialization script against a database containing teammates' data. Do not commit real Neon credentials.

**OPEN:** confirm the Neon connection string/SSL settings and whether the existing `database/init.sql` should be adapted for Neon. If Neon is the chosen database, a local PostgreSQL container may not be needed in Compose.

---

## 10. Docker and configuration conventions

These are **PROPOSED** conventions to agree on.

- Each service repository directory contains its own `Dockerfile` and necessary build files.
- The root Compose file starts all four application services on one network.
- Configure ports consistently: `8081`, `8082`, `8083`, `8084`, unless owners agree otherwise.
- Use environment variables for database settings and downstream service URLs.
- Do not hard-code secrets in Dockerfiles, Compose files, or source code.
- Container-to-container URLs use Compose service names. `localhost` inside a container points to that same container, not another service.
- `depends_on` alone may not mean that an application is ready. Handle connection retries/readiness as appropriate.
- Because Neon is hosted, Compose should not start a second PostgreSQL database unless the team explicitly chooses a local/hybrid setup.
- Confirm that each service binds to `0.0.0.0` inside its container where needed.

**OPEN:** final service names in Compose, environment variable names, health checks, and build commands must match the actual implementations.

---

## 11. Postman collection and acceptance tests

The team will maintain a shared Postman collection, with safe example environment variables and no real credentials.

### 11.1 Common environment variables (proposed)

```text
FOURNISSEURS_BASE_URL=http://localhost:8081
COMMANDES_BASE_URL=http://localhost:8082
RECEPTIONS_BASE_URL=http://localhost:8083
FACTURES_BASE_URL=http://localhost:8084
```

These are host-side URLs for Postman. Container-side service URLs must use Compose DNS names.

### 11.2 Minimum integration test scenarios

1. Create a supplier with valid data → `201 Created`.
2. Create a supplier with invalid email → `400 Bad Request`.
3. Create another supplier with the same email → `409 Conflict`.
4. Retrieve a nonexistent supplier → `404 Not Found`.
5. Soft-deactivate a supplier → it remains available for historical lookup with `actif=false`.
6. Attempt to create an order for a nonexistent supplier → creation fails.
7. Attempt to create an order for an inactive supplier → creation fails.
8. Create an order for an active supplier → succeeds.
9. Query `GET /api/commandes?fournisseurId={id}` → only matching orders are returned.
10. Query `GET /api/fournisseurs/{id}/commandes` → Fournisseurs consumes Commandes and returns the corresponding order history.
11. Attempt to create a receipt for a nonexistent order → creation fails.
12. Create multiple receipts for the same order → behavior matches the final partial-delivery rules.
13. Attempt to create an invoice for a nonexistent order or receipt → creation fails.
14. Attempt to create an invoice for a receipt belonging to a different order → creation fails.
15. Verify that a downstream service outage is reported consistently and does not leak internal errors.
16. Build and start all four services through root-level Docker Compose and run the integration collection.

---

## 12. Decisions still open

The following items must remain visible as unresolved until the team agrees:

| ID | Open decision | Suggested person(s) to coordinate |
|---|---|---|
| O1 | Final endpoint list for each service; confirm which CRUD operations are required | All four owners |
| O2 | Confirm all IDs use the same type and date/time convention | All four owners |
| O3 | Exact error body and downstream failure mapping | All four owners |
| O4 | Supplier reactivation and `includeInactive` filtering | Mohamed + Yassine |
| O5 | Order status transitions and cancellation policy | Yassine + Ali + Hamza |
| O6 | Partial-receipt data model and quantities | Ali + Yassine |
| O7 | Whether partial receipts are invoice-eligible | Ali + Hamza + Yassine |
| O8 | Invoice amount rules and invoice/receipt cardinality | Hamza + Yassine + Ali |
| O9 | Database initialization versus per-service migrations | All four owners |
| O10 | Whether any authentication is required by the teacher | Team; ask teacher if unclear |
| O11 | Framework for the Node.js Factures service | Hamza |
| O12 | Compose service names, health checks, and environment-variable names | All four owners |
| O13 | What meaningful REST service Fournisseurs consumes | Mohamed + team/teacher; proposed Commandes order history |
| O14 | Whether OpenAPI/Swagger is additionally expected | All four owners / teacher |

---

## 13. Proposed repository artifacts

```text
ProcuraFlow/
├── README.md
├── docker-compose.yml
├── docs/
│   └── integration-contract.md
├── database/
│   └── init.sql                 # approach OPEN; may be replaced by migrations
├── postman/
│   ├── ProcuraFlow.postman_collection.json
│   └── ProcuraFlow.environment.example.json
├── service-fournisseurs-mohamed/
├── service-commandes-yassine/
├── service-receptions-ali/
└── service-factures-hamza/
```

This layout is a proposal, not a requirement. Keep the current repository names if the team has already adopted them.

---

## 14. Team review checklist

Before calling this contract approved, confirm that:

- [ ] Each service owner approves their endpoint list and JSON fields.
- [ ] All owners agree on ID types, date format, status codes, and error body.
- [ ] Each service owns its schema and other services use REST for cross-service access.
- [ ] Supplier soft deactivation preserves historical supplier references.
- [ ] Commandes validates supplier existence and active status through Fournisseurs.
- [ ] Fournisseurs retrieves order history through Commandes.
- [ ] Partial/multiple receipt behavior is reflected in the data model.
- [ ] Invoice eligibility and amount rules are agreed.
- [ ] Neon initialization/migrations are agreed and reproducible.
- [ ] Authentication expectations have been checked against the assignment.
- [ ] Each service has a working Dockerfile and the root Compose setup works.
- [ ] The shared Postman collection tests both success and failure paths.

**Approval status:** Pending review by Mohamed, Yassine, Ali, and Hamza.

*End of draft.*
