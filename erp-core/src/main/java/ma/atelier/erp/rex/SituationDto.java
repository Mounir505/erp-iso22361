/* © 2026 Équipe projet n°3 — ENSA. Tous droits réservés. Voir le fichier LICENSE. */
package ma.atelier.erp.rex;

import ma.atelier.erp.crise.dto.CriseDtos.CelluleDto;
import ma.atelier.erp.crise.dto.EtatDetecteurDto;

/**
 * Situation globale pour le dashboard (conscience partagée, ISO 22361 5.3.4.4).
 *
 * @param niveau NORMAL | ALERTE (signal WARNING récent) | CRISE (cellule active)
 */
public record SituationDto(
        String niveau,
        CelluleDto celluleActive,
        IndicateursDto indicateurs,
        IndicateursDto derniereCrise,
        boolean modeDegradeActif,
        EtatDetecteurDto detecteur,
        long evenementsWarning24h,
        long evenementsCritical24h) {}
