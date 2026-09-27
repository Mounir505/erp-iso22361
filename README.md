# Mini-ERP hospitalier — Gestion de crise ISO 22361

Atelier académique (projet n°3) : un petit ERP hospitalier qui met en œuvre la **réponse à une crise** selon
ISO 22361:2022 — détection, cellule de crise, mode dégradé, traçabilité, rétablissement et retour d'expérience.

> ⚠️ **Lab isolé, données entièrement fictives.** L'ERP tourne sur un réseau Docker interne sans accès Internet.
> Aucun exploit ni charge malveillante : les signaux de crise sont produits par un usage normal de l'API ou par un
> simulateur désactivé par défaut (voir [scénario](docs/scenario-exercice.md#la--faiblesse-contrôlée-)).

Entrées de conception : `Livrables_Equipe_ERP_ISO22361.pdf` (besoins, contrat d'interface, infrastructure) et
`classeDiagram.png` (modèle de données).

---

## Sommaire

1. [Architecture](#architecture)
2. [Prérequis](#prérequis)
3. [Lancement](#lancement)
4. [Comptes de démonstration](#comptes-de-démonstration)
5. [Fonctionnalités](#fonctionnalités)
6. [API](#api)
7. [Scénario de soutenance](#scénario-de-soutenance)
8. [Correspondance ISO 22361](#correspondance-iso-22361)
9. [Sauvegarde et restauration](#sauvegarde-et-restauration)
10. [Développement et tests](#développement-et-tests)
11. [Écarts assumés par rapport au diagramme](#écarts-assumés-par-rapport-au-diagramme)
12. [Copyright](#copyright)

---

## Architecture

```
                 réseau Docker interne « erp-lab » 172.16.10.0/24 (internal: true)
 ┌────────────────────────┐      ┌────────────────────────┐      ┌───────────────────────┐
 │ dashboard 172.16.10.20 │ ───► │ erp-core 172.16.10.10  │ ───► │ erp-db 172.16.10.11   │
 │ nginx :8501            │ /api │ Spring Boot :8000      │ JDBC │ PostgreSQL 16 :5432   │
 │ React 19 (ERP + crise) │      │ /health /events …      │      │ données FICTIVES      │
 └──────────┬─────────────┘      └───────────┬────────────┘      └──────────┬────────────┘
            │ lecture seule                  │ écrit                        │
            └──────── volume erp-logs (events.log, JSON) ◄──┘               volume erp-backups
                                                                            (monté ici seulement)
 poste-test 172.16.10.30 : conteneur éphémère lancé par scripts/poste-test.sh
```

| Service | Stack | Rôle |
|---|---|---|
| `erp-core` | Java 21, Spring Boot 3.5 (Jakarta EE), Spring Web/Data JPA/Security (JWT), Bean Validation, Flyway, OpenPDF | API métier, journal d'audit, détecteur de seuils, cellule de crise, mode dégradé, REX |
| `erp-db` | PostgreSQL 16 | Schéma et jeu de données créés par Flyway |
| `dashboard` | React 19 + TypeScript, Vite, React Router, TanStack Query, Tailwind CSS 4 + composants shadcn/ui, Recharts ; servi par nginx | Interface ERP + tableau de bord de crise |

Backend en couches : `controller → service → repository`, DTO en `record`, erreurs uniformes (`ApiError`) via
`GlobalExceptionHandler` (400 / 401 / 403 / 404 / 409 / **423 = mode dégradé** / 500).

```
ISO22361/
├── docker-compose.yml          # 3 services, réseau interne, aucun port publié
├── docker-compose.demo.yml     # soutenance : dashboard sur 127.0.0.1:8501 uniquement
├── .env.example                # secrets et drapeau d'exercice
├── scripts/                    # backup.sh, restore.sh, poste-test.sh
├── docs/                       # correspondance ISO, scénario, politique de sauvegarde
├── erp-core/                   # backend (ma.atelier.erp.{config,security,common,metier,crise,supervision,rex,exercice})
│   └── src/main/resources/db/migration/   # V1__schema.sql, V2__donnees_fictives.sql
└── dashboard/                  # frontend (src/{api,auth,components,features})
```

## Prérequis

- Docker ≥ 24 et Docker Compose v2 (Docker Desktop sur macOS / Windows).
- Accès Internet **au moment du build uniquement** (images de base, dépendances Maven et npm). À l'exécution, rien ne sort du réseau `erp-lab`.
- Facultatif, pour développer hors conteneurs : JDK 21 + Maven 3.9, Node.js ≥ 20.19.

## Lancement

```bash
cp .env.example .env
# Générer des secrets propres au lab :
sed -i.bak "s|^JWT_SECRET=.*|JWT_SECRET=$(openssl rand -base64 48)|; s|^POSTGRES_PASSWORD=.*|POSTGRES_PASSWORD=$(openssl rand -hex 16)|" .env && rm .env.bak

# Mode lab (aucun port exposé ; accès depuis un conteneur du réseau erp-lab) :
docker compose up -d --build

# Mode soutenance sur un poste unique (dashboard sur http://localhost:8501, boucle locale seulement) :
docker compose -f docker-compose.yml -f docker-compose.demo.yml up -d --build
```

Au premier démarrage, Flyway crée le schéma et le jeu de données fictif (16 patients, admissions, prescriptions,
10 comptes). `docker compose ps` doit montrer les trois services `healthy` (≈ 1 min).

Arrêt : `docker compose down` (les volumes sont conservés) — remise à zéro complète : `docker compose down -v`.

### Sous Windows

1. Installer **Docker Desktop** (backend **WSL 2**, activé par défaut) et le démarrer ; vérifier : `docker compose version`.
2. Ouvrir un terminal **Ubuntu (WSL)** — recommandé — ou **Git Bash** (fourni avec Git for Windows) : les scripts sont en bash.
3. Copier le projet **dans le système de fichiers WSL** (plus rapide, fins de ligne préservées) :
   ```bash
   cp -r /mnt/c/Users/<vous>/Desktop/ISO22361 ~/ && cd ~/ISO22361
   ```
4. Suivre ensuite exactement les étapes ci-dessus (`cp .env.example .env`, génération des secrets, `docker compose … up -d --build`), puis ouvrir http://localhost:8501 dans le navigateur Windows.

Si le projet arrive par `git clone`, le fichier `.gitattributes` impose des fins de ligne LF. En cas d'erreur
`$'\r': command not found`, convertir les scripts : `sed -i 's/\r$//' scripts/*.sh`.
Dans Git Bash, préfixer les commandes `docker run -v …` par `MSYS_NO_PATHCONV=1` (les scripts du projet le font déjà).

## Comptes de démonstration

Mot de passe commun : **`Lab2026!`** (données fictives, à ne jamais réutiliser ailleurs).

| Identifiant | Rôle | Accès principal |
|---|---|---|
| `pilote` | Pilote de crise | Tableau de crise : active/clôture la crise, décisions, mode dégradé, seuils, REX |
| `tech` | Responsable technique | Tableau de crise : mode dégradé, seuils, référence d'intégrité |
| `com` | Responsable communication | Tableau de crise et REX (lecture) |
| `secretaire` | Secrétaire de crise | Tableau de crise : saisie des décisions, REX |
| `dr.benali`, `dr.ouazzani` | Médecin | Patients, dossiers médicaux (lecture/écriture), admissions, prescriptions |
| `inf.amrani` | Infirmière | Patients, dossiers, admissions, prescriptions (lecture) |
| `adm.idrissi` | Agent d'admission | Patients et admissions (écriture), pas de dossier médical |
| `pharma.tazi` | Pharmacienne | Prescriptions (lecture), patients |
| `admin` | Administrateur SI / animateur d'exercice | Utilisateurs et rôles, tableau de crise, simulateur (si activé) |

Les droits sont appliqués **côté serveur** (`@PreAuthorize`, classe `security/Roles`) ; le front ne fait que masquer ce qui est interdit.

## Fonctionnalités

### Métier
CRUD patients (avec ouverture automatique du dossier médical), dossiers médicaux, admissions (une admission en cours
rend le patient hospitalisé), prescriptions, gestion des utilisateurs et des rôles. Chaque action sensible est journalisée.

### Gestion de crise (cœur du projet)

1. **Journal d'audit** — toute action sensible produit une entrée au format du contrat 2.1 :
   `{ timestamp, level (INFO|WARNING|CRITICAL), event_type, source, user, message, details }`.
   Écrite en base (transaction indépendante : un refus reste tracé) **et** dans `events.log` (volume partagé).
   Si la base est indisponible, l'entrée est écrite dans le fichier puis rejouée en base à son retour.
2. **Détecteur de seuils** (livrable 1.3), fenêtres glissantes, seuils configurables :

   | Signal | Seuil par défaut | Niveau | Conséquence |
   |---|---|---|---|
   | Échecs d'authentification | > 20 en 60 s (modifiable à chaud dans le dashboard) | WARNING | Alerte équipe technique |
   | Indisponibilité de la base / ERP | > 120 s (`SEUIL_INDISPO_SECONDES`) | WARNING | Ouverture d'incident |
   | Altération de fichiers surveillés (SHA-256) | ≥ 1 fichier (`SEUIL_ALTERATIONS`) | CRITICAL | Activation de la cellule |
   | Consultations de dossiers par un même compte | > 50 en 60 s (`SEUIL_ACCES_MASSE`) | CRITICAL | Activation + investigation |
3. **Cellule de crise** — activée automatiquement par tout événement CRITICAL (ou manuellement par le pilote) ;
   registre de **décisions horodatées** ; clôture par le pilote. Une seule cellule active à la fois (contrainte en base).
4. **Mode dégradé** — bascule des dossiers des patients hospitalisés en **lecture seule** : consultation garantie
   (vue *Dossiers hospitalisés*), toute écriture refusée en HTTP 423 et tracée. Pendant une crise, la bascule est
   aussi consignée comme décision de la cellule.
5. **REX** — rapport post-crise calculé depuis le journal : chronologie (rafales regroupées), temps de détection /
   d'activation / de 1re décision, durée de crise et de mode dégradé, décisions, constats automatiques + leçons de
   l'équipe ; export **PDF** ou **JSON**.

### Tableau de bord
Polling conforme au contrat 2.4 : `/health` toutes les **5 s**, `/events` toutes les **2 s** (incrémental).
Situation (NORMAL / ALERTE / CRISE), indicateurs, histogramme des événements par minute et par niveau, timeline,
santé de l'ERP (durée d'indisponibilité mesurée côté dashboard), décisions, mode dégradé, jauges de seuils,
intégrité. Thème clair/sombre ; les niveaux sont toujours donnés par icône + libellé, jamais par la couleur seule.

## API

| Méthode | Chemin | Accès | Rôle |
|---|---|---|---|
| GET | `/health` | public | État de santé : `status` UP/DOWN (HTTP 200/503), base, latence, mode dégradé, crise active |
| GET | `/events?apresId=&limite=` | cellule + admin | Flux du journal au format du contrat 2.1 (plus récent d'abord) |
| GET | `/status/degraded` | connecté | État du mode dégradé |
| POST | `/status/degraded` `{actif, motif}` | pilote, tech | Activer / désactiver le mode dégradé |
| POST | `/api/auth/login` · `/logout` · GET `/me` | — | Authentification JWT (HS256) |
| CRUD | `/api/patients`, `/api/patients/{id}/dossier`, `/api/dossiers/hospitalises`, `/api/admissions`, `/api/prescriptions`, `/api/utilisateurs`, `/api/roles` | selon rôle | Métier |
| GET | `/api/crise/situation`, `/cellules`, `/cellules/active`, `/cellules/{id}/decisions`, `/evenements`, `/detecteur`, `/integrite` | cellule + admin | Conscience partagée |
| POST | `/api/crise/cellules/activer`, `/cellules/{id}/cloturer` | pilote | Cycle de vie de la crise |
| POST | `/api/crise/cellules/{id}/decisions` | pilote, secrétaire | Décision horodatée |
| PUT | `/api/crise/detecteur` | admin, pilote, tech | Seuils |
| POST | `/api/crise/integrite/reference` | admin, tech | Nouvelle référence d'intégrité |
| GET / POST | `/api/rex/{celluleId}` (`?format=pdf\|json`, corps `{lecons: []}`) | cellule + admin | REX |
| POST | `/api/exercice/*` | admin, **si drapeau activé** | Simulateur d'exercice |

Le journal brut est aussi exposé en lecture seule par le dashboard : `GET /logs/events.log` (NDJSON, alternative fichier du contrat 2.4).

## Scénario de soutenance

Détail, variantes et « faiblesse contrôlée » : **[docs/scenario-exercice.md](docs/scenario-exercice.md)**.

```
incident → franchissement du seuil → activation de la cellule → mode dégradé → décisions → rétablissement → REX
```

1. **Préparer** : `./scripts/backup.sh avant-exercice`. Se connecter en `pilote`, `tech`, `secretaire` (tableau de crise) et `dr.benali` (fiche d'un patient hospitalisé).
2. **Incident** : `./scripts/poste-test.sh echecs-auth` → pastille **ALERTE**, jauge 21/20, `auth_failure_threshold` (WARNING).
3. **Franchissement du seuil de crise** : `./scripts/poste-test.sh integrite` → sous 15 s, `file_integrity` (CRITICAL).
4. **Activation de la cellule** : automatique → bandeau **CRISE**, tuiles *temps de détection* / *temps d'activation*.
5. **Mode dégradé** : `tech` l'active avec un motif → bandeau jaune partout ; `dr.benali` voit son dossier en lecture seule (écriture → 423).
6. **Décisions** : `secretaire` consigne les décisions du pilote, du technique et de la communication.
7. **Rétablissement** : `./scripts/restore.sh` (dashboard DOWN ~10 s puis UP, `database_restore`) ; `./scripts/poste-test.sh retablir-documents` ; `pilote` repasse en mode nominal puis **clôture** la crise.
8. **REX** : page *Retour d'expérience* → saisir les leçons → **Exporter en PDF**.

## Correspondance ISO 22361

| Fonctionnalité | Clause | Pourquoi |
|---|---|---|
| Détecteur de seuils, sonde de disponibilité, contrôle d'intégrité | **5.3.1** Anticipation · principe C | Détecter les signaux faibles, qualifier incident → crise |
| Seuils issus de l'analyse de risques (rançongiciel retenu) | **5.3.2** Appréciation · **5.3.3** Prévention | Les seuils traduisent l'appréciation des risques |
| Journal d'audit JSON horodaté | **5.3.4.3** Gestion de l'information | Information fiable, horodatée, non perdue |
| Tableau de bord temps réel | **5.3.4.4** Conscience partagée · principe D | Même image de la situation pour toute la CDC |
| Rôles de crise et droits | **5.2** Cadre · **5.3.4** Composition de la CDC · principe A | Structure et chaîne d'autorité claires |
| Activation automatique / manuelle de la cellule | **5.3.5** Réponse · **5.3.4.2** Plan générique | Répondre aussi aux crises hors scénario |
| Registre des décisions horodatées | **Art. 7** Prise de décision | Traçabilité de la décision |
| Décisions de communication (rôle communication) | **Art. 8** Communication | Messages décidés et tracés |
| Mode dégradé lecture seule | **5.3.5** Réponse · principe F | Continuité des soins, sécurité des patients |
| Sauvegarde / restauration, clôture | **5.3.6** Rétablissement | Retour à la normale sur base saine |
| REX (PDF / JSON) | **5.3.7** Amélioration continue · **9.6** Évaluation | Apprendre de l'exercice |
| Simulateur d'exercice | **Art. 9** Validation par l'exercice | Exercice réaliste sans attaque réelle |

Version détaillée (fichiers de code concernés) : [docs/correspondance-iso22361.md](docs/correspondance-iso22361.md).

## Sauvegarde et restauration

```bash
./scripts/backup.sh avant-exercice          # pg_dump -Fc + SHA-256 dans le volume erp-backups (7 conservées)
./scripts/restore.sh                        # dernière sauvegarde, données métier seulement (traces de crise conservées)
./scripts/restore.sh latest complet         # restauration totale
```

RPO < 24 h (sauvegarde quotidienne), restauration < 30 min (≈ 10 s mesurées), RTO < 4 h.
Politique complète : [docs/politique-sauvegarde.md](docs/politique-sauvegarde.md).

## Développement et tests

```bash
# Tests unitaires de la logique de crise (détecteur, cellule, mode dégradé, REX) — sans JDK local :
docker run --rm -v "$PWD/erp-core":/src -v erp-m2:/root/.m2 -w /src maven:3.9-eclipse-temurin-21 mvn test
# ou, avec JDK 21 + Maven : cd erp-core && mvn test

# Frontend en mode développement (proxy Vite vers un erp-core accessible sur localhost:8000) :
cd dashboard && npm install && npm run dev
npm run typecheck && npm run build
```

Couverture des tests (20 tests, JUnit 5 + Mockito, horloge injectable) :
- `DetecteurDeSeuilServiceTest` — seuil strict (> 20), fenêtre glissante, pas de rafale d'alertes, intégrité → CRITICAL → Evenement persisté + cellule sollicitée, indisponibilité ≥ 2 min, accès en masse ;
- `CelluleDeCriseServiceTest` — activation automatique sur CRITICAL seulement, rattachement à une crise en cours, décisions horodatées (refusées après clôture), bascule du mode dégradé consignée comme décision, clôture unique ;
- `ModeDegradeServiceTest` — lecture seule limitée aux hospitalisés, refus tracé, bascule idempotente et tracée ;
- `RexServiceTest` — regroupement de la chronologie, constats automatiques.

## Écarts assumés par rapport au diagramme

Le modèle suit `classeDiagram.png` (classes, attributs, types, cardinalités, méthodes `evaluer()`, `activer()`,
`cloturer()`, `estHospitalise()`, `authentifier()`, `enregistrer()`). Écarts validés par l'équipe :

| Point | Choix | Raison |
|---|---|---|
| `Utilisateur 1 → 1 Role` | chaque utilisateur a exactement un rôle ; un rôle est partagé (`@ManyToOne`) | Un rôle réservé à un seul utilisateur n'aurait pas de sens métier |
| `JournalAudit` sans `details` | colonne `details jsonb` ajoutée ; `user` = association « genere » (nullable) | Exigé par le contrat d'interface 2.1 |
| `ModeDegrade`, `DetecteurDeSeuil` sans `id` | singletons à clé technique `id = 1` | Une table relationnelle exige une clé |
| `DetecteurDeSeuil` ne porte qu'un seuil | seuil d'auth. + fenêtre dans l'entité ; autres seuils du livrable 1.3 en configuration | Respect des attributs du diagramme |
| `Evenement * → 1 JournalAudit` | FK `journal_audit_id` : l'entrée de journal qui trace l'événement | Lecture de l'association « trace » |
| `Evenement ⇢ CelluleDeCrise` | dépendance sans FK ; l'événement déclencheur est noté dans le journal d'activation | Une dépendance UML n'est pas une association |
| `JournalAudit.enregistrer(evt)` | porté par `JournalAuditService` (l'entité est immuable) | Séparation entité / service |

## Copyright

© 2026 Équipe projet n°3 — ENSA (Malak, Hiba, Yao Landry, Mounir Merghiche). **Tous droits réservés.**

Toute reproduction, modification ou diffusion, totale ou partielle, est interdite sans l'accord écrit des auteurs,
hors consultation et évaluation par les enseignants et le jury de l'atelier. Voir le fichier [LICENSE](LICENSE).
Les composants tiers (Spring Boot, React, PostgreSQL, etc.) restent soumis à leurs propres licences.
