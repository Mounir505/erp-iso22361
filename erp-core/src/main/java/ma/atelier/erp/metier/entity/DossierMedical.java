/* © 2026 Équipe projet n°3 — ENSA. Tous droits réservés. Voir le fichier LICENSE. */
package ma.atelier.erp.metier.entity;

import jakarta.persistence.*;

/** Dossier médical (diagramme : DossierMedical), donnée VITALE restreinte par le mode dégradé. */
@Entity
@Table(name = "dossier_medical")
public class DossierMedical {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "patient_id", unique = true)
    private Patient patient;

    @Column(columnDefinition = "text")
    private String antecedents;

    @Column(columnDefinition = "text")
    private String observations;

    protected DossierMedical() {}

    public DossierMedical(Patient patient, String antecedents, String observations) {
        this.patient = patient;
        this.antecedents = antecedents;
        this.observations = observations;
    }

    public Integer getId() { return id; }
    public Patient getPatient() { return patient; }
    public String getAntecedents() { return antecedents; }
    public void setAntecedents(String antecedents) { this.antecedents = antecedents; }
    public String getObservations() { return observations; }
    public void setObservations(String observations) { this.observations = observations; }
}
