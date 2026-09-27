/* © 2026 Équipe projet n°3 — ENSA. Tous droits réservés. Voir le fichier LICENSE. */
package ma.atelier.erp.metier.dto;

import ma.atelier.erp.metier.entity.Admission;
import ma.atelier.erp.metier.entity.StatutAdmission;

import java.time.Instant;

public record AdmissionDto(Integer id, Integer patientId, String patientNom, Instant dateEntree, String service,
                           StatutAdmission statut) {

    public static AdmissionDto de(Admission a) {
        return new AdmissionDto(a.getId(), a.getPatient().getId(), a.getPatient().getNom(), a.getDateEntree(),
                a.getService(), a.getStatut());
    }
}
