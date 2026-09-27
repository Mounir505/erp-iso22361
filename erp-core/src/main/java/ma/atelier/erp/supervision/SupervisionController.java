/* © 2026 Équipe projet n°3 — ENSA. Tous droits réservés. Voir le fichier LICENSE. */
package ma.atelier.erp.supervision;

import jakarta.validation.Valid;
import ma.atelier.erp.crise.dto.CriseDtos.ModeDegradeDto;
import ma.atelier.erp.crise.dto.CriseDtos.ModeDegradeRequest;
import ma.atelier.erp.crise.dto.EvenementJournalDto;
import ma.atelier.erp.crise.entity.ModeDegrade;
import ma.atelier.erp.crise.service.CelluleDeCriseService;
import ma.atelier.erp.crise.service.JournalAuditService;
import ma.atelier.erp.crise.service.ModeDegradeService;
import ma.atelier.erp.crise.service.SondeDisponibilite;
import ma.atelier.erp.metier.entity.StatutPatient;
import ma.atelier.erp.metier.repository.PatientRepository;
import ma.atelier.erp.security.Roles;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

/**
 * API de supervision — contrat d'interface avec l'outillage de crise (livrable 2.3).
 * <pre>
 *   GET      /health            état de santé (up/down, base, latence) — public
 *   GET      /events            flux des événements récents (format JSON du contrat 2.1)
 *   GET|POST /status/degraded   lire ou basculer le mode dégradé lecture seule
 * </pre>
 */
@RestController
public class SupervisionController {

    private final SondeDisponibilite sonde;
    private final JournalAuditService journal;
    private final ModeDegradeService modeDegradeService;
    private final CelluleDeCriseService celluleService;
    private final PatientRepository patientRepository;

    public SupervisionController(SondeDisponibilite sonde, JournalAuditService journal,
                                 ModeDegradeService modeDegradeService, CelluleDeCriseService celluleService,
                                 PatientRepository patientRepository) {
        this.sonde = sonde;
        this.journal = journal;
        this.modeDegradeService = modeDegradeService;
        this.celluleService = celluleService;
        this.patientRepository = patientRepository;
    }

    public record EtatBase(String status, long latencyMs, String erreur) {}

    public record Sante(String status, Instant timestamp, EtatBase database, Boolean degradedMode,
                        Boolean crisisActive, int journalEnAttente, boolean simulation, Instant indisponibleDepuis) {}

    @GetMapping("/health")
    public ResponseEntity<Sante> health() {
        SondeDisponibilite.Mesure m = sonde.mesurer();
        boolean up = m.baseDisponible() && !sonde.simulationEnCours();
        Boolean degrade = null;
        Boolean crise = null;
        if (m.baseDisponible()) {
            try {
                degrade = modeDegradeService.etat().isActif();
                crise = celluleService.active().isPresent();
            } catch (RuntimeException ignored) {
                // la base vient de tomber entre la mesure et la lecture : champs laissés à null
            }
        }
        Sante sante = new Sante(up ? "UP" : "DOWN", Instant.now(),
                new EtatBase(m.baseDisponible() ? "UP" : "DOWN", m.latenceMs(), m.erreur()),
                degrade, crise, journal.tailleFileAttente(), sonde.simulationEnCours(), sonde.indisponibleDepuis());
        return ResponseEntity.status(up ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE).body(sante);
    }

    /**
     * @param apresId  ne renvoie que les entrées d'id supérieur (polling incrémental, 0 = tout)
     * @param limite   nombre maximal d'entrées (1–500)
     */
    @GetMapping("/events")
    @PreAuthorize(Roles.CELLULE_OU_ADMIN)
    public List<EvenementJournalDto> events(@RequestParam(defaultValue = "0") int apresId,
                                            @RequestParam(defaultValue = "100") int limite) {
        return journal.recents(apresId, limite);
    }

    /** Lecture ouverte à tout utilisateur connecté : chacun doit savoir que le mode dégradé est actif. */
    @GetMapping("/status/degraded")
    public ModeDegradeDto etatModeDegrade() {
        return dto(modeDegradeService.etat());
    }

    @PostMapping("/status/degraded")
    @PreAuthorize(Roles.BASCULE_MODE_DEGRADE)
    public ModeDegradeDto basculer(@Valid @RequestBody ModeDegradeRequest req) {
        celluleService.basculerModeDegrade(req.actif(), req.motif());
        return dto(modeDegradeService.etat());
    }

    private ModeDegradeDto dto(ModeDegrade m) {
        return new ModeDegradeDto(m.isActif(), m.isLectureSeule(), "Dossiers médicaux des patients hospitalisés",
                patientRepository.countByStatut(StatutPatient.HOSPITALISE));
    }
}
