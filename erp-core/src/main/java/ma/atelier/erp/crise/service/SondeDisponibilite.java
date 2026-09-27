/* © 2026 Équipe projet n°3 — ENSA. Tous droits réservés. Voir le fichier LICENSE. */
package ma.atelier.erp.crise.service;

import ma.atelier.erp.common.Details;
import ma.atelier.erp.crise.entity.Niveau;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/**
 * Sonde de disponibilité : vérifie périodiquement que la base répond et mesure la latence.
 * Au-delà du seuil (2 min, livrable 1.3), un incident est ouvert via le détecteur ; le retour
 * à la normale est tracé. Une indisponibilité peut aussi être SIMULÉE par le module d'exercice.
 */
@Component
public class SondeDisponibilite {

    private static final String SOURCE = "sonde-disponibilite";

    /** Résultat d'une mesure. */
    public record Mesure(boolean baseDisponible, long latenceMs, String erreur) {}

    private final JdbcTemplate jdbc;
    private final DetecteurDeSeuilService detecteur;
    private final JournalAuditService journal;
    private final Clock horloge;

    private volatile Instant indisponibleDepuis;
    private volatile boolean incidentOuvert;
    private volatile boolean simulation;

    public SondeDisponibilite(DataSource dataSource, DetecteurDeSeuilService detecteur,
                              JournalAuditService journal, Clock horloge) {
        this.jdbc = new JdbcTemplate(dataSource);
        this.jdbc.setQueryTimeout(2);
        this.detecteur = detecteur;
        this.journal = journal;
        this.horloge = horloge;
    }

    /** Mesure instantanée (utilisée par GET /health). */
    public Mesure mesurer() {
        long debut = System.nanoTime();
        try {
            jdbc.queryForObject("SELECT 1", Integer.class);
            return new Mesure(true, (System.nanoTime() - debut) / 1_000_000, null);
        } catch (RuntimeException e) {
            return new Mesure(false, (System.nanoTime() - debut) / 1_000_000, e.getClass().getSimpleName());
        }
    }

    @Scheduled(fixedDelayString = "${erp.crise.sonde-disponibilite-ms:10000}", initialDelay = 15_000)
    public void sonder() {
        boolean disponible = !simulation && mesurer().baseDisponible();
        Instant maintenant = Instant.now(horloge);
        if (!disponible) {
            if (indisponibleDepuis == null) {
                indisponibleDepuis = maintenant;
            }
            if (!incidentOuvert) {
                incidentOuvert = detecteur.signalerIndisponibilite(
                        Duration.between(indisponibleDepuis, maintenant), simulation);
            }
        } else if (indisponibleDepuis != null) {
            long duree = Duration.between(indisponibleDepuis, maintenant).toSeconds();
            if (incidentOuvert) {
                journal.enregistrer(Niveau.INFO, TypesEvenement.SERVICE_RETABLI, SOURCE,
                        "Service ERP de nouveau disponible après " + duree + " s",
                        Details.de("dureeSecondes", duree));
            }
            indisponibleDepuis = null;
            incidentOuvert = false;
        }
    }

    /** Exercice : simule une panne ayant déjà duré {@code dejaEcoulee} (évite d'attendre 2 min). */
    public void simulerIndisponibilite(Duration dejaEcoulee) {
        indisponibleDepuis = Instant.now(horloge).minus(dejaEcoulee);
        incidentOuvert = false;
        simulation = true;
    }

    public void arreterSimulation() {
        simulation = false;
    }

    public boolean simulationEnCours() {
        return simulation;
    }

    public Instant indisponibleDepuis() {
        return indisponibleDepuis;
    }
}
