/* © 2026 Équipe projet n°3 — ENSA. Tous droits réservés. Voir le fichier LICENSE. */
package ma.atelier.erp.metier.repository;

import ma.atelier.erp.metier.entity.Patient;
import ma.atelier.erp.metier.entity.StatutPatient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PatientRepository extends JpaRepository<Patient, Integer> {

    /** Recherche par nom (insensible à la casse) et/ou statut ; paramètres nuls ignorés. */
    @Query("""
            select p from Patient p
            where (:q is null or lower(p.nom) like lower(concat('%', cast(:q as string), '%')))
              and (:statut is null or p.statut = :statut)
            order by p.nom""")
    List<Patient> rechercher(@Param("q") String q, @Param("statut") StatutPatient statut);

    long countByStatut(StatutPatient statut);
}
