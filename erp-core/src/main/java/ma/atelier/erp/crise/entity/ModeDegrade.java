/* © 2026 Équipe projet n°3 — ENSA. Tous droits réservés. Voir le fichier LICENSE. */
package ma.atelier.erp.crise.entity;

import jakarta.persistence.*;
import ma.atelier.erp.metier.entity.Patient;

/**
 * Mode dégradé (diagramme : ModeDegrade, singleton id = 1).
 * Lorsqu'il est actif en lecture seule, il restreint DossierMedical des patients hospitalisés
 * (dépendance « restreint (lecture seule) », livrable 1.5).
 */
@Entity
@Table(name = "mode_degrade")
public class ModeDegrade {

    public static final int ID_UNIQUE = 1;

    @Id
    private Integer id = ID_UNIQUE;

    @Column(nullable = false)
    private boolean actif;

    @Column(name = "lecture_seule", nullable = false)
    private boolean lectureSeule = true;

    public ModeDegrade() {}

    /** Méthode du diagramme : bascule les données vitales en lecture seule. */
    public void activer() {
        this.actif = true;
        this.lectureSeule = true;
    }

    /** Retour à la normale (phase de rétablissement, ISO 22361 5.3.6). */
    public void desactiver() {
        this.actif = false;
    }

    /** Règle de restriction : une écriture sur les données de ce patient est-elle bloquée ? */
    public boolean bloqueEcriture(Patient patient) {
        return actif && lectureSeule && patient != null && patient.estHospitalise();
    }

    public Integer getId() { return id; }
    public boolean isActif() { return actif; }
    public boolean isLectureSeule() { return lectureSeule; }
}
