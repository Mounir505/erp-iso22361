/* © 2026 Équipe projet n°3 — ENSA. Tous droits réservés. Voir le fichier LICENSE. */
package ma.atelier.erp.metier.dto;

import ma.atelier.erp.metier.entity.Prescription;

import java.time.Instant;

public record PrescriptionDto(Integer id, Integer admissionId, Integer patientId, String patientNom,
                              String medicament, String posologie, Instant date) {

    public static PrescriptionDto de(Prescription p) {
        var patient = p.getAdmission().getPatient();
        return new PrescriptionDto(p.getId(), p.getAdmission().getId(), patient.getId(), patient.getNom(),
                p.getMedicament(), p.getPosologie(), p.getDate());
    }
}
