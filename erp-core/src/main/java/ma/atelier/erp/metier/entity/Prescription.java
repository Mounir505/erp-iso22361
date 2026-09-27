/* © 2026 Équipe projet n°3 — ENSA. Tous droits réservés. Voir le fichier LICENSE. */
package ma.atelier.erp.metier.entity;

import jakarta.persistence.*;

import java.time.Instant;

/** Prescription médicamenteuse rattachée à une admission (diagramme : Prescription). */
@Entity
@Table(name = "prescription")
public class Prescription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "admission_id")
    private Admission admission;

    @Column(nullable = false, length = 120)
    private String medicament;

    @Column(nullable = false, length = 200)
    private String posologie;

    @Column(nullable = false)
    private Instant date;

    protected Prescription() {}

    public Prescription(Admission admission, String medicament, String posologie, Instant date) {
        this.admission = admission;
        this.medicament = medicament;
        this.posologie = posologie;
        this.date = date;
    }

    public Integer getId() { return id; }
    public Admission getAdmission() { return admission; }
    public String getMedicament() { return medicament; }
    public void setMedicament(String medicament) { this.medicament = medicament; }
    public String getPosologie() { return posologie; }
    public void setPosologie(String posologie) { this.posologie = posologie; }
    public Instant getDate() { return date; }
    public void setDate(Instant date) { this.date = date; }
}
