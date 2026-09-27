/* © 2026 Équipe projet n°3 — ENSA. Tous droits réservés. Voir le fichier LICENSE. */
package ma.atelier.erp.crise.repository;

import ma.atelier.erp.crise.entity.JournalAudit;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface JournalAuditRepository extends JpaRepository<JournalAudit, Integer> {

    /** Flux récent pour le dashboard ; {@code apresId} permet un polling incrémental. */
    @Query("""
            select j from JournalAudit j left join fetch j.utilisateur
            where j.id > :apresId
            order by j.id desc""")
    List<JournalAudit> recents(@Param("apresId") int apresId, Pageable page);

    @Query("""
            select j from JournalAudit j left join fetch j.utilisateur
            where j.timestamp between :debut and :fin
            order by j.timestamp asc, j.id asc""")
    List<JournalAudit> entre(@Param("debut") Instant debut, @Param("fin") Instant fin);

    @Query(value = """
            select * from journal_audit
            where type_evenement = :type and details ->> 'celluleId' = cast(:celluleId as text)
            order by timestamp desc limit 1""", nativeQuery = true)
    Optional<JournalAudit> dernierPourCellule(@Param("type") String type, @Param("celluleId") int celluleId);
}
