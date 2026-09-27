/* © 2026 Équipe projet n°3 — ENSA. Tous droits réservés. Voir le fichier LICENSE. */
package ma.atelier.erp.metier.entity;

import jakarta.persistence.*;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Utilisateur de l'ERP (diagramme : Utilisateur, association 1 → 1 Role).
 * Le mot de passe n'est jamais stocké en clair (hash BCrypt).
 */
@Entity
@Table(name = "utilisateur")
public class Utilisateur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 100)
    private String nom;

    @Column(nullable = false, unique = true, length = 50)
    private String login;

    @Column(name = "mot_de_passe", nullable = false, length = 100)
    private String motDePasse;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "role_id")
    private Role role;

    protected Utilisateur() {}

    public Utilisateur(String nom, String login, String motDePasseHash, Role role) {
        this.nom = nom;
        this.login = login;
        this.motDePasse = motDePasseHash;
        this.role = role;
    }

    /** Méthode du diagramme : vérifie un mot de passe candidat contre le hash stocké. */
    public boolean authentifier(String motDePasseCandidat, PasswordEncoder encoder) {
        return motDePasseCandidat != null && encoder.matches(motDePasseCandidat, motDePasse);
    }

    public Integer getId() { return id; }
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    public String getLogin() { return login; }
    public void setLogin(String login) { this.login = login; }
    public String getMotDePasse() { return motDePasse; }
    public void setMotDePasse(String motDePasseHash) { this.motDePasse = motDePasseHash; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
}
