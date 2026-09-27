/* © 2026 Équipe projet n°3 — ENSA. Tous droits réservés. Voir le fichier LICENSE. */
package ma.atelier.erp.crise.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import ma.atelier.erp.crise.entity.*;

import java.time.Instant;

/** DTO de la gestion de crise. */
public final class CriseDtos {

    private CriseDtos() {}

    public record CelluleDto(Integer id, StatutCellule statut, Instant dateActivation) {
        public static CelluleDto de(CelluleDeCrise c) {
            return new CelluleDto(c.getId(), c.getStatut(), c.getDateActivation());
        }
    }

    public record DecisionDto(Integer id, Integer celluleId, String libelle, String auteur, Instant timestamp) {
        public static DecisionDto de(Decision d) {
            return new DecisionDto(d.getId(), d.getCellule().getId(), d.getLibelle(), d.getAuteur(), d.getTimestamp());
        }
    }

    /** @param auteur facultatif : par défaut, l'utilisateur connecté */
    public record DecisionRequest(@NotBlank @Size(max = 2000) String libelle, @Size(max = 100) String auteur) {}

    public record MotifRequest(@Size(max = 500) String motif) {}

    public record EvenementDto(Integer id, Niveau niveau, String type, Instant timestamp, Integer journalId) {
        public static EvenementDto de(Evenement e) {
            return new EvenementDto(e.getId(), e.getNiveau(), e.getType(), e.getTimestamp(), e.getJournal().getId());
        }
    }

    public record SeuilsRequest(@Min(1) @Max(10_000) int seuilEchecsAuth, @Min(5) @Max(86_400) int fenetre) {}

    public record ModeDegradeDto(boolean actif, boolean lectureSeule, String perimetre, long patientsProteges) {}

    public record ModeDegradeRequest(boolean actif, @Size(max = 500) String motif) {}
}
