/* © 2026 Équipe projet n°3 — ENSA. Tous droits réservés. Voir le fichier LICENSE. */
package ma.atelier.erp.exercice;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import ma.atelier.erp.common.Details;
import ma.atelier.erp.crise.entity.Niveau;
import ma.atelier.erp.crise.service.DetecteurDeSeuilService;
import ma.atelier.erp.crise.service.JournalAuditService;
import ma.atelier.erp.crise.service.SondeDisponibilite;
import ma.atelier.erp.crise.service.TypesEvenement;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * ============================ FAIBLESSE CONTRÔLÉE ============================
 * Simulateur de signaux pour DÉCLENCHER L'EXERCICE de crise dans le lab.
 * <ul>
 *   <li>n'existe que si {@code erp.exercice.enabled=true} (CRISIS_EXERCISE_ENABLED, défaut false) ;</li>
 *   <li>réservé au rôle ADMIN (animateur de l'exercice), jamais à la cellule évaluée ;</li>
 *   <li>n'attaque rien : il se contente d'alimenter les compteurs du détecteur de seuils
 *       comme le feraient de vrais signaux ; aucune donnée n'est chiffrée, modifiée ou exfiltrée ;</li>
 *   <li>chaque injection est tracée (WARNING, source « exercice », {@code simule: true}).</li>
 * </ul>
 * Voir docs/scenario-exercice.md.
 * ============================================================================
 */
@RestController
@RequestMapping("/api/exercice")
@ConditionalOnProperty(name = "erp.exercice.enabled", havingValue = "true")
@PreAuthorize("hasRole('ADMIN')")
@Validated
public class ExerciceController {

    private static final String SOURCE = "exercice";

    private final DetecteurDeSeuilService detecteur;
    private final SondeDisponibilite sonde;
    private final JournalAuditService journal;

    public ExerciceController(DetecteurDeSeuilService detecteur, SondeDisponibilite sonde,
                              JournalAuditService journal) {
        this.detecteur = detecteur;
        this.sonde = sonde;
        this.journal = journal;
    }

    /** Rafale d'échecs d'authentification simulés (par défaut : seuil + 1 → alerte WARNING). */
    @PostMapping("/echecs-auth")
    public Map<String, Object> echecsAuth(@RequestParam(required = false) @Min(1) @Max(500) Integer nombre) {
        int n = nombre != null ? nombre : detecteur.parametres().getSeuilEchecsAuth() + 1;
        tracer("echecs-auth", n);
        for (int i = 0; i < n; i++) {
            detecteur.signalerEchecAuthentification();
        }
        return Map.of("scenario", "echecs-auth", "injectes", n);
    }

    /** Panne simulée ayant déjà duré {@code dureeSecondes} (par défaut au-delà du seuil de 2 min). */
    @PostMapping("/indisponibilite")
    public Map<String, Object> indisponibilite(@RequestParam(defaultValue = "130") @Min(1) @Max(3600) int dureeSecondes) {
        tracer("indisponibilite", dureeSecondes);
        sonde.simulerIndisponibilite(Duration.ofSeconds(dureeSecondes));
        sonde.sonder();
        return Map.of("scenario", "indisponibilite", "dureeSecondes", dureeSecondes);
    }

    @PostMapping("/indisponibilite/fin")
    public Map<String, Object> finIndisponibilite() {
        sonde.arreterSimulation();
        sonde.sonder();
        return Map.of("scenario", "indisponibilite", "termine", true);
    }

    /** Altérations de fichiers SIMULÉES (aucun fichier n'est touché) → crise CRITICAL. */
    @PostMapping("/integrite")
    public Map<String, Object> integrite(@RequestParam(defaultValue = "12") @Min(1) @Max(1000) int nombre) {
        tracer("integrite", nombre);
        detecteur.signalerAlterationsIntegrite(nombre, List.of("simulation d'exercice"), true);
        return Map.of("scenario", "integrite", "alterations", nombre);
    }

    /** Consultations en masse simulées, attribuées à l'animateur → crise CRITICAL. */
    @PostMapping("/acces-masse")
    public Map<String, Object> accesMasse(@RequestParam(required = false) @Min(1) @Max(5000) Integer nombre) {
        int n = nombre != null ? nombre : detecteur.etat().seuilAccesMasseDossiers() + 1;
        tracer("acces-masse", n);
        for (int i = 0; i < n; i++) {
            detecteur.signalerAccesDossier();
        }
        return Map.of("scenario", "acces-masse", "injectes", n);
    }

    private void tracer(String scenario, int valeur) {
        journal.enregistrer(Niveau.WARNING, TypesEvenement.EXERCICE, SOURCE,
                "Injection d'exercice : " + scenario, Details.de("scenario", scenario, "valeur", valeur, "simule", true));
    }
}
