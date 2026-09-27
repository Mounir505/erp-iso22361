/* © 2026 Équipe projet n°3 — ENSA. Tous droits réservés. Voir le fichier LICENSE. */
package ma.atelier.erp.crise.repository;

import ma.atelier.erp.crise.entity.CelluleDeCrise;
import ma.atelier.erp.crise.entity.StatutCellule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CelluleDeCriseRepository extends JpaRepository<CelluleDeCrise, Integer> {

    Optional<CelluleDeCrise> findFirstByStatut(StatutCellule statut);

    List<CelluleDeCrise> findAllByOrderByDateActivationDesc();
}
