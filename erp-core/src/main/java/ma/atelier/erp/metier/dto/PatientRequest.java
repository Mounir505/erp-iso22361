/* © 2026 Équipe projet n°3 — ENSA. Tous droits réservés. Voir le fichier LICENSE. */
package ma.atelier.erp.metier.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import ma.atelier.erp.metier.entity.StatutPatient;

import java.time.LocalDate;

public record PatientRequest(
        @NotBlank @Size(max = 100) String nom,
        @NotNull @Past LocalDate dateNaissance,
        @NotNull StatutPatient statut) {}
