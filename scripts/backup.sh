#!/usr/bin/env bash
# © 2026 Équipe projet n°3 — ENSA. Tous droits réservés. Voir le fichier LICENSE.
# =============================================================================
# Sauvegarde complète de la base (livrable 3.2) — lab ISO 22361, données fictives.
#   * format custom pg_dump (-Fc), compressé, restaurable table par table ;
#   * stockée dans le volume « erp-backups », monté UNIQUEMENT dans erp-db
#     (hors de portée du conteneur applicatif erp-core) ;
#   * empreinte SHA-256 + vérification de lisibilité (pg_restore --list) ;
#   * rétention : 7 dernières sauvegardes ;
#   * l'opération est tracée dans le journal d'audit (event_type database_backup).
# Usage : ./scripts/backup.sh [libellé]      ex. ./scripts/backup.sh avant-exercice
# Planification 24 h (hôte du lab) : 0 2 * * * /chemin/ISO22361/scripts/backup.sh quotidienne
# =============================================================================
set -euo pipefail
# Windows (Git Bash) : empêche la conversion des chemins « /backups/... » en « C:/Program Files/Git/... »
export MSYS_NO_PATHCONV=1
cd "$(dirname "$0")/.."

LIBELLE="${1:-manuelle}"
HORODATAGE="$(date -u +%Y%m%dT%H%M%SZ)"
FICHIER="/backups/erp-${HORODATAGE}-${LIBELLE}.dump"
RETENTION=7

db() { docker compose exec -T erp-db sh -c "$1"; }

echo "▶ Sauvegarde de la base vers ${FICHIER}"
DEBUT=$(date +%s)
db "pg_dump -U \"\$POSTGRES_USER\" -d \"\$POSTGRES_DB\" -Fc -f '${FICHIER}'"
db "pg_restore --list '${FICHIER}' > /dev/null"                 # le fichier est-il exploitable ?
db "cd /backups && sha256sum '$(basename "$FICHIER")' > '$(basename "$FICHIER").sha256'"
TAILLE=$(db "du -h '${FICHIER}' | cut -f1")
DUREE=$(( $(date +%s) - DEBUT ))

# Rétention : on ne garde que les N plus récentes
db "ls -1t /backups/erp-*.dump 2>/dev/null | tail -n +$((RETENTION + 1)) | while read f; do rm -f \"\$f\" \"\$f.sha256\"; done"

# Traçabilité dans le journal d'audit (source : script, acteur : null)
db "psql -q -U \"\$POSTGRES_USER\" -d \"\$POSTGRES_DB\" -c \"INSERT INTO journal_audit (timestamp, niveau, type_evenement, source, message, utilisateur_id, details) VALUES (now(), 'INFO', 'database_backup', 'scripts/backup.sh', 'Sauvegarde complète de la base', NULL, jsonb_build_object('fichier', '${FICHIER}', 'libelle', '${LIBELLE}', 'taille', '${TAILLE}', 'dureeSecondes', ${DUREE}));\""

echo "✔ Sauvegarde terminée en ${DUREE} s (${TAILLE})"
echo "  Sauvegardes disponibles :"
db "ls -1t /backups/*.dump | sed 's/^/    /'"
