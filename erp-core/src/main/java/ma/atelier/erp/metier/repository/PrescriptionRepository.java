/* © 2026 Équipe projet n°3 — ENSA. Tous droits réservés. Voir le fichier LICENSE. */
package ma.atelier.erp.metier.repository;

import ma.atelier.erp.metier.entity.Prescription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PrescriptionRepository extends JpaRepository<Prescription, Integer> {

    List<Prescription> findAllByOrderByDateDesc();

    List<Prescription> findByAdmissionIdOrderByDateDesc(Integer admissionId);
}
