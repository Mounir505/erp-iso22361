/* © 2026 Équipe projet n°3 — ENSA. Tous droits réservés. Voir le fichier LICENSE. */
package ma.atelier.erp.crise.service;

import ma.atelier.erp.common.Details;
import ma.atelier.erp.config.ErpProperties;
import ma.atelier.erp.crise.dto.EtatDetecteurDto;
import ma.atelier.erp.crise.entity.DetecteurDeSeuil;
import ma.atelier.erp.crise.entity.Evenement;
import ma.atelier.erp.crise.entity.Niveau;
import ma.atelier.erp.crise.repository.DetecteurDeSeuilRepository;
import ma.atelier.erp.crise.repository.EvenementRepository;
import ma.atelier.erp.security.SecuriteUtils;
import ma.atelier.erp.security.UtilisateurPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Détecteur de seuils — ISO 22361 5.3.1 (anticipation : détecter les signaux faibles) et
 * principe C (surveillance continue). Implémente la grille du livrable 1.3 :
 * <pre>
 *  Signal                                     Niveau     Conséquence
 *  > N échecs d'authentification / fenêtre    WARNING    alerte équipe technique
 *  Service ERP indisponible > 2 min           WARNING    ouverture d'incident
 *  Altération de fichiers détectée            CRITICAL   activation de la cellule de crise
 *  Accès anormal en masse aux dossiers        CRITICAL   activation + investigation
 * </pre>
 * Chaque franchissement produit un Evenement, tracé par une entrée de journal ; un Evenement
 * CRITICAL déclenche la cellule de crise. Les compteurs sont des fenêtres glissantes en mémoire,
 * remises à zéro après émission pour éviter les rafales d'alertes identiques.
 */
@Service
public class DetecteurDeSeuilService {

    private static final String SOURCE = "detecteur-seuils";

    private final DetecteurDeSeuilRepository repository;
    private final EvenementRepository evenementRepository;
    private final JournalAuditService journal;
    private final CelluleDeCriseService celluleService;
    private final ErpProperties.Seuils seuils;
    private final Clock horloge;

    private final Deque<Instant> echecsAuth = new ArrayDeque<>();
    private final Map<String, Deque<Instant>> accesDossiers = new ConcurrentHashMap<>();
    private volatile DetecteurDeSeuil parametres;

    public DetecteurDeSeuilService(DetecteurDeSeuilRepository repository, EvenementRepository evenementRepository,
                                   JournalAuditService journal, CelluleDeCriseService celluleService,
                                   ErpProperties props, Clock horloge) {
        this.repository = repository;
        this.evenementRepository = evenementRepository;
        this.journal = journal;
        this.celluleService = celluleService;
        this.seuils = props.crise().seuils();
        this.horloge = horloge;
    }

    // ------------------------------------------------------------ signaux --

    /** Un échec d'authentification vient d'avoir lieu. */
    public void signalerEchecAuthentification() {
        Instant maintenant = Instant.now(horloge);
        DetecteurDeSeuil p = parametres();
        Evenement evt;
        int nb;
        synchronized (echecsAuth) {
            echecsAuth.addLast(maintenant);
            purger(echecsAuth, maintenant, p.getFenetre());
            nb = echecsAuth.size();
            evt = p.evaluer(nb, maintenant);          // méthode evaluer() du diagramme
            if (evt != null) {
                echecsAuth.clear();
            }
        }
        if (evt != null) {
            emettre(evt, null, "Seuil d'échecs d'authentification franchi (" + nb + " en "
                            + p.getFenetre() + " s) — notification équipe technique",
                    Details.de("count", nb, "seuil", p.getSeuilEchecsAuth(), "fenetreSecondes", p.getFenetre()));
        }
    }

    /** Un utilisateur vient de consulter un dossier médical. */
    public void signalerAccesDossier() {
        UtilisateurPrincipal acteur = SecuriteUtils.utilisateurCourant().orElse(null);
        String cle = acteur == null ? "anonyme" : acteur.login();
        Instant maintenant = Instant.now(horloge);
        Deque<Instant> fenetre = accesDossiers.computeIfAbsent(cle, k -> new ArrayDeque<>());
        int nb;
        synchronized (fenetre) {
            fenetre.addLast(maintenant);
            purger(fenetre, maintenant, seuils.fenetreAccesMasseSecondes());
            nb = fenetre.size();
            if (nb <= seuils.accesMasseDossiers()) {
                return;
            }
            fenetre.clear();
        }
        emettre(new Evenement(Niveau.CRITICAL, TypesEvenement.ACCES_MASSE, maintenant), acteur,
                "Accès anormal en masse aux dossiers patients (" + nb + " en "
                        + seuils.fenetreAccesMasseSecondes() + " s) — activation + investigation",
                Details.de("count", nb, "login", cle, "seuil", seuils.accesMasseDossiers(),
                        "fenetreSecondes", seuils.fenetreAccesMasseSecondes()));
    }

