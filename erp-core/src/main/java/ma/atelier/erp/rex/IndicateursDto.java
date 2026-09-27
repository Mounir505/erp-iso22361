/* © 2026 Équipe projet n°3 — ENSA. Tous droits réservés. Voir le fichier LICENSE. */
package ma.atelier.erp.rex;

import java.time.Instant;

/**
 * Indicateurs temporels d'une crise (dashboard + REX).
 *
 * @param premierSignal              premier signal suspect (WARNING+) précédant le déclenchement
 * @param declenchement              Evenement CRITICAL déclencheur (ou activation manuelle)
 * @param tempsDetectionSecondes     premierSignal → déclenchement
 * @param tempsActivationMs          déclenchement → activation de la cellule
 * @param tempsPremiereDecisionSecondes activation → première décision (null si aucune)
 * @param dureeCriseSecondes         activation → clôture (ou maintenant si en cours)
 * @param dureeModeDegradeSecondes   cumul du temps passé en mode dégradé pendant la crise
 */
public record IndicateursDto(
        Instant premierSignal,
        Instant declenchement,
        Instant activation,
        Instant premiereDecision,
        Instant cloture,
        String typeDeclenchement,
        long tempsDetectionSecondes,
        long tempsActivationMs,
        Long tempsPremiereDecisionSecondes,
        long dureeCriseSecondes,
        long dureeModeDegradeSecondes,
        int nombreDecisions,
        boolean enCours) {}
