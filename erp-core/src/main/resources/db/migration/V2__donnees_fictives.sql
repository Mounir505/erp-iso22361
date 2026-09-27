-- =============================================================================
-- V2 — Jeu de données ENTIÈREMENT FICTIF (atelier ISO 22361, lab isolé)
-- Aucune personne réelle. Mot de passe de démonstration commun : Lab2026!
-- (hash BCrypt calculé par pgcrypto, compatible avec BCryptPasswordEncoder)
-- =============================================================================
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- ------------------------------------------------------------------ Rôles ----
INSERT INTO role (libelle, est_role_de_crise) VALUES
    ('ADMIN',              FALSE),
    ('MEDECIN',            FALSE),
    ('INFIRMIER',          FALSE),
    ('AGENT_ADMISSION',    FALSE),
    ('PHARMACIEN',         FALSE),
    -- Rôles de la cellule de crise (livrable 1.4)
    ('PILOTE_CRISE',       TRUE),
    ('RESP_TECHNIQUE',     TRUE),
    ('RESP_COMMUNICATION', TRUE),
    ('SECRETAIRE_CRISE',   TRUE);

-- ------------------------------------------------------------ Utilisateurs ----
INSERT INTO utilisateur (nom, login, mot_de_passe, role_id)
SELECT u.nom, u.login, crypt('Lab2026!', gen_salt('bf', 10)), r.id
FROM (VALUES
    ('Administrateur SI',        'admin',       'ADMIN'),
    ('Dr Salma Benali',          'dr.benali',   'MEDECIN'),
    ('Dr Karim Ouazzani',        'dr.ouazzani', 'MEDECIN'),
    ('Nadia Amrani',             'inf.amrani',  'INFIRMIER'),
    ('Youssef Idrissi',          'adm.idrissi', 'AGENT_ADMISSION'),
    ('Leila Tazi',               'pharma.tazi', 'PHARMACIEN'),
    ('Omar Chraibi',             'pilote',      'PILOTE_CRISE'),
    ('Imane Berrada',            'tech',        'RESP_TECHNIQUE'),
    ('Hamza El Fassi',           'com',         'RESP_COMMUNICATION'),
    ('Sara Lahlou',              'secretaire',  'SECRETAIRE_CRISE')
) AS u(nom, login, role)
JOIN role r ON r.libelle = u.role;

-- --------------------------------------------------------------- Patients ----
INSERT INTO patient (nom, date_naissance, statut) VALUES
    ('Amina Fictive-Alaoui',     '1958-03-14', 'HOSPITALISE'),
    ('Rachid Fictif-Bennani',    '1971-11-02', 'HOSPITALISE'),
    ('Fatima Fictive-Cherkaoui', '1990-06-21', 'HOSPITALISE'),
    ('Mehdi Fictif-Daoudi',      '1985-01-30', 'HOSPITALISE'),
    ('Khadija Fictive-Essaidi',  '1947-09-09', 'HOSPITALISE'),
    ('Yassine Fictif-Filali',    '2001-12-17', 'HOSPITALISE'),
    ('Zineb Fictive-Guessous',   '1966-04-05', 'HOSPITALISE'),
    ('Hicham Fictif-Hajji',      '1979-08-23', 'HOSPITALISE'),
    ('Sanae Fictive-Iraqi',      '1995-02-11', 'AMBULATOIRE'),
    ('Adil Fictif-Jabri',        '1988-07-07', 'AMBULATOIRE'),
    ('Houda Fictive-Kettani',    '1973-10-29', 'AMBULATOIRE'),
    ('Nabil Fictif-Lamrani',     '1962-05-16', 'AMBULATOIRE'),
    ('Salma Fictive-Mansouri',   '2010-03-03', 'SORTI'),
    ('Tarik Fictif-Naciri',      '1955-12-01', 'SORTI'),
    ('Rim Fictive-Ouali',        '1999-09-19', 'SORTI'),
    ('Anas Fictif-Rami',         '1983-06-12', 'SORTI');

-- Un dossier médical par patient (Patient 1 --> 1 DossierMedical)
INSERT INTO dossier_medical (patient_id, antecedents, observations)
SELECT p.id,
       (ARRAY['Hypertension artérielle', 'Diabète de type 2', 'Asthme', 'Aucun antécédent notable',
              'Insuffisance cardiaque', 'Allergie à la pénicilline', 'BPCO', 'Appendicectomie (2012)'])[1 + (p.id % 8)],
       (ARRAY['Constantes stables, surveillance standard.',
              'Douleur thoracique à surveiller, ECG de contrôle prévu.',
              'Post-opératoire J2, cicatrice propre.',
              'Glycémie à contrôler 3x/jour.',
              'Saturation 94 % sous 2 L d''O2.',
              'Patient autonome, sortie envisagée sous 48 h.'])[1 + (p.id % 6)]
FROM patient p;

-- ------------------------------------------------------------- Admissions ----
-- Admissions en cours pour les patients hospitalisés
INSERT INTO admission (patient_id, date_entree, service, statut)
SELECT p.id,
       now() - (p.id || ' days')::interval - interval '3 hours',
       (ARRAY['Urgences', 'Cardiologie', 'Chirurgie', 'Médecine interne', 'Pneumologie'])[1 + (p.id % 5)],
       'EN_COURS'
FROM patient p WHERE p.statut = 'HOSPITALISE';

-- Admissions clôturées (historique) pour les autres patients
INSERT INTO admission (patient_id, date_entree, service, statut)
SELECT p.id,
       now() - ((20 + p.id * 3) || ' days')::interval,
       (ARRAY['Urgences', 'Consultations externes', 'Pédiatrie', 'Orthopédie'])[1 + (p.id % 4)],
       'CLOTUREE'
FROM patient p WHERE p.statut <> 'HOSPITALISE';

-- ----------------------------------------------------------- Prescriptions ----
INSERT INTO prescription (admission_id, medicament, posologie, date)
SELECT a.id, m.medicament, m.posologie, a.date_entree + interval '2 hours'
FROM admission a
JOIN LATERAL (
    SELECT * FROM (VALUES
        ('Paracétamol 1 g',     '1 cp toutes les 6 h si douleur, max 4/j'),
        ('Amoxicilline 1 g',    '1 cp matin et soir pendant 7 jours'),
        ('Énoxaparine 4000 UI', '1 injection SC par jour'),
        ('Metformine 850 mg',   '1 cp matin et soir au repas'),
        ('Furosémide 40 mg',    '1 cp le matin'),
        ('Oméprazole 20 mg',    '1 gélule le matin à jeun')
    ) AS t(medicament, posologie)
    OFFSET (a.id % 6) LIMIT 1 + (a.id % 2)
) m ON TRUE;

-- --------------------------------------------------- Supervision & crise ----
-- Seuil livrable 1.3 : > 20 échecs d'authentification / minute
INSERT INTO detecteur_de_seuil (id, seuil_echecs_auth, fenetre) VALUES (1, 20, 60);
INSERT INTO mode_degrade (id, actif, lecture_seule) VALUES (1, FALSE, TRUE);

INSERT INTO journal_audit (timestamp, niveau, type_evenement, source, message, utilisateur_id, details)
VALUES (now(), 'INFO', 'initialisation', 'flyway', 'Base initialisée avec un jeu de données fictif', NULL,
        '{"patients": 16, "fictif": true}'::jsonb);