    /**
     * Signalé par la sonde de disponibilité.
     *
     * @return {@code true} si le seuil d'incident est franchi (un Evenement a été émis)
     */
    public boolean signalerIndisponibilite(Duration duree, boolean simule) {
        if (duree.toSeconds() < seuils.indisponibiliteSecondes()) {
            return false;
        }
        emettre(new Evenement(Niveau.WARNING, TypesEvenement.SERVICE_INDISPONIBLE, Instant.now(horloge)), null,
                "Service ERP indisponible depuis " + duree.toSeconds() + " s — ouverture d'incident",
                Details.de("dureeSecondes", duree.toSeconds(), "seuilSecondes", seuils.indisponibiliteSecondes(),
                        "incident", true, "simule", simule));
        return true;
    }

    /**
     * Signalé par le contrôle d'intégrité des fichiers.
     *
     * @return {@code true} si le seuil de crise est franchi (un Evenement CRITICAL a été émis)
     */
    public boolean signalerAlterationsIntegrite(int nombre, List<String> exemples, boolean simule) {
        if (nombre < seuils.alterationsIntegrite()) {
            return false;
        }
        emettre(new Evenement(Niveau.CRITICAL, TypesEvenement.INTEGRITE_FICHIERS, Instant.now(horloge)), null,
                "Altération / chiffrement de fichiers détecté — activation de la cellule de crise",
                Details.de("count", nombre, "fichiers", exemples, "seuil", seuils.alterationsIntegrite(),
                        "simule", simule));
        return true;
    }

    // ------------------------------------------------------ configuration --

    public DetecteurDeSeuil parametres() {
        DetecteurDeSeuil p = parametres;
        if (p == null) {
            p = repository.findById(DetecteurDeSeuil.ID_UNIQUE)
                    .orElseGet(() -> new DetecteurDeSeuil(20, 60));
            parametres = p;
        }
        return p;
    }

    /** Modification à chaud des seuils (tracée en WARNING : c'est un paramètre de sécurité). */
    @Transactional
    public DetecteurDeSeuil configurer(int seuilEchecsAuth, int fenetre) {
        DetecteurDeSeuil p = repository.findById(DetecteurDeSeuil.ID_UNIQUE)
                .orElseGet(() -> new DetecteurDeSeuil(seuilEchecsAuth, fenetre));
        int ancienSeuil = p.getSeuilEchecsAuth();
        int ancienneFenetre = p.getFenetre();
        p.setSeuilEchecsAuth(seuilEchecsAuth);
        p.setFenetre(fenetre);
        p = repository.save(p);
        parametres = p;
        journal.enregistrer(Niveau.WARNING, TypesEvenement.CONFIG_SEUILS, SOURCE, "Seuils du détecteur modifiés",
                Details.de("avant", Map.of("seuilEchecsAuth", ancienSeuil, "fenetre", ancienneFenetre),
                        "apres", Map.of("seuilEchecsAuth", seuilEchecsAuth, "fenetre", fenetre)));
        return p;
    }

    public EtatDetecteurDto etat() {
        DetecteurDeSeuil p = parametres();
        Instant maintenant = Instant.now(horloge);
        int nbEchecs;
        synchronized (echecsAuth) {
            purger(echecsAuth, maintenant, p.getFenetre());
            nbEchecs = echecsAuth.size();
        }
        int maxAcces = accesDossiers.values().stream().mapToInt(d -> {
            synchronized (d) {
                purger(d, maintenant, seuils.fenetreAccesMasseSecondes());
                return d.size();
            }
        }).max().orElse(0);
        return new EtatDetecteurDto(p.getSeuilEchecsAuth(), p.getFenetre(), nbEchecs,
                seuils.indisponibiliteSecondes(), seuils.alterationsIntegrite(), seuils.accesMasseDossiers(),
                seuils.fenetreAccesMasseSecondes(), maxAcces);
    }

    // ------------------------------------------------------------- interne --

    /**
     * Trace l'événement dans le journal puis, une fois l'entrée persistée, enregistre l'Evenement
     * (rattaché au détecteur et à l'entrée qui le trace) et sollicite la cellule de crise.
     */
    private void emettre(Evenement evt, UtilisateurPrincipal acteur, String message, Map<String, Object> details) {
        var entree = new JournalAuditService.Entree(evt.getTimestamp(), evt.getNiveau(), evt.getType(), SOURCE,
                message, acteur == null ? null : acteur.id(), acteur == null ? null : acteur.login(), details);
        journal.enregistrer(entree, entreeJournal -> {
            evt.rattacher(repository.getReferenceById(DetecteurDeSeuil.ID_UNIQUE), entreeJournal);
            evenementRepository.save(evt);
            celluleService.activerSurEvenement(evt);
        });
    }

    private static void purger(Deque<Instant> fenetre, Instant maintenant, int secondes) {
        Instant limite = maintenant.minusSeconds(secondes);
        while (!fenetre.isEmpty() && !fenetre.peekFirst().isAfter(limite)) {
            fenetre.pollFirst();
        }
    }
}
