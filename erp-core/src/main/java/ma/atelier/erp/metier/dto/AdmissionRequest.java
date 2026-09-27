/* © 2026 Équipe projet n°3 — ENSA. Tous droits réservés. Voir le fichier LICENSE. */
package ma.atelier.erp.metier.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ma.atelier.erp.metier.entity.StatutAdmission;

import java.time.Instant;

/** @param dateEntree facultative : maintenant par défaut */
public record AdmissionRequest(
        @NotNull Integer patientId,
        Instant dateEntree,
        @NotBlank @Size(max = 80) String service,
        @NotNull StatutAdmission statut) {}
