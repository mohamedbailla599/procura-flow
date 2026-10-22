# ProcuraFlow — Integration Contract

**Version:** 0.3 (updated with Fournisseurs implementation notes for team review)  
**Status:** Proposed decisions; Fournisseurs service implemented and ready for integration  
**Project:** Gestion des fournisseurs et des achats  
**Last updated:** 2026-10-21

> **Purpose:** This document defines the proposed REST interfaces and integration rules for the four ProcuraFlow services. Requirements explicitly stated by the teacher take precedence. Items marked **OPEN** require a team decision; items marked **PROPOSED** are recommendations to review.

---

## 1. Project scope and constraints

ProcuraFlow is an academic collaborative project with four independently owned services:

1. **Fournisseurs** — Mohamed Bailla (`service-fournisseurs-mohamed`, port `8081`)
2. **Commandes** — Yassine (`service-commandes-yassine`, port `8082`)
3. **Réceptions** — Ali (`service-receptions-ali`, port `8083`)
4. **Factures** — Hamza (`service-factures-hamza`, port `8084`)

### Team's current technology plan

| Service | Owner | Proposed technology | Container port |
|---|---|---|---:|
| Fournisseurs | Mohamed | Java / Spring Boot | `8081` |
| Commandes | Yassine | Java / Spring Boot | `8082` |
| Réceptions | Ali | Java / Spring Boot | `8083` |
| Factures | Hamza | Node.js (framework **OPEN**) | `8084` |

---

## 2. Architecture and ownership

Each service owns its business logic and its database schema in the shared Neon PostgreSQL 17 database (`procura_flow`):

- `schema_fournisseurs` (table `fournisseurs`)
- `schema_commandes` (tables `commandes`, `lignes_commande`)
- `schema_receptions` (table `receptions`)
- `schema_factures` (table `factures`)

**Ownership rule:** Each service is the only service allowed to read or write to its own schema. Cross-service interactions must occur exclusively via REST APIs.

---

## 3. Common REST and JSON conventions

- Base paths under `/api/...`
- JSON properties in `camelCase` (`fournisseurId`, `dateCommande`, `montantTotal`, `createdAt`, `updatedAt`)
- Timestamps in UTC ISO-8601 (`2026-10-21T13:30:00Z`)
- Common error payload:

```json
{
  "timestamp": "2026-10-21T13:30:00Z",
  "status": 404,
  "error": "NOT_FOUND",
  "message": "Fournisseur introuvable avec l'identifiant : 42",
  "path": "/api/fournisseurs/42"
}
```

---

## 4. Fournisseurs Service — Implemented Contract (`service-fournisseurs-mohamed`)

| Method and path | Purpose | Status code |
|---|---|---|
| `POST /api/fournisseurs` | Create supplier (`actif = true` by default) | `201 Created` (`FournisseurResponse`), `400`, `409` |
| `GET /api/fournisseurs` | List suppliers (optional `actif`, `nom`, `email`, `includeInactive` filters) | `200 OK` (`FournisseurResponse[]`) |
| `GET /api/fournisseurs/{id}` | Retrieve supplier by ID (returns active and inactive suppliers) | `200 OK` (`FournisseurResponse`), `404` |
| `PUT /api/fournisseurs/{id}` | Update supplier (`nom`, `email`, `telephone`, `adresse`) | `200 OK` (`FournisseurResponse`), `400`, `404`, `409` |
| `PATCH /api/fournisseurs/{id}/desactiver` | Soft-deactivate supplier (`actif = false`) | `200 OK` (`FournisseurResponse`), `404` |
| `DELETE /api/fournisseurs/{id}` | Soft-deactivate supplier (`actif = false`, no physical delete) | `204 No Content`, `404` |
| `PATCH /api/fournisseurs/{id}/reactiver` | Reactivate supplier (`actif = true`, proposed extension for O4) | `200 OK` (`FournisseurResponse`), `404` |
| `GET /api/fournisseurs/{id}/commandes` | Retrieve supplier order history from Commandes (`GET /api/commandes?fournisseurId={id}`) | `200 OK`, `404`, `502`, `503` |
| `GET /actuator/health` | Service readiness/liveness check | `200 OK` |

### Supplier JSON representation (`FournisseurResponse`)

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

### Outbound integration with Commandes (`Yassine`)

- Fournisseurs calls `GET ${COMMANDES_SERVICE_URL}/api/commandes?fournisseurId={id}` (`COMMANDES_SERVICE_URL` is the single canonical variable name in `service-fournisseurs-mohamed`).
- When Commandes creates a new order (`POST /api/commandes`), Commandes should call `GET ${FOURNISSEURS_URL}/api/fournisseurs/{fournisseurId}` and verify both HTTP `200 OK` and `"actif": true`.
