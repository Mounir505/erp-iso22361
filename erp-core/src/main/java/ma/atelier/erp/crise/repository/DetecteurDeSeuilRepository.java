/* © 2026 Équipe projet n°3 — ENSA. Tous droits réservés. Voir le fichier LICENSE. */
package ma.atelier.erp.crise.repository;

import ma.atelier.erp.crise.entity.DetecteurDeSeuil;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DetecteurDeSeuilRepository extends JpaRepository<DetecteurDeSeuil, Integer> {}
