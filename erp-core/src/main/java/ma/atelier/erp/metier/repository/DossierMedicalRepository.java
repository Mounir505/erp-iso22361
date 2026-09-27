/* © 2026 Équipe projet n°3 — ENSA. Tous droits réservés. Voir le fichier LICENSE. */
package ma.atelier.erp.metier.repository;

import ma.atelier.erp.metier.entity.DossierMedical;
import ma.atelier.erp.metier.entity.StatutPatient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DossierMedicalRepository extends JpaRepository<DossierMedical, Integer> {

    Optional<DossierMedical> findByPatientId(Integer patientId);

    List<DossierMedical> findByPatientStatutOrderByPatientNom(StatutPatient statut);
}
