#!/usr/bin/env bash
# © 2026 Équipe projet n°3 — ENSA. Tous droits réservés. Voir le fichier LICENSE.
# =============================================================================
# « Poste de test » du lab (172.16.10.30, livrable 3.1) — déroulement de l'exercice.
# Produit de VRAIS signaux par l'usage normal de l'API, sans outil offensif :
#   echecs-auth   : 25 tentatives de connexion avec un mauvais mot de passe  → WARNING
#   acces-masse   : 55 consultations de dossiers par un compte médecin      → CRITICAL
#   integrite     : modifie un document FICTIF surveillé (via docker exec)   → CRITICAL (≤ 15 s)
#   sante         : interroge GET /health
# Les requêtes partent d'un conteneur éphémère sur le réseau isolé erp-lab.
# Usage : ./scripts/poste-test.sh <echecs-auth|acces-masse|integrite|sante>
# =============================================================================
set -euo pipefail
# Windows (Git Bash) : empêche la conversion des chemins « /backups/... » en « C:/Program Files/Git/... »
export MSYS_NO_PATHCONV=1
cd "$(dirname "$0")/.."

poste() { docker run --rm --network erp-lab --ip 172.16.10.30 curlimages/curl sh -c "$1"; }
B=http://erp-core:8000

case "${1:-}" in
  echecs-auth)
    echo "▶ 25 échecs d'authentification depuis 172.16.10.30"
    poste "for i in \$(seq 1 25); do curl -s -o /dev/null -w '%{http_code} ' -X POST $B/api/auth/login -H 'Content-Type: application/json' -d '{\"login\":\"intrus\",\"motDePasse\":\"essai'\$i'\"}'; done; echo"
    ;;
  acces-masse)
    echo "▶ 55 consultations de dossiers en rafale (compte dr.ouazzani)"
    poste "T=\$(curl -s -X POST $B/api/auth/login -H 'Content-Type: application/json' -d '{\"login\":\"dr.ouazzani\",\"motDePasse\":\"Lab2026!\"}' | sed -n 's/.*\"token\":\"\\([^\"]*\\)\".*/\\1/p');
           for i in \$(seq 1 55); do curl -s -o /dev/null -H \"Authorization: Bearer \$T\" $B/api/patients/\$(( i % 16 + 1 ))/dossier; done; echo termine"
    ;;
  integrite)
    echo "▶ Altération d'un document fictif surveillé (détection au prochain cycle, ≤ 15 s)"
    docker compose exec -T erp-core sh -c 'echo "ALTERATION EXERCICE $(date -u)" >> /var/lib/erp/documents/compte-rendu-01.txt && mv /var/lib/erp/documents/compte-rendu-02.txt /var/lib/erp/documents/compte-rendu-02.txt.locked'
    ;;
  retablir-documents)
    echo "▶ Remise en état des documents fictifs (rétablissement)"
    docker compose exec -T erp-core sh -c 'cd /var/lib/erp/documents && [ -f compte-rendu-02.txt.locked ] && mv compte-rendu-02.txt.locked compte-rendu-02.txt; sed -i "/ALTERATION EXERCICE/d" compte-rendu-01.txt; echo ok'
    ;;
  sante)
    poste "curl -s -w '\n' $B/health"
    ;;
  *)
    sed -n '2,13p' "$0"; exit 1 ;;
esac
