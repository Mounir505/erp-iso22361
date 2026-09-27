/* © 2026 Équipe projet n°3 — ENSA. Tous droits réservés. Voir le fichier LICENSE. */
package ma.atelier.erp.rex;

import ma.atelier.erp.common.Details;
import ma.atelier.erp.crise.dto.CriseDtos.CelluleDto;
import ma.atelier.erp.crise.dto.CriseDtos.DecisionDto;
import ma.atelier.erp.crise.entity.*;
import ma.atelier.erp.crise.repository.EvenementRepository;
import ma.atelier.erp.crise.repository.JournalAuditRepository;
import ma.atelier.erp.crise.service.*;
import ma.atelier.erp.security.SecuriteUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * Module REX — génère le rapport post-crise à partir du JOURNAL (source de vérité horodatée) :
 * chronologie, temps de réponse, décisions, leçons (ISO 22361 5.3.7 amélioration continue,
 * 9.6 évaluation / apprentissage). Fournit aussi les indicateurs temps réel du dashboard.
 */
@Service
public class RexService {

    /** Fenêtre d'observation avant le déclenchement pour retrouver le premier signal. */
    static final Duration FENETRE_ANTERIEURE = Duration.ofMinutes(30);
    /** Objectif de reprise du livrable 1.5. */
    static final Duration RTO = Duration.ofHours(4);

    /** Types toujours retenus dans la chronologie, même au niveau INFO. */
    private static final Set<String> TYPES_CHRONOLOGIE = Set.of(
            TypesEvenement.DECISION, TypesEvenement.MODE_DEGRADE_ON, TypesEvenement.MODE_DEGRADE_OFF,
            TypesEvenement.CELLULE_ACTIVEE, TypesEvenement.CELLULE_CLOTUREE, TypesEvenement.EVENEMENT_RATTACHE,
            TypesEvenement.SERVICE_RETABLI, TypesEvenement.RESTAURATION, TypesEvenement.SAUVEGARDE,
            TypesEvenement.INTEGRITE_REFERENCE, TypesEvenement.CONFIG_SEUILS);

    private final CelluleDeCriseService celluleService;
    private final JournalAuditRepository journalRepository;
    private final EvenementRepository evenementRepository;
    private final ModeDegradeService modeDegradeService;
    private final DetecteurDeSeuilService detecteur;
    private final JournalAuditService journal;
    private final Clock horloge;

    public RexService(CelluleDeCriseService celluleService, JournalAuditRepository journalRepository,
                      EvenementRepository evenementRepository, ModeDegradeService modeDegradeService,
                      DetecteurDeSeuilService detecteur, JournalAuditService journal, Clock horloge) {
        this.celluleService = celluleService;
        this.journalRepository = journalRepository;
        this.evenementRepository = evenementRepository;
        this.modeDegradeService = modeDegradeService;
        this.detecteur = detecteur;
        this.journal = journal;
        this.horloge = horloge;
    }

    // ---------------------------------------------------------- situation --

    @Transactional(readOnly = true)
    public SituationDto situation() {
        Instant maintenant = Instant.now(horloge);
        Optional<CelluleDeCrise> active = celluleService.active();
        List<Evenement> recents = evenementRepository.findByTimestampBetweenOrderByTimestampAsc(
                maintenant.minus(Duration.ofHours(24)), maintenant);
        long nbWarning = recents.stream().filter(e -> e.getNiveau() == Niveau.WARNING).count();
        long nbCritical = recents.stream().filter(e -> e.getNiveau() == Niveau.CRITICAL).count();
        boolean alerteRecente = recents.stream().anyMatch(e -> e.getNiveau() == Niveau.WARNING
                && e.getTimestamp().isAfter(maintenant.minus(Duration.ofMinutes(15))));

        String niveau = active.isPresent() ? "CRISE" : alerteRecente ? "ALERTE" : "NORMAL";
        IndicateursDto indicateurs = active.map(this::indicateurs).orElse(null);
        IndicateursDto derniere = active.isPresent() ? null : celluleService.lister().stream().findFirst()
                .map(this::indicateurs).orElse(null);
        return new SituationDto(niveau, active.map(CelluleDto::de).orElse(null), indicateurs, derniere,
                modeDegradeService.etat().isActif(), detecteur.etat(), nbWarning, nbCritical);
    }

    // ---------------------------------------------------------------- REX --

