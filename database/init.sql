CREATE SCHEMA IF NOT EXISTS schema_fournisseurs;
CREATE SCHEMA IF NOT EXISTS schema_commandes;
CREATE SCHEMA IF NOT EXISTS schema_receptions;
CREATE SCHEMA IF NOT EXISTS schema_factures;

-- Table Fournisseurs (Mohamed)
CREATE TABLE schema_fournisseurs.fournisseurs (
    id SERIAL PRIMARY KEY,
    nom VARCHAR(150) NOT NULL,
    email VARCHAR(150) UNIQUE NOT NULL,
    telephone VARCHAR(30),
    adresse TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Table Commandes (Yassine)
CREATE TABLE schema_commandes.commandes (
    id SERIAL PRIMARY KEY,
    fournisseur_id INT NOT NULL,
    date_commande TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    statut VARCHAR(30) DEFAULT 'EN_ATTENTE',
    montant_total DECIMAL(10, 2) NOT NULL
);

CREATE TABLE schema_commandes.lignes_commande (
    id SERIAL PRIMARY KEY,
    commande_id INT REFERENCES schema_commandes.commandes(id) ON DELETE CASCADE,
    designation VARCHAR(150) NOT NULL,
    quantite INT NOT NULL,
    prix_unitaire DECIMAL(10, 2) NOT NULL
);

-- Table Réceptions (Ali)
CREATE TABLE schema_receptions.receptions (
    id SERIAL PRIMARY KEY,
    commande_id INT NOT NULL,
    date_reception TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    remarques TEXT,
    statut_reception VARCHAR(30) DEFAULT 'CONFORME'
);

-- Table Factures (Hamza)
CREATE TABLE schema_factures.factures (
    id SERIAL PRIMARY KEY,
    commande_id INT NOT NULL,
    reception_id INT,
    date_facture TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    montant_facture DECIMAL(10, 2) NOT NULL,
    statut_paiement VARCHAR(30) DEFAULT 'NON_PAYEE'
);