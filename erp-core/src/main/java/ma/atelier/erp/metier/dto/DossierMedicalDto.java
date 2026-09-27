/* © 2026 Équipe projet n°3 — ENSA. Tous droits réservés. Voir le fichier LICENSE. */
package ma.atelier.erp.metier.dto;

import ma.atelier.erp.metier.entity.DossierMedical;

/**
 * @param lectureSeule vrai si le mode dégradé interdit actuellement la modification de ce dossier
 */
public record DossierMedicalDto(Integer id, Integer patientId, String patientNom, boolean patientHospitalise,
                                String antecedents, String observations, boolean lectureSeule) {

    public static DossierMedicalDto de(DossierMedical d, boolean lectureSeule) {
        return new DossierMedicalDto(d.getId(), d.getPatient().getId(), d.getPatient().getNom(),
                d.getPatient().estHospitalise(), d.getAntecedents(), d.getObservations(), lectureSeule);
    }
}
