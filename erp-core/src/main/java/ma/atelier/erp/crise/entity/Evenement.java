/* © 2026 Équipe projet n°3 — ENSA. Tous droits réservés. Voir le fichier LICENSE. */
package ma.atelier.erp.crise.entity;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * Événement de franchissement de seuil (diagramme : Evenement).
 * DetecteurDeSeuil 1 → * Evenement (« produit ») ; Evenement * → 1 JournalAudit (« trace »).
 * Un Evenement CRITICAL déclenche la cellule de crise (dépendance « declenche »).
 */
@Entity
@Table(name = "evenement")
public class Evenement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Niveau niveau;

    @Column(nullable = false, length = 60)
    private String type;

    @Column(nullable = false)
    private Instant timestamp;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "detecteur_de_seuil_id")
    private DetecteurDeSeuil detecteur;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "journal_audit_id")
    private JournalAudit journal;

    protected Evenement() {}

    public Evenement(Niveau niveau, String type, Instant timestamp) {
        this.niveau = niveau;
        this.type = type;
        this.timestamp = timestamp;
    }

    /** Rattache l'événement au détecteur qui l'a produit et à l'entrée de journal qui le trace. */
    public void rattacher(DetecteurDeSeuil detecteur, JournalAudit journal) {
        this.detecteur = detecteur;
        this.journal = journal;
    }

    public boolean estCritique() {
        return niveau == Niveau.CRITICAL;
    }

    public Integer getId() { return id; }
    public Niveau getNiveau() { return niveau; }
    public String getType() { return type; }
    public Instant getTimestamp() { return timestamp; }
    public DetecteurDeSeuil getDetecteur() { return detecteur; }
    public JournalAudit getJournal() { return journal; }
}
