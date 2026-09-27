/* © 2026 Équipe projet n°3 — ENSA. Tous droits réservés. Voir le fichier LICENSE. */
package ma.atelier.erp.metier.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/** @param date facultative : maintenant par défaut */
public record PrescriptionRequest(
        @NotNull Integer admissionId,
        @NotBlank @Size(max = 120) String medicament,
        @NotBlank @Size(max = 200) String posologie,
        Instant date) {}
