/* © 2026 Équipe projet n°3 — ENSA. Tous droits réservés. Voir le fichier LICENSE. */
package ma.atelier.erp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Point d'entrée du mini-ERP hospitalier de l'atelier ISO 22361.
 * <p>
 * Environnement : LAB ISOLÉ, données ENTIÈREMENT FICTIVES. L'objet du projet est la
 * réponse à une crise (détection, cellule de crise, mode dégradé, traçabilité, REX).
 * Le scheduling alimente la sonde de disponibilité et le contrôle d'intégrité.
 */
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@ConfigurationPropertiesScan
@EnableScheduling
public class ErpApplication {

    public static void main(String[] args) {
        SpringApplication.run(ErpApplication.class, args);
    }
}
