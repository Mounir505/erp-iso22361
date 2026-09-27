/* © 2026 Équipe projet n°3 — ENSA. Tous droits réservés. Voir le fichier LICENSE. */
package ma.atelier.erp.crise.repository;

import ma.atelier.erp.crise.entity.Evenement;
import ma.atelier.erp.crise.entity.Niveau;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface EvenementRepository extends JpaRepository<Evenement, Integer> {

    List<Evenement> findTop100ByOrderByTimestampDesc();

    List<Evenement> findByTimestampBetweenOrderByTimestampAsc(Instant debut, Instant fin);

    List<Evenement> findByNiveauAndTimestampBetweenOrderByTimestampAsc(Niveau niveau, Instant debut, Instant fin);
}
