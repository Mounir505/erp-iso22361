/* © 2026 Équipe projet n°3 — ENSA. Tous droits réservés. Voir le fichier LICENSE. */
package ma.atelier.erp.crise.entity;

import jakarta.persistence.*;
import ma.atelier.erp.metier.entity.Utilisateur;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;

/**
 * Entrée du journal d'audit (diagramme : JournalAudit ; Utilisateur 1 → * JournalAudit « genere »).
 * Le champ {@code details} (jsonb) est l'ajout validé imposé par le contrat d'interface 2.1.
 * Une entrée est immuable une fois écrite : pas de setters.
 */
@Entity
@Table(name = "journal_audit")
public class JournalAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private Instant timestamp;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Niveau niveau;

    @Column(name = "type_evenement", nullable = false, length = 60)
    private String typeEvenement;

    @Column(nullable = false, length = 60)
    private String source;

    @Column(nullable = false, columnDefinition = "text")
    private String message;

    /** Acteur ; null pour un compte de service ou un acteur non authentifié. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utilisateur_id")
    private Utilisateur utilisateur;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> details;

    protected JournalAudit() {}

    public JournalAudit(Instant timestamp, Niveau niveau, String typeEvenement, String source, String message,
                        Utilisateur utilisateur, Map<String, Object> details) {
        this.timestamp = timestamp;
        this.niveau = niveau;
        this.typeEvenement = typeEvenement;
        this.source = source;
        this.message = message;
        this.utilisateur = utilisateur;
        this.details = details;
    }

    public Integer getId() { return id; }
    public Instant getTimestamp() { return timestamp; }
    public Niveau getNiveau() { return niveau; }
    public String getTypeEvenement() { return typeEvenement; }
    public String getSource() { return source; }
    public String getMessage() { return message; }
    public Utilisateur getUtilisateur() { return utilisateur; }
    public Map<String, Object> getDetails() { return details; }
}
