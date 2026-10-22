# ProcuraFlow — Gestion des fournisseurs et des achats (Architecture Microservices)

Ce dépôt contient les quatre microservices du projet académique **ProcuraFlow**, partageant une base de données **Neon PostgreSQL 17** (`procura_flow`) avec isolation stricte par schéma PostgreSQL et communication inter-services par API REST JSON.

## Services du projet

| Service | Répertoire | Responsable | Technologie | Port | Schéma PostgreSQL | Statut |
|---|---|---|---|---:|---|---|
| **Fournisseurs** | `service-fournisseurs-mohamed` | Mohamed Bailla | Java / Spring Boot 3 | `8081` | `schema_fournisseurs` | Implémenté & testé |
| **Commandes** | `service-commandes-yassine` | Yassine | Java / Spring Boot | `8082` | `schema_commandes` | Squelette |
| **Réceptions** | `service-receptions-ali` | Ali | Java / Spring Boot | `8083` | `schema_receptions` | Squelette |
| **Factures** | `service-factures-hamza` | Hamza | Node.js | `8084` | `schema_factures` | Squelette |

## Documentation

- **Contrat d'intégration inter-services :** [`docs/integration-contract.md`](docs/integration-contract.md)
- **Documentation complète du service Fournisseurs :** [`service-fournisseurs-mohamed/README.md`](service-fournisseurs-mohamed/README.md)
- **Variables d'environnement d'exemple :** [`.env.example`](.env.example) et [`service-fournisseurs-mohamed/.env.example`](service-fournisseurs-mohamed/.env.example)
- **Collection Postman :** [`postman/Collection_Projet_Achats_Univ.json`](postman/Collection_Projet_Achats_Univ.json)