    @Transactional(readOnly = true)
    public RapportRexDto rapport(int celluleId, List<String> leconsEquipe) {
        CelluleDeCrise c = celluleService.trouver(celluleId);
        IndicateursDto ind = indicateurs(c);
        Instant fin = ind.cloture() != null ? ind.cloture() : Instant.now(horloge);

        List<JournalAudit> entrees = journalRepository.entre(ind.premierSignal(), fin.plusSeconds(1));
        List<RapportRexDto.LigneChronologie> chronologie = regrouper(entrees.stream()
                .filter(j -> j.getNiveau() != Niveau.INFO || TYPES_CHRONOLOGIE.contains(j.getTypeEvenement()))
                .toList());
        List<DecisionDto> decisions = celluleService.decisions(celluleId).stream().map(DecisionDto::de).toList();
        List<String> lecons = leconsEquipe == null ? List.of()
                : leconsEquipe.stream().filter(l -> l != null && !l.isBlank()).map(String::trim).toList();

        return new RapportRexDto(Instant.now(horloge), SecuriteUtils.nomCourant(), CelluleDto.de(c), ind,
                chronologie, decisions, leconsAutomatiques(ind, entrees), lecons);
    }

    /** Trace la génération du REX (les leçons de l'équipe sont conservées dans le journal). */
    public void tracerGeneration(int celluleId, String format, List<String> lecons) {
        journal.enregistrer(Niveau.INFO, TypesEvenement.REX, "rex", "Rapport REX généré (" + format + ")",
                Details.de("celluleId", celluleId, "format", format, "lecons", lecons == null ? List.of() : lecons));
    }

    // ------------------------------------------------------- indicateurs --

    IndicateursDto indicateurs(CelluleDeCrise c) {
        Instant activation = c.getDateActivation();
        Optional<JournalAudit> entreeActivation = journalRepository
                .dernierPourCellule(TypesEvenement.CELLULE_ACTIVEE, c.getId());
        Optional<JournalAudit> entreeCloture = journalRepository
                .dernierPourCellule(TypesEvenement.CELLULE_CLOTUREE, c.getId());

        String type = entreeActivation.map(j -> String.valueOf(j.getDetails().get("declenchement")))
                .orElse("inconnu");
        Instant declenchement = entreeActivation
                .map(j -> j.getDetails().get("evenementTimestamp"))
                .map(Object::toString).map(Instant::parse)
                .orElse(activation);
        Instant cloture = c.estActive() ? null : entreeCloture.map(JournalAudit::getTimestamp).orElse(null);
        Instant fin = cloture != null ? cloture : Instant.now(horloge);

        // Premier signal suspect (WARNING ou plus) dans la fenêtre précédant le déclenchement,
        // sans remonter avant la clôture d'une crise précédente (ses signaux lui appartiennent)
        List<JournalAudit> avant = journalRepository.entre(declenchement.minus(FENETRE_ANTERIEURE), declenchement);
        Instant borne = avant.stream()
                .filter(j -> j.getTypeEvenement().equals(TypesEvenement.CELLULE_CLOTUREE))
                .map(JournalAudit::getTimestamp).reduce((a, b) -> b).orElse(Instant.MIN);
        Instant premierSignal = avant.stream()
                .filter(j -> j.getNiveau() != Niveau.INFO && j.getTimestamp().isAfter(borne))
                // les actions de réponse (restauration, bascules, décisions…) ne sont pas des signaux
                .filter(j -> !TYPES_CHRONOLOGIE.contains(j.getTypeEvenement()))
                .map(JournalAudit::getTimestamp).findFirst().orElse(declenchement);

        List<Decision> decisions = celluleService.decisions(c.getId());
        Instant premiereDecision = decisions.isEmpty() ? null : decisions.getFirst().getTimestamp();

        return new IndicateursDto(premierSignal, declenchement, activation, premiereDecision, cloture, type,
                secondes(premierSignal, declenchement),
                Math.max(0, Duration.between(declenchement, activation).toMillis()),
                premiereDecision == null ? null : secondes(activation, premiereDecision),
                secondes(activation, fin),
                dureeModeDegrade(activation, fin),
                decisions.size(), c.estActive());
    }

