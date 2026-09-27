/* © 2026 Équipe projet n°3 — ENSA. Tous droits réservés. Voir le fichier LICENSE. */
package ma.atelier.erp.metier.entity;

import jakarta.persistence.*;

import java.time.LocalDate;

/** Patient (diagramme : Patient, 1 → 1 DossierMedical, 1 → * Admission). */
@Entity
@Table(name = "patient")
public class Patient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 100)
    private String nom;

    @Column(name = "date_naissance", nullable = false)
    private LocalDate dateNaissance;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatutPatient statut;

    protected Patient() {}

    public Patient(String nom, LocalDate dateNaissance, StatutPatient statut) {
        this.nom = nom;
        this.dateNaissance = dateNaissance;
        this.statut = statut;
    }

    /** Méthode du diagramme : le patient occupe-t-il actuellement un lit ? */
    public boolean estHospitalise() {
        return statut == StatutPatient.HOSPITALISE;
    }

    public Integer getId() { return id; }
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    public LocalDate getDateNaissance() { return dateNaissance; }
    public void setDateNaissance(LocalDate dateNaissance) { this.dateNaissance = dateNaissance; }
    public StatutPatient getStatut() { return statut; }
    public void setStatut(StatutPatient statut) { this.statut = statut; }
}
