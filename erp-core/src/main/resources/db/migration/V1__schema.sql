-- =============================================================================
-- V1 — Schéma conforme au diagramme de classes (classeDiagram.png)
-- Écarts documentés (validés par l'équipe) :
--   * journal_audit.details (jsonb)   : exigé par le contrat d'interface 2.1
--   * detecteur_de_seuil.id / mode_degrade.id : clés techniques de singletons
-- Tous les horodatages sont en UTC (timestamptz).
-- =============================================================================

-- ---------------------------------------------------------------- Métier ----
CREATE TABLE role (
    id                  SERIAL PRIMARY KEY,
    libelle             VARCHAR(50)  NOT NULL UNIQUE,
    est_role_de_crise   BOOLEAN      NOT NULL DEFAULT FALSE
);

-- Utilisateur 1 --> 1 Role : chaque utilisateur a exactement un rôle
CREATE TABLE utilisateur (
    id              SERIAL PRIMARY KEY,
    nom             VARCHAR(100) NOT NULL,
    login           VARCHAR(50)  NOT NULL UNIQUE,
    mot_de_passe    VARCHAR(100) NOT NULL,          -- hash BCrypt
    role_id         INTEGER      NOT NULL REFERENCES role(id)
);

CREATE TABLE patient (
    id               SERIAL PRIMARY KEY,
    nom              VARCHAR(100) NOT NULL,
    date_naissance   DATE         NOT NULL,
    statut           VARCHAR(20)  NOT NULL
        CHECK (statut IN ('HOSPITALISE', 'AMBULATOIRE', 'SORTI'))
);

-- Patient 1 --> 1 DossierMedical
CREATE TABLE dossier_medical (
    id              SERIAL PRIMARY KEY,
    patient_id      INTEGER NOT NULL UNIQUE REFERENCES patient(id) ON DELETE CASCADE,
    antecedents     TEXT,
    observations    TEXT
);

-- Patient 1 --> * Admission
CREATE TABLE admission (
    id              SERIAL PRIMARY KEY,
    patient_id      INTEGER      NOT NULL REFERENCES patient(id) ON DELETE CASCADE,
    date_entree     TIMESTAMPTZ  NOT NULL,
    service         VARCHAR(80)  NOT NULL,
    statut          VARCHAR(20)  NOT NULL
        CHECK (statut IN ('EN_COURS', 'CLOTUREE', 'ANNULEE'))
);
CREATE INDEX idx_admission_patient ON admission(patient_id);

-- Admission 1 --> * Prescription
CREATE TABLE prescription (
    id              SERIAL PRIMARY KEY,
    admission_id    INTEGER      NOT NULL REFERENCES admission(id) ON DELETE CASCADE,
    medicament      VARCHAR(120) NOT NULL,
    posologie       VARCHAR(200) NOT NULL,
    date            TIMESTAMPTZ  NOT NULL
);
CREATE INDEX idx_prescription_admission ON prescription(admission_id);

-- ---------------------------------------------------- Supervision & crise ----
-- Utilisateur 1 --genere--> * JournalAudit (acteur nullable : compte de service)
CREATE TABLE journal_audit (
    id                  SERIAL PRIMARY KEY,
    timestamp           TIMESTAMPTZ  NOT NULL,
    niveau              VARCHAR(10)  NOT NULL CHECK (niveau IN ('INFO', 'WARNING', 'CRITICAL')),
    type_evenement      VARCHAR(60)  NOT NULL,
    source              VARCHAR(60)  NOT NULL,
    message             TEXT         NOT NULL,
    utilisateur_id      INTEGER      REFERENCES utilisateur(id) ON DELETE SET NULL,
    details             JSONB
);
CREATE INDEX idx_journal_timestamp ON journal_audit(timestamp);
CREATE INDEX idx_journal_niveau    ON journal_audit(niveau);

-- Singleton : paramètres modifiables à chaud du détecteur
CREATE TABLE detecteur_de_seuil (
    id                  INTEGER PRIMARY KEY CHECK (id = 1),
    seuil_echecs_auth   INTEGER NOT NULL CHECK (seuil_echecs_auth > 0),
    fenetre             INTEGER NOT NULL CHECK (fenetre > 0)       -- en secondes
);

-- DetecteurDeSeuil 1 --produit--> * Evenement ; Evenement * --trace--> 1 JournalAudit
CREATE TABLE evenement (
    id                      SERIAL PRIMARY KEY,
    niveau                  VARCHAR(10)  NOT NULL CHECK (niveau IN ('INFO', 'WARNING', 'CRITICAL')),
    type                    VARCHAR(60)  NOT NULL,
    timestamp               TIMESTAMPTZ  NOT NULL,
    detecteur_de_seuil_id   INTEGER      NOT NULL REFERENCES detecteur_de_seuil(id),
    journal_audit_id        INTEGER      NOT NULL REFERENCES journal_audit(id)
);
CREATE INDEX idx_evenement_timestamp ON evenement(timestamp);

CREATE TABLE cellule_de_crise (
    id                  SERIAL PRIMARY KEY,
    statut              VARCHAR(20)  NOT NULL CHECK (statut IN ('ACTIVE', 'CLOTUREE')),
    date_activation     TIMESTAMPTZ  NOT NULL
);
-- Au plus une cellule active à la fois
CREATE UNIQUE INDEX uq_cellule_active ON cellule_de_crise(statut) WHERE statut = 'ACTIVE';

-- CelluleDeCrise 1 --prend--> * Decision
CREATE TABLE decision (
    id                  SERIAL PRIMARY KEY,
    cellule_de_crise_id INTEGER      NOT NULL REFERENCES cellule_de_crise(id),
    libelle             TEXT         NOT NULL,
    auteur              VARCHAR(100) NOT NULL,
    timestamp           TIMESTAMPTZ  NOT NULL
);
CREATE INDEX idx_decision_cellule ON decision(cellule_de_crise_id);

-- Singleton : état du mode dégradé (restreint DossierMedical en lecture seule)
CREATE TABLE mode_degrade (
    id              INTEGER PRIMARY KEY CHECK (id = 1),
    actif           BOOLEAN NOT NULL DEFAULT FALSE,
    lecture_seule   BOOLEAN NOT NULL DEFAULT TRUE
);
