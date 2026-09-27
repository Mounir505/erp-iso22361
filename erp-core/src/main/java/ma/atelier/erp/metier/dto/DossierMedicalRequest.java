/* © 2026 Équipe projet n°3 — ENSA. Tous droits réservés. Voir le fichier LICENSE. */
package ma.atelier.erp.metier.dto;

import jakarta.validation.constraints.Size;

public record DossierMedicalRequest(@Size(max = 10_000) String antecedents,
                                    @Size(max = 10_000) String observations) {}
