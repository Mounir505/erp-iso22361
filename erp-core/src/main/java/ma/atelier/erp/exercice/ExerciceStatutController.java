/* © 2026 Équipe projet n°3 — ENSA. Tous droits réservés. Voir le fichier LICENSE. */
package ma.atelier.erp.exercice;

import ma.atelier.erp.config.ErpProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Indique au front si le simulateur d'exercice est activé (pour afficher ou non son panneau). */
@RestController
public class ExerciceStatutController {

    private final boolean active;

    public ExerciceStatutController(ErpProperties props) {
        this.active = props.exercice().enabled();
    }

    public record Statut(boolean enabled) {}

    @GetMapping("/api/exercice/statut")
    public Statut statut() {
        return new Statut(active);
    }
}
