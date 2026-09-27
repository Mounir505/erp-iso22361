/* © 2026 Équipe projet n°3 — ENSA. Tous droits réservés. Voir le fichier LICENSE. */
package ma.atelier.erp.metier.entity;

import jakarta.persistence.*;

import java.time.Instant;

/** Admission d'un patient dans un service (diagramme : Admission, 1 → * Prescription). */
@Entity
@Table(name = "admission")
public class Admission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "patient_id")
    private Patient patient;

    @Column(name = "date_entree", nullable = false)
    private Instant dateEntree;

    @Column(nullable = false, length = 80)
    private String service;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatutAdmission statut;

    protected Admission() {}

    public Admission(Patient patient, Instant dateEntree, String service, StatutAdmission statut) {
        this.patient = patient;
        this.dateEntree = dateEntree;
        this.service = service;
        this.statut = statut;
    }

    public Integer getId() { return id; }
    public Patient getPatient() { return patient; }
    public Instant getDateEntree() { return dateEntree; }
    public void setDateEntree(Instant dateEntree) { this.dateEntree = dateEntree; }
    public String getService() { return service; }
    public void setService(String service) { this.service = service; }
    public StatutAdmission getStatut() { return statut; }
    public void setStatut(StatutAdmission statut) { this.statut = statut; }
}
