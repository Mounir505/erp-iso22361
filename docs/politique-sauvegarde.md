# Politique de sauvegarde et de restauration

Référence : livrable 3.2 (Yao Landry) et livrable 1.5 (RTO / RPO). Clause ISO 22361 **5.3.6** (rétablissement).

## Paramètres

| Paramètre | Valeur | Mise en œuvre |
|---|---|---|
| Fréquence | Sauvegarde complète toutes les 24 h **et** avant chaque exercice | `scripts/backup.sh quotidienne` en cron sur l'hôte du lab ; `scripts/backup.sh avant-exercice` |
| RPO (perte max.) | < 24 h | Découle de la fréquence quotidienne |
| Emplacement | Volume Docker `erp-backups`, **monté uniquement dans `erp-db`** | Le conteneur applicatif `erp-core` n'y a pas accès (un attaquant qui compromettrait l'application ne pourrait pas chiffrer les sauvegardes) |
| Format | `pg_dump -Fc` (compressé, restaurable table par table) + empreinte `.sha256` | Vérifié par `pg_restore --list` à la création, `sha256sum -c` à la restauration |
| Rétention | 7 dernières sauvegardes | Purge automatique dans `backup.sh` |
| Objectif de restauration | < 30 min (mesuré : ~10 s sur le jeu de données du lab) | Durée affichée et journalisée par `restore.sh` |
| RTO (reprise du service) | < 4 h | Indicateur « durée de crise » du dashboard et constat automatique du REX |

Planification (hôte du lab) :

```cron
0 2 * * * /chemin/vers/ISO22361/scripts/backup.sh quotidienne >> /var/log/erp-backup.log 2>&1
```

## Restauration

```bash
./scripts/restore.sh                       # dernière sauvegarde, mode « metier »
./scripts/restore.sh <fichier.dump> metier
./scripts/restore.sh latest complet        # sinistre total
```

| Mode | Tables restaurées | Tables conservées | Usage |
|---|---|---|---|
| `metier` (défaut) | `patient`, `dossier_medical`, `admission`, `prescription` | comptes, rôles, **journal d'audit, événements, cellule, décisions, mode dégradé** | Données vitales altérées (scénario rançongiciel). Les traces de la crise sont des **preuves** : elles doivent survivre à la restauration pour le REX. |
| `complet` | toutes | aucune | Perte totale de la base. Les traces postérieures à la sauvegarde ne subsistent que dans `events.log` (volume `erp-logs`). |

Déroulé du script : vérification SHA-256 → arrêt d'`erp-core` (fenêtre de maintenance) → restauration **dans une seule transaction** (en cas d'erreur, rien n'est modifié) → recalage des séquences → entrée `database_restore` dans le journal → redémarrage et attente de l'état `healthy`. Un `trap` garantit le redémarrage de l'application même si la restauration échoue.

Après restauration des documents surveillés, le responsable technique fixe une nouvelle référence d'intégrité (dashboard → *Détecteur de seuils* → *Fixer une nouvelle référence*).

## Test de la politique

La restauration est testée à chaque exercice (étape 7 du scénario) ; la durée et le mode sont conservés dans le journal (`event_type = database_restore`) et apparaissent dans la chronologie du REX.
