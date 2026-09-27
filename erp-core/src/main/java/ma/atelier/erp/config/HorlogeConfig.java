/* © 2026 Équipe projet n°3 — ENSA. Tous droits réservés. Voir le fichier LICENSE. */
package ma.atelier.erp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/** Horloge UTC injectable : permet aux tests de maîtriser le temps (fenêtres glissantes). */
@Configuration
public class HorlogeConfig {

    @Bean
    Clock horloge() {
        return Clock.systemUTC();
    }
}
