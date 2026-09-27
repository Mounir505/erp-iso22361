/* © 2026 Équipe projet n°3 — ENSA. Tous droits réservés. Voir le fichier LICENSE. */
package ma.atelier.erp.crise.entity;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * Paramètres du détecteur de seuils (diagramme : DetecteurDeSeuil, singleton id = 1).
 * Porte le seuil d'échecs d'authentification et la fenêtre (en secondes), modifiables à chaud.
 * Les autres seuils du livrable 1.3 sont en configuration (voir ErpProperties.Seuils).
 */
@Entity
@Table(name = "detecteur_de_seuil")
public class DetecteurDeSeuil {

    public static final int ID_UNIQUE = 1;
    public static final String TYPE_SEUIL_AUTH = "auth_failure_threshold";

    @Id
    private Integer id = ID_UNIQUE;

    @Column(name = "seuil_echecs_auth", nullable = false)
    private int seuilEchecsAuth;

    /** Fenêtre glissante d'observation, en secondes. */
    @Column(nullable = false)
    private int fenetre;

    protected DetecteurDeSeuil() {}

    public DetecteurDeSeuil(int seuilEchecsAuth, int fenetre) {
        this.seuilEchecsAuth = seuilEchecsAuth;
        this.fenetre = fenetre;
    }

    /**
     * Méthode du diagramme : évalue le compteur d'échecs observés dans la fenêtre.
     * Le livrable 1.3 parle de « plus de 20 échecs / min » : le seuil est franchi strictement.
     *
     * @return un Evenement WARNING (niveau « Alerte ») si le seuil est franchi, sinon {@code null}
     */
    public Evenement evaluer(int echecsDansFenetre, Instant maintenant) {
        if (echecsDansFenetre > seuilEchecsAuth) {
            return new Evenement(Niveau.WARNING, TYPE_SEUIL_AUTH, maintenant);
        }
        return null;
    }

    public Integer getId() { return id; }
    public int getSeuilEchecsAuth() { return seuilEchecsAuth; }
    public void setSeuilEchecsAuth(int seuilEchecsAuth) { this.seuilEchecsAuth = seuilEchecsAuth; }
    public int getFenetre() { return fenetre; }
    public void setFenetre(int fenetre) { this.fenetre = fenetre; }
}