    /** Cumul des périodes en mode dégradé, bornées à [début, fin]. */
    private long dureeModeDegrade(Instant debut, Instant fin) {
        List<JournalAudit> bascules = journalRepository.entre(debut.minus(FENETRE_ANTERIEURE), fin).stream()
                .filter(j -> j.getTypeEvenement().equals(TypesEvenement.MODE_DEGRADE_ON)
                        || j.getTypeEvenement().equals(TypesEvenement.MODE_DEGRADE_OFF))
                .toList();
        long total = 0;
        Instant depuis = null;
        for (JournalAudit j : bascules) {
            if (j.getTypeEvenement().equals(TypesEvenement.MODE_DEGRADE_ON) && depuis == null) {
                depuis = j.getTimestamp().isBefore(debut) ? debut : j.getTimestamp();
            } else if (j.getTypeEvenement().equals(TypesEvenement.MODE_DEGRADE_OFF) && depuis != null) {
                total += secondes(depuis, j.getTimestamp());
                depuis = null;
            }
        }
        if (depuis != null) {
            total += secondes(depuis, fin);
        }
        return total;
    }

    // ------------------------------------------------------------ leçons --

    /** Constats objectifs tirés des indicateurs ; complétés par les leçons saisies par l'équipe. */
    List<String> leconsAutomatiques(IndicateursDto ind, List<JournalAudit> entrees) {
        List<String> l = new ArrayList<>();
        if (ind.tempsDetectionSecondes() > 300) {
            l.add("Détection tardive (" + ind.tempsDetectionSecondes() + " s entre le premier signal et le "
                    + "déclenchement) : revoir les seuils et la surveillance (ISO 22361 5.3.1).");
        } else {
            l.add("Détection en " + ind.tempsDetectionSecondes() + " s après le premier signal suspect.");
        }
        if (ind.tempsPremiereDecisionSecondes() == null) {
            l.add("Aucune décision enregistrée : la traçabilité de la prise de décision est à renforcer (art. 7).");
        } else if (ind.tempsPremiereDecisionSecondes() > 900) {
            l.add("Première décision après plus de 15 min : entraîner la cellule à décider sous incertitude (art. 7).");
        } else {
            l.add("Première décision prise " + ind.tempsPremiereDecisionSecondes() + " s après l'activation.");
        }
        if (ind.dureeModeDegradeSecondes() == 0) {
            l.add("Mode dégradé non activé : vérifier que la continuité des soins était assurée (livrable 1.5).");
        }
        if (ind.dureeCriseSecondes() > RTO.toSeconds()) {
            l.add("Durée de crise supérieure au RTO de 4 h : revoir le plan de rétablissement (5.3.6).");
        }
        boolean restauration = entrees.stream()
                .anyMatch(j -> j.getTypeEvenement().equals(TypesEvenement.RESTAURATION));
        if (!restauration) {
            l.add("Aucune restauration de base tracée pendant la crise : confirmer que le rétablissement "
                    + "s'appuie sur une sauvegarde vérifiée (livrable 3.2).");
        }
        if (ind.enCours()) {
            l.add("Rapport intermédiaire : la crise n'est pas encore clôturée.");
        }
        return l;
    }

    // ------------------------------------------------------------- outils --

    /** Regroupe les entrées consécutives de même type, niveau et source. */
    static List<RapportRexDto.LigneChronologie> regrouper(List<JournalAudit> entrees) {
        List<RapportRexDto.LigneChronologie> lignes = new ArrayList<>();
        RapportRexDto.LigneChronologie courante = null;
        for (JournalAudit j : entrees) {
            String user = j.getUtilisateur() == null ? null : j.getUtilisateur().getLogin();
            if (courante != null && courante.type().equals(j.getTypeEvenement())
                    && courante.niveau() == j.getNiveau() && courante.source().equals(j.getSource())
                    && !TYPES_CHRONOLOGIE.contains(j.getTypeEvenement())) {
                courante = new RapportRexDto.LigneChronologie(courante.debut(), j.getTimestamp(), courante.niveau(),
                        courante.type(), courante.source(), courante.user(), courante.message(),
                        courante.occurrences() + 1);
                lignes.set(lignes.size() - 1, courante);
            } else {
                courante = new RapportRexDto.LigneChronologie(j.getTimestamp(), j.getTimestamp(), j.getNiveau(),
                        j.getTypeEvenement(), j.getSource(), user, j.getMessage(), 1);
                lignes.add(courante);
            }
        }
        return lignes;
    }

    private static long secondes(Instant a, Instant b) {
        return Math.max(0, Duration.between(a, b).toSeconds());
    }
}
