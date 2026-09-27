/* © 2026 Équipe projet n°3 — ENSA. Tous droits réservés. Voir le fichier LICENSE. */
package ma.atelier.erp.crise.entity;

import jakarta.persistence.*;

import java.time.Instant;

/** Décision horodatée de la cellule de crise (diagramme : Decision) — traçabilité ISO 22361 art. 7. */
@Entity
@Table(name = "decision")
public class Decision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cellule_de_crise_id")
    private CelluleDeCrise cellule;

    @Column(nullable = false, columnDefinition = "text")
    private String libelle;

    @Column(nullable = false, length = 100)
    private String auteur;

    @Column(nullable = false)
    private Instant timestamp;

    protected Decision() {}

    public Decision(CelluleDeCrise cellule, String libelle, String auteur, Instant timestamp) {
        this.cellule = cellule;
        this.libelle = libelle;
        this.auteur = auteur;
        this.timestamp = timestamp;
    }

    public Integer getId() { return id; }
    public CelluleDeCrise getCellule() { return cellule; }
    public String getLibelle() { return libelle; }
    public String getAuteur() { return auteur; }
    public Instant getTimestamp() { return timestamp; }
}
