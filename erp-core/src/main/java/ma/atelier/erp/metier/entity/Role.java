/* © 2026 Équipe projet n°3 — ENSA. Tous droits réservés. Voir le fichier LICENSE. */
package ma.atelier.erp.metier.entity;

import jakarta.persistence.*;

/** Rôle applicatif (diagramme : Role). Les rôles de crise ont {@code estRoleDeCrise = true}. */
@Entity
@Table(name = "role")
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 50)
    private String libelle;

    @Column(name = "est_role_de_crise", nullable = false)
    private boolean estRoleDeCrise;

    protected Role() {}

    public Role(String libelle, boolean estRoleDeCrise) {
        this.libelle = libelle;
        this.estRoleDeCrise = estRoleDeCrise;
    }

    public Integer getId() { return id; }
    public String getLibelle() { return libelle; }
    public boolean isEstRoleDeCrise() { return estRoleDeCrise; }
}
