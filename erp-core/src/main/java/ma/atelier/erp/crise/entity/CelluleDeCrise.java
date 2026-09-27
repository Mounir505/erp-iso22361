/* © 2026 Équipe projet n°3 — ENSA. Tous droits réservés. Voir le fichier LICENSE. */
package ma.atelier.erp.crise.entity;

import jakarta.persistence.*;
import ma.atelier.erp.common.RegleMetierException;

import java.time.Instant;

/**
 * Cellule de crise (diagramme : CelluleDeCrise, 1 → * Decision « prend », peut activer ModeDegrade).
 * Une seule cellule peut être ACTIVE à la fois (index unique partiel en base).
 */
@Entity
@Table(name = "cellule_de_crise")
public class CelluleDeCrise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatutCellule statut;

    @Column(name = "date_activation", nullable = false)
    private Instant dateActivation;

    public CelluleDeCrise() {}

    /** Méthode du diagramme : active la cellule (horodatage de l'activation). */
    public void activer() {
        activer(Instant.now());
    }

    public void activer(Instant instant) {
        if (statut != null) {
            throw new RegleMetierException("Cette cellule a déjà été activée");
        }
        this.statut = StatutCellule.ACTIVE;
        this.dateActivation = instant;
    }

    /** Méthode du diagramme : clôture la crise (fin de la phase de réponse, ISO 22361 5.3.6). */
    public void cloturer() {
        if (statut != StatutCellule.ACTIVE) {
            throw new RegleMetierException("Seule une cellule active peut être clôturée");
        }
        this.statut = StatutCellule.CLOTUREE;
    }

    public boolean estActive() {
        return statut == StatutCellule.ACTIVE;
    }

    public Integer getId() { return id; }
    public StatutCellule getStatut() { return statut; }
    public Instant getDateActivation() { return dateActivation; }
}
