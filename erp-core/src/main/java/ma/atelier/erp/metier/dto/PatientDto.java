/* © 2026 Équipe projet n°3 — ENSA. Tous droits réservés. Voir le fichier LICENSE. */
package ma.atelier.erp.metier.dto;

import ma.atelier.erp.metier.entity.Patient;
import ma.atelier.erp.metier.entity.StatutPatient;

import java.time.LocalDate;

public record PatientDto(Integer id, String nom, LocalDate dateNaissance, StatutPatient statut, boolean hospitalise) {

    public static PatientDto de(Patient p) {
        return new PatientDto(p.getId(), p.getNom(), p.getDateNaissance(), p.getStatut(), p.estHospitalise());
    }
}
