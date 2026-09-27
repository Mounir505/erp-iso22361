/* © 2026 Équipe projet n°3 — ENSA. Tous droits réservés. Voir le fichier LICENSE. */
package ma.atelier.erp.metier.repository;

import ma.atelier.erp.metier.entity.Admission;
import ma.atelier.erp.metier.entity.StatutAdmission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AdmissionRepository extends JpaRepository<Admission, Integer> {

    List<Admission> findAllByOrderByDateEntreeDesc();

    List<Admission> findByPatientIdOrderByDateEntreeDesc(Integer patientId);

    boolean existsByPatientIdAndStatutAndIdNot(Integer patientId, StatutAdmission statut, Integer id);
}
