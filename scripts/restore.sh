#!/usr/bin/env bash
# © 2026 Équipe projet n°3 — ENSA. Tous droits réservés. Voir le fichier LICENSE.
# =============================================================================
# Restauration de la base — phase de RÉTABLISSEMENT (ISO 22361 5.3.6, livrable 3.2).
# Objectif : restauration < 30 min ; RPO < 24 h (dernière sauvegarde).
#
# Deux modes :
#   metier  (défaut) : restaure UNIQUEMENT les données métier (patient, dossier_medical,
#                      admission, prescription). Les comptes, le journal d'audit, les
#                      événements, la cellule de crise et ses décisions sont CONSERVÉS :
#                      ce sont les preuves de la crise, nécessaires au REX (5.3.7 / 9.6).
#   complet          : restaure toute la base (sinistre total). ⚠ Les traces postérieures
#                      à la sauvegarde sont perdues en base (elles restent dans events.log).
#
# Usage : ./scripts/restore.sh [fichier.dump|latest] [metier|complet]
#         ./scripts/restore.sh                    → dernière sauvegarde, mode metier
#         ./scripts/restore.sh erp-20261001T020000Z-quotidienne.dump complet
# =============================================================================
set -euo pipefail
# Windows (Git Bash) : empêche la conversion des chemins « /backups/... » en « C:/Program Files/Git/... »
export MSYS_NO_PATHCONV=1
cd "$(dirname "$0")/.."

CIBLE="${1:-latest}"
MODE="${2:-metier}"
TABLES_METIER="patient dossier_medical admission prescription"

db() { docker compose exec -T erp-db sh -c "$1"; }
psql_db() { db "psql -v ON_ERROR_STOP=1 -q -U \"\$POSTGRES_USER\" -d \"\$POSTGRES_DB\" $1"; }

if [[ "$MODE" != "metier" && "$MODE" != "complet" ]]; then
  echo "Mode inconnu : $MODE (metier|complet)" >&2; exit 1
fi

if [[ "$CIBLE" == "latest" ]]; then
  FICHIER=$(db "ls -1t /backups/erp-*.dump 2>/dev/null | head -n1" | tr -d '\r')
else
  FICHIER="/backups/$(basename "$CIBLE")"
fi
[[ -n "$FICHIER" ]] || { echo "Aucune sauvegarde trouvée dans le volume erp-backups" >&2; exit 1; }

echo "▶ Restauration ($MODE) depuis $FICHIER"
DEBUT=$(date +%s)

echo "  1/5 Vérification de l'empreinte SHA-256"
db "cd /backups && sha256sum -c '$(basename "$FICHIER").sha256'"

echo "  2/5 Arrêt de l'application (fenêtre de maintenance)"
# Quoi qu'il arrive (erreur comprise), l'application est redémarrée en sortie de script
trap 'docker compose start erp-core >/dev/null 2>&1 || true' EXIT
docker compose stop erp-core >/dev/null

echo "  3/5 Restauration des données"
if [[ "$MODE" == "metier" ]]; then
  TABLES_ARGS=""; SETVAL=""
  for t in $TABLES_METIER; do
    TABLES_ARGS="$TABLES_ARGS -t $t"
    # Les séquences ne font pas partie d'une restauration table par table : on les recale
    SETVAL="$SETVAL SELECT setval(pg_get_serial_sequence('public.$t','id'), COALESCE((SELECT max(id) FROM public.$t), 1));"
  done
  # Une SEULE transaction : vidage + rechargement + séquences. En cas d'erreur, rien n'est modifié.
  db "{ echo 'TRUNCATE public.patient, public.dossier_medical, public.admission, public.prescription RESTART IDENTITY CASCADE;';
        pg_restore -f - --data-only --disable-triggers $TABLES_ARGS '$FICHIER';
        echo \"$SETVAL\"; } \
      | psql -v ON_ERROR_STOP=1 -q --single-transaction -U \"\$POSTGRES_USER\" -d \"\$POSTGRES_DB\" >/dev/null"
else
  db "pg_restore -U \"\$POSTGRES_USER\" -d \"\$POSTGRES_DB\" --clean --if-exists --single-transaction '$FICHIER'"
fi

DUREE=$(( $(date +%s) - DEBUT ))
echo "  4/5 Traçabilité dans le journal d'audit"
psql_db "-c \"INSERT INTO journal_audit (timestamp, niveau, type_evenement, source, message, utilisateur_id, details) VALUES (now(), 'WARNING', 'database_restore', 'scripts/restore.sh', 'Restauration de la base (${MODE}) — phase de rétablissement', NULL, jsonb_build_object('fichier', '${FICHIER}', 'mode', '${MODE}', 'dureeSecondes', ${DUREE}, 'objectifSecondes', 1800));\""

echo "  5/5 Redémarrage de l'application"
docker compose start erp-core >/dev/null
for _ in $(seq 1 40); do
  [[ "$(docker inspect -f '{{.State.Health.Status}}' erp-core)" == "healthy" ]] && break
  sleep 3
done

TOTAL=$(( $(date +%s) - DEBUT ))
echo "✔ Restauration terminée en ${TOTAL} s (objectif < 1800 s) — état erp-core : $(docker inspect -f '{{.State.Health.Status}}' erp-core)"
if [[ "$MODE" == "metier" ]]; then
  echo "  Pensez à fixer une nouvelle référence d'intégrité (dashboard → Intégrité) si les documents ont été restaurés."
fi
