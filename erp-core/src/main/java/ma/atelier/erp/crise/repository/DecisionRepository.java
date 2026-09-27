/* © 2026 Équipe projet n°3 — ENSA. Tous droits réservés. Voir le fichier LICENSE. */
package ma.atelier.erp.crise.repository;

import ma.atelier.erp.crise.entity.Decision;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DecisionRepository extends JpaRepository<Decision, Integer> {

    List<Decision> findByCelluleIdOrderByTimestampAsc(Integer celluleId);
}
