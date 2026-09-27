/* © 2026 Équipe projet n°3 — ENSA. Tous droits réservés. Voir le fichier LICENSE. */
package ma.atelier.erp.crise.service;

import ma.atelier.erp.common.Details;
import ma.atelier.erp.common.RegleMetierException;
import ma.atelier.erp.common.RessourceIntrouvableException;
import ma.atelier.erp.crise.entity.*;
import ma.atelier.erp.crise.repository.CelluleDeCriseRepository;
import ma.atelier.erp.crise.repository.DecisionRepository;
import ma.atelier.erp.security.SecuriteUtils;
import ma.atelier.erp.security.UtilisateurPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Cellule de crise — ISO 22361 5.3.4 (composition, responsabilités) et 5.3.5 (réponse).
 * <ul>
 *   <li>activation automatique sur tout Evenement CRITICAL (ou manuelle par le pilote) ;</li>
 *   <li>enregistrement de décisions horodatées (art. 7, traçabilité de la décision) ;</li>
 *   <li>pilotage du mode dégradé (« peut activer ») ;</li>
 *   <li>clôture par le pilote (sortie de crise, 5.3.6).</li>
 * </ul>
 */
@Service
public class CelluleDeCriseService {

    private static final String SOURCE = "cellule-de-crise";

    private final CelluleDeCriseRepository repository;
    private final DecisionRepository decisionRepository;
    private final ModeDegradeService modeDegradeService;
    private final JournalAuditService journal;
    private final Clock horloge;

    public CelluleDeCriseService(CelluleDeCriseRepository repository, DecisionRepository decisionRepository,
                                 ModeDegradeService modeDegradeService, JournalAuditService journal, Clock horloge) {
        this.repository = repository;
        this.decisionRepository = decisionRepository;
        this.modeDegradeService = modeDegradeService;
        this.journal = journal;
        this.horloge = horloge;
    }

    /**
     * Réaction à un Evenement (dépendance « declenche » du diagramme). Seul un événement CRITICAL
     * active la cellule ; si une crise est déjà en cours, l'événement lui est rattaché.
     *
     * @return la cellule active concernée, vide si l'événement n'est pas critique
     */
    @Transactional
    public synchronized Optional<CelluleDeCrise> activerSurEvenement(Evenement evenement) {
        if (!evenement.estCritique()) {
            return Optional.empty();
        }
        Optional<CelluleDeCrise> enCours = active();
        if (enCours.isPresent()) {
            // Réaction automatique : l'acteur est le système, pas l'utilisateur dont la requête a franchi le seuil
            journal.enregistrerPour(null, Niveau.INFO, TypesEvenement.EVENEMENT_RATTACHE, SOURCE,
                    "Nouvel événement critique rattaché à la crise en cours",
                    Details.de("celluleId", enCours.get().getId(), "evenementId", evenement.getId(),
                            "evenementType", evenement.getType()));
            return enCours;
        }
        return Optional.of(creerCellule("automatique", evenement, null));
    }

    /** Activation manuelle par le pilote (crise détectée hors seuils : ISO 22361 5.3.4.2, plan générique). */
    @Transactional
    public synchronized CelluleDeCrise activerManuellement(String motif) {
        if (active().isPresent()) {
            throw new RegleMetierException("Une cellule de crise est déjà active");
        }
        return creerCellule("manuelle", null, motif);
    }

    @Transactional
    public CelluleDeCrise cloturer(int id, String motif) {
        CelluleDeCrise c = trouver(id);
        c.cloturer();
        long duree = Duration.between(c.getDateActivation(), Instant.now(horloge)).toSeconds();
        journal.enregistrer(Niveau.INFO, TypesEvenement.CELLULE_CLOTUREE, SOURCE,
                "Cellule de crise clôturée — sortie de crise",
                Details.de("celluleId", c.getId(), "motif", motif, "dureeSecondes", duree,
                        "modeDegradeEncoreActif", modeDegradeService.etat().isActif()));
        return c;
    }

    @Transactional
    public Decision ajouterDecision(int celluleId, String libelle, String auteur) {
        return enregistrerDecision(trouver(celluleId), libelle, auteur);
    }

    private Decision enregistrerDecision(CelluleDeCrise c, String libelle, String auteur) {
        if (!c.estActive()) {
            throw new RegleMetierException("Impossible d'enregistrer une décision : la cellule est clôturée");
        }
        String auteurEffectif = (auteur == null || auteur.isBlank()) ? SecuriteUtils.nomCourant() : auteur.trim();
        Decision d = decisionRepository.save(new Decision(c, libelle.trim(), auteurEffectif, Instant.now(horloge)));
        journal.enregistrer(Niveau.INFO, TypesEvenement.DECISION, SOURCE, "Décision : " + d.getLibelle(),
                Details.de("celluleId", c.getId(), "decisionId", d.getId(), "auteur", auteurEffectif));
        return d;
    }

    /**
     * Bascule du mode dégradé. Si une crise est en cours, la bascule est aussi consignée comme
     * décision de la cellule (la cellule « peut activer » le mode dégradé).
     */
    @Transactional
    public boolean basculerModeDegrade(boolean actif, String motif) {
        boolean change = modeDegradeService.definir(actif, motif);
        if (change) {
            active().ifPresent(c -> enregistrerDecision(c,
                    (actif ? "Activation" : "Désactivation")
                            + " du mode dégradé (lecture seule des dossiers des patients hospitalisés)"
                            + (motif == null || motif.isBlank() ? "" : " — " + motif),
                    null));
        }
        return change;
    }

    @Transactional(readOnly = true)
    public Optional<CelluleDeCrise> active() {
        return repository.findFirstByStatut(StatutCellule.ACTIVE);
    }

    @Transactional(readOnly = true)
    public List<CelluleDeCrise> lister() {
        return repository.findAllByOrderByDateActivationDesc();
    }

    @Transactional(readOnly = true)
    public CelluleDeCrise trouver(int id) {
        return repository.findById(id).orElseThrow(() -> new RessourceIntrouvableException("Cellule de crise", id));
    }

    @Transactional(readOnly = true)
    public List<Decision> decisions(int celluleId) {
        trouver(celluleId);
        return decisionRepository.findByCelluleIdOrderByTimestampAsc(celluleId);
    }

    private CelluleDeCrise creerCellule(String declenchement, Evenement evenement, String motif) {
        CelluleDeCrise c = new CelluleDeCrise();
        c.activer(Instant.now(horloge));
        c = repository.save(c);
        UtilisateurPrincipal acteur = evenement == null ? SecuriteUtils.utilisateurCourant().orElse(null) : null;
        journal.enregistrerPour(acteur, Niveau.CRITICAL, TypesEvenement.CELLULE_ACTIVEE, SOURCE,
                "Cellule de crise activée (" + declenchement + ") — alerte CDC",
                Details.de("celluleId", c.getId(), "declenchement", declenchement,
                        "evenementId", evenement == null ? null : evenement.getId(),
                        "evenementType", evenement == null ? null : evenement.getType(),
                        "evenementTimestamp", evenement == null ? null : evenement.getTimestamp().toString(),
                        "motif", motif));
        return c;
    }
}
