/* © 2026 Équipe projet n°3 — ENSA. Tous droits réservés. Voir le fichier LICENSE. */
package ma.atelier.erp.rex;

import ma.atelier.erp.crise.dto.CriseDtos.CelluleDto;
import ma.atelier.erp.crise.dto.CriseDtos.DecisionDto;
import ma.atelier.erp.crise.entity.Niveau;

import java.time.Instant;
import java.util.List;

/** Rapport de retour d'expérience (ISO 22361 5.3.7 et 9.6). */
public record RapportRexDto(
        Instant genereLe,
        String generePar,
        CelluleDto cellule,
        IndicateursDto indicateurs,
        List<LigneChronologie> chronologie,
        List<DecisionDto> decisions,
        List<String> leconsAutomatiques,
        List<String> leconsEquipe) {

    /**
     * Ligne de chronologie ; les entrées consécutives identiques (ex. rafale d'échecs
     * d'authentification) sont regroupées ({@code occurrences} > 1).
     */
    public record LigneChronologie(Instant debut, Instant fin, Niveau niveau, String type, String source,
                                   String user, String message, int occurrences) {}

    public record LeconsRequest(List<String> lecons) {}
}
