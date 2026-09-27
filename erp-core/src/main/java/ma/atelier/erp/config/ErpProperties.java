/* © 2026 Équipe projet n°3 — ENSA. Tous droits réservés. Voir le fichier LICENSE. */
package ma.atelier.erp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Propriétés applicatives typées (préfixe {@code erp} dans application.yml).
 * Les secrets proviennent exclusivement des variables d'environnement.
 */
@ConfigurationProperties(prefix = "erp")
public record ErpProperties(Jwt jwt, Cors cors, Audit audit, Crise crise, Exercice exercice) {

    public record Jwt(String secret, long expirationMinutes) {}

    public record Cors(List<String> allowedOrigins) {}

    /** Fichier JSON ligne par ligne du journal d'audit (volume partagé). */
    public record Audit(String fichier) {}

    public record Crise(Seuils seuils, long sondeDisponibiliteMs, Integrite integrite) {}

    /**
     * Seuils du livrable 1.3 non portés par l'entité DetecteurDeSeuil.
     *
     * @param indisponibiliteSecondes    durée d'indisponibilité au-delà de laquelle un incident est ouvert
     * @param alterationsIntegrite       nombre de fichiers altérés déclenchant la crise
     * @param accesMasseDossiers         nombre de consultations de dossiers par un même utilisateur…
     * @param fenetreAccesMasseSecondes  …dans cette fenêtre glissante
     */
    public record Seuils(int indisponibiliteSecondes, int alterationsIntegrite,
                         int accesMasseDossiers, int fenetreAccesMasseSecondes) {}

    public record Integrite(String repertoire, long intervalleMs) {}

    /** Simulateur d'exercice : « faiblesse contrôlée », désactivée par défaut. */
    public record Exercice(boolean enabled) {}
}
