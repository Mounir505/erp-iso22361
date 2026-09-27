/* © 2026 Équipe projet n°3 — ENSA. Tous droits réservés. Voir le fichier LICENSE. */
package ma.atelier.erp.crise.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import ma.atelier.erp.config.ErpProperties;
import ma.atelier.erp.crise.dto.EvenementJournalDto;
import ma.atelier.erp.crise.entity.JournalAudit;
import ma.atelier.erp.crise.entity.Niveau;
import ma.atelier.erp.crise.repository.JournalAuditRepository;
import ma.atelier.erp.metier.repository.UtilisateurRepository;
import ma.atelier.erp.security.SecuriteUtils;
import ma.atelier.erp.security.UtilisateurPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

/**
 * Journal d'audit — traçabilité ISO 22361 5.3.4.3 (gestion de l'information).
 * <p>
 * Chaque entrée est :
 * <ol>
 *   <li>persistée en base dans une transaction indépendante (REQUIRES_NEW), afin qu'une action
 *       refusée ou annulée reste tracée ;</li>
 *   <li>ajoutée au fichier partagé {@code events.log} (une ligne JSON au format du contrat 2.1),
 *       lisible par le dashboard et conservé pour le REX (livrable 3.3).</li>
 * </ol>
 * Si la base est indisponible (scénario d'incident), l'entrée est écrite dans le fichier puis
 * mise en file d'attente et persistée dès le retour de la base : aucune trace n'est perdue.
 */
@Service
public class JournalAuditService {

    private static final Logger log = LoggerFactory.getLogger(JournalAuditService.class);

    /** Données d'une entrée avant persistance (horodatage figé à la création). */
    public record Entree(Instant timestamp, Niveau niveau, String type, String source, String message,
                         Integer utilisateurId, String login, Map<String, Object> details) {}

    private record EnAttente(Entree entree, Consumer<JournalAudit> apresPersistance) {}

    private final JournalAuditRepository repository;
    private final UtilisateurRepository utilisateurRepository;
    private final ObjectMapper mapper;
    private final TransactionTemplate nouvelleTransaction;
    private final Path fichier;
    private final Queue<EnAttente> fileAttente = new ConcurrentLinkedQueue<>();
    private volatile boolean fichierDisponible = true;

    public JournalAuditService(JournalAuditRepository repository, UtilisateurRepository utilisateurRepository,
                               ObjectMapper mapper, PlatformTransactionManager txManager, ErpProperties props) {
        this.repository = repository;
        this.utilisateurRepository = utilisateurRepository;
        this.mapper = mapper;
        this.nouvelleTransaction = new TransactionTemplate(txManager);
        this.nouvelleTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.fichier = Path.of(props.audit().fichier());
    }

    @PostConstruct
    void preparerFichier() {
        try {
            Files.createDirectories(fichier.getParent());
        } catch (IOException | RuntimeException e) {
            fichierDisponible = false;
            log.warn("Journal fichier indisponible ({}) : seule la base sera utilisée", fichier);
        }
    }

    // ------------------------------------------------------------------ API --

    /** Trace une action de l'utilisateur courant (ou du système hors requête HTTP). */
    public void enregistrer(Niveau niveau, String type, String source, String message, Map<String, Object> details) {
        UtilisateurPrincipal p = SecuriteUtils.utilisateurCourant().orElse(null);
        enregistrer(new Entree(Instant.now(), niveau, type, source, message,
                p == null ? null : p.id(), p == null ? null : p.login(), details), null);
    }

    /** Trace une action pour un utilisateur explicite (ex. connexion réussie, contexte non encore peuplé). */
    public void enregistrerPour(UtilisateurPrincipal acteur, Niveau niveau, String type, String source,
                                String message, Map<String, Object> details) {
        enregistrer(new Entree(Instant.now(), niveau, type, source, message,
                acteur == null ? null : acteur.id(), acteur == null ? null : acteur.login(), details), null);
    }

    /**
     * Trace une entrée puis exécute {@code apresPersistance} (dans sa propre transaction) avec
     * l'entrée persistée — utilisé par le détecteur pour créer l'Evenement qu'elle « trace ».
     * En cas d'indisponibilité de la base, le traitement est différé jusqu'au retour de celle-ci.
     */
    public void enregistrer(Entree entree, Consumer<JournalAudit> apresPersistance) {
        try {
            JournalAudit j = persister(entree);
            ecrireFichier(versDto(j.getId(), entree));
            executerSuite(j, apresPersistance);
        } catch (RuntimeException e) {
            log.warn("Base indisponible, entrée de journal mise en attente : {} ({})", entree.type(), e.getMessage());
            ecrireFichier(versDto(null, entree));
            fileAttente.add(new EnAttente(entree, apresPersistance));
        }
    }

    public List<EvenementJournalDto> recents(int apresId, int limite) {
        return repository.recents(apresId, PageRequest.of(0, Math.clamp(limite, 1, 500)))
                .stream().map(EvenementJournalDto::de).toList();
    }

    public int tailleFileAttente() {
        return fileAttente.size();
    }

    /** Rejoue les entrées en attente dès que la base répond de nouveau (ordre conservé). */
    @Scheduled(fixedDelay = 10_000)
    public void viderFileAttente() {
        EnAttente suivante;
        while ((suivante = fileAttente.peek()) != null) {
            try {
                JournalAudit j = persister(suivante.entree());
                fileAttente.poll();
                executerSuite(j, suivante.apresPersistance());
            } catch (RuntimeException e) {
                return; // base toujours indisponible : nouvel essai au prochain cycle
            }
        }
    }

    // ------------------------------------------------------------- interne --

    private JournalAudit persister(Entree e) {
        return nouvelleTransaction.execute(status -> repository.save(new JournalAudit(
                e.timestamp(), e.niveau(), e.type(), e.source(), e.message(),
                e.utilisateurId() == null ? null : utilisateurRepository.getReferenceById(e.utilisateurId()),
                e.details())));
    }

    private void executerSuite(JournalAudit j, Consumer<JournalAudit> suite) {
        if (suite == null) {
            return;
        }
        try {
            nouvelleTransaction.executeWithoutResult(status -> suite.accept(j));
        } catch (RuntimeException ex) {
            log.error("Traitement consécutif à l'entrée de journal {} en échec", j.getId(), ex);
        }
    }

    /** Le login vient de l'entrée : l'utilisateur de l'entité n'est qu'un proxy hors session. */
    private static EvenementJournalDto versDto(Integer id, Entree e) {
        return new EvenementJournalDto(id, e.timestamp(), e.niveau(), e.type(), e.source(), e.login(),
                e.message(), e.details());
    }

    private synchronized void ecrireFichier(EvenementJournalDto dto) {
        if (!fichierDisponible) {
            return;
        }
        try {
            String ligne = mapper.writeValueAsString(dto) + System.lineSeparator();
            Files.writeString(fichier, ligne, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            log.warn("Écriture impossible dans {} : {}", fichier, e.getMessage());
        }
    }
}
