# Scénario d'exercice — rançongiciel sur l'ERP hospitalier

Scénario retenu (livrable 1.2) : **rançongiciel paralysant l'ERP, dossiers patients inaccessibles, menace de divulgation**.
Tout se déroule dans le réseau isolé `erp-lab` avec des données **fictives**. Aucune attaque réelle n'est menée :
les signaux sont produits soit par un usage normal de l'API depuis le poste de test, soit par le simulateur.

## Deux façons de déclencher les signaux

| Signal | Sans drapeau (usage réel de l'API) | Avec `CRISIS_EXERCISE_ENABLED=true` (compte `admin`) |
|---|---|---|
| Échecs d'authentification | `./scripts/poste-test.sh echecs-auth` | Panneau *Animateur d'exercice* → « Rafale d'échecs » |
| Accès en masse aux dossiers | `./scripts/poste-test.sh acces-masse` | « Accès en masse aux dossiers » |
| Altération de fichiers | `./scripts/poste-test.sh integrite` | « Altération de fichiers (rançongiciel) » |
| Indisponibilité > 2 min | `docker compose stop erp-db` (attendre 2 min) | « Panne ERP simulée » |

### La « faiblesse contrôlée »

Le simulateur (`erp-core/src/main/java/ma/atelier/erp/exercice/ExerciceController.java`) est la seule faiblesse volontaire :

- il **n'existe pas** tant que `CRISIS_EXERCISE_ENABLED` vaut `false` (valeur par défaut) : le contrôleur n'est même pas chargé ;
- il est réservé au rôle `ADMIN` (animateur), jamais à la cellule évaluée ; une tentative d'un autre rôle est refusée (403) et tracée ;
- il **n'attaque rien** : il alimente les compteurs du détecteur comme le feraient de vrais signaux ; aucune donnée n'est chiffrée, modifiée ni exfiltrée ;
- chaque injection est journalisée (`exercise_injection`, `WARNING`, `simule: true`) et apparaît dans le REX.

Pour l'activer le temps de l'exercice : `CRISIS_EXERCISE_ENABLED=true` dans `.env`, puis `docker compose up -d erp-core`. Le remettre à `false` ensuite.

## Déroulé pas à pas (≈ 20 min)

Ouvrir trois onglets (ou trois postes) : **pilote**, **tech**, **secretaire** sur le tableau de crise, et un onglet **dr.benali** sur un dossier patient.

| # | Phase | Action | Ce qu'on observe | ISO 22361 |
|---|---|---|---|---|
| 0 | Préparation | `./scripts/backup.sh avant-exercice` | Sauvegarde + entrée `database_backup` | 5.3.4, 5.3.6 |
| 1 | **Incident** | `./scripts/poste-test.sh echecs-auth` | Jauge « échecs d'authentification » → 21/20 ; pastille **ALERTE** orange ; ligne `auth_failure_threshold` (WARNING) | 5.3.1 |
| 2 | **Franchissement du seuil de crise** | `./scripts/poste-test.sh integrite` (ou `acces-masse`) | Sous 15 s : `file_integrity` **CRITICAL** ; panneau Intégrité en rouge | 5.3.1, 5.3.2 |
| 3 | **Activation de la cellule** | automatique | Bandeau **CRISE — cellule n°X activée** ; `crisis_cell_activated` ; tuiles *temps de détection* et *temps d'activation* | 5.3.4, 5.3.5 |
| 4 | **Mode dégradé** | *tech* : motif « Suspicion de rançongiciel » → *Activer le mode dégradé* | Bandeau jaune pour tous ; *dr.benali* voit le dossier en **lecture seule** ; toute tentative d'écriture → 423 + `degraded_mode_write_blocked` ; la vue *Dossiers hospitalisés* reste consultable | 5.3.5, livrable 1.5 |
| 5 | **Décisions** | *secretaire* consigne : « Isoler le serveur ERP du réseau » (pilote) ; « Message d'attente aux services de soins » (com) ; « Restaurer depuis la sauvegarde avant-exercice » (tech) | Registre horodaté ; tuile *1re décision* | Art. 7, Art. 8 |
| 6 | Confinement | (discussion) comptes suspects, isolement | Décisions supplémentaires | 5.3.5 |
| 7 | **Rétablissement** | `./scripts/restore.sh` puis `./scripts/poste-test.sh retablir-documents` ; *tech* : *Fixer une nouvelle référence* si besoin | Santé **DOWN** pendant ~10 s puis **UP** ; `database_restore` ; intégrité « Intègre » | 5.3.6 |
| 8 | Retour à la normale | *pilote* : *Revenir au mode nominal*, puis *Clôturer la crise* | `degraded_mode_off`, `crisis_cell_closed` ; pastille verte | 5.3.6 |
| 9 | **REX** | *Retour d'expérience* → saisir 2-3 leçons → *Exporter en PDF* | Chronologie, indicateurs, décisions, constats automatiques + leçons ; `rex_generated` | 5.3.7, 9.6 |

### Variante « indisponibilité »

`docker compose stop erp-db` : le dashboard passe **DOWN** et affiche « indisponible depuis … ».
Après 2 min, la sonde ouvre un incident `service_unavailable` (écrit d'abord dans `events.log` puis rejoué en base au retour de celle-ci — aucune trace perdue).
`docker compose start erp-db` → `service_restored`.

### Indicateurs attendus

| Indicateur | Définition | Cible |
|---|---|---|
| Temps de détection | 1er signal suspect → franchissement du seuil CRITICAL | quelques secondes à minutes |
| Temps d'activation | seuil CRITICAL → activation de la cellule | < 1 s (automatique) |
| Délai de 1re décision | activation → 1re décision | < 15 min |
| Durée de crise | activation → clôture | < RTO 4 h |
| Restauration | durée du script | < 30 min |
