/* © 2026 Équipe projet n°3 — ENSA. Tous droits réservés. Voir le fichier LICENSE. */
package ma.atelier.erp.crise.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import ma.atelier.erp.common.Details;
import ma.atelier.erp.config.ErpProperties;
import ma.atelier.erp.crise.entity.Niveau;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.*;
import java.util.stream.Stream;

/**
 * Contrôle d'intégrité des documents (FICTIFS) du répertoire surveillé.
 * <p>
 * Au démarrage, une empreinte SHA-256 de référence est calculée pour chaque fichier et
 * sauvegardée. Périodiquement, toute modification, suppression ou apparition de fichier
 * (ex. extension « .locked » typique d'un rançongiciel) est signalée au détecteur de seuils,
 * qui ouvre une crise si le seuil d'altérations est atteint (livrable 1.3).
 * <p>
 * En exercice, une altération peut être produite sans outil malveillant, par exemple :
 * {@code docker compose exec erp-core sh -c 'echo x >> /var/lib/erp/documents/compte-rendu-01.txt'}
 */
@Component
public class ControleIntegrite {

    private static final Logger log = LoggerFactory.getLogger(ControleIntegrite.class);
    private static final String SOURCE = "controle-integrite";
    private static final String FICHIER_REFERENCE = ".reference-integrite.json";

    /** Dernier résultat, exposé au dashboard. */
    public record Resultat(Instant derniereVerification, int fichiersSurveilles, List<String> alterations) {}

    private final Path repertoire;
    private final DetecteurDeSeuilService detecteur;
    private final JournalAuditService journal;
    private final ObjectMapper mapper;

    private volatile Map<String, String> reference = Map.of();
    private final Set<String> dejaSignales = Collections.synchronizedSet(new HashSet<>());
    private volatile Resultat dernier = new Resultat(null, 0, List.of());

    public ControleIntegrite(ErpProperties props, DetecteurDeSeuilService detecteur,
                             JournalAuditService journal, ObjectMapper mapper) {
        this.repertoire = Path.of(props.crise().integrite().repertoire());
        this.detecteur = detecteur;
        this.journal = journal;
        this.mapper = mapper;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initialiser() {
        try {
            Files.createDirectories(repertoire);
            if (empreintes().isEmpty()) {
                genererDocumentsFictifs();
            }
            Path ref = repertoire.resolve(FICHIER_REFERENCE);
            if (Files.exists(ref)) {
                reference = mapper.readValue(ref.toFile(), new TypeReference<>() {});
            } else {
                enregistrerReference();
            }
            dernier = new Resultat(null, reference.size(), List.of());   // en attente de la 1re vérification
            log.info("Contrôle d'intégrité : {} fichiers de référence dans {}", reference.size(), repertoire);
        } catch (IOException | RuntimeException e) {
            log.warn("Contrôle d'intégrité désactivé : répertoire {} inaccessible ({})", repertoire, e.getMessage());
        }
    }

    @Scheduled(fixedDelayString = "${erp.crise.integrite.intervalle-ms:15000}", initialDelay = 20_000)
    public void verifier() {
        if (reference.isEmpty()) {
            return;
        }
        try {
            Map<String, String> actuelles = empreintes();
            List<String> alterations = new ArrayList<>();
            reference.forEach((nom, hash) -> {
                String actuel = actuelles.get(nom);
                if (actuel == null) {
                    alterations.add(nom + " (supprimé)");
                } else if (!actuel.equals(hash)) {
                    alterations.add(nom + " (modifié)");
                }
            });
            actuelles.keySet().stream().filter(n -> !reference.containsKey(n))
                    .forEach(n -> alterations.add(n + " (nouveau)"));
            dernier = new Resultat(Instant.now(), reference.size(), List.copyOf(alterations));

            if (!alterations.isEmpty() && !dejaSignales.containsAll(alterations)) {
                boolean signale = detecteur.signalerAlterationsIntegrite(alterations.size(),
                        alterations.subList(0, Math.min(10, alterations.size())), false);
                if (signale) {
                    dejaSignales.addAll(alterations);
                }
            }
        } catch (IOException e) {
            log.warn("Vérification d'intégrité impossible : {}", e.getMessage());
        }
    }

    /**
     * Après restauration (5.3.6), le responsable technique fixe une nouvelle référence saine.
     */
    public Resultat reinitialiserReference() throws IOException {
        enregistrerReference();
        dejaSignales.clear();
        dernier = new Resultat(Instant.now(), reference.size(), List.of());
        journal.enregistrer(Niveau.INFO, TypesEvenement.INTEGRITE_REFERENCE, SOURCE,
                "Nouvelle référence d'intégrité enregistrée", Details.de("fichiers", reference.size()));
        return dernier;
    }

    public Resultat dernierResultat() {
        return dernier;
    }

    // ------------------------------------------------------------- interne --

    private void enregistrerReference() throws IOException {
        reference = empreintes();
        mapper.writeValue(repertoire.resolve(FICHIER_REFERENCE).toFile(), reference);
    }

    private Map<String, String> empreintes() throws IOException {
        Map<String, String> m = new TreeMap<>();
        try (Stream<Path> fichiers = Files.list(repertoire)) {
            for (Path f : fichiers.filter(Files::isRegularFile).toList()) {
                String nom = f.getFileName().toString();
                if (!nom.equals(FICHIER_REFERENCE)) {
                    m.put(nom, sha256(f));
                }
            }
        }
        return m;
    }

    private static String sha256(Path f) throws IOException {
        try (InputStream in = Files.newInputStream(f)) {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] tampon = new byte[8192];
            int lus;
            while ((lus = in.read(tampon)) > 0) {
                md.update(tampon, 0, lus);
            }
            return HexFormat.of().formatHex(md.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private void genererDocumentsFictifs() throws IOException {
        for (int i = 1; i <= 12; i++) {
            Files.writeString(repertoire.resolve("compte-rendu-%02d.txt".formatted(i)),
                    """
                    DOCUMENT FICTIF — atelier ISO 22361 (lab isolé)
                    Compte-rendu d'hospitalisation n°%02d
                    Patient : Patient Fictif %02d
                    Contenu : texte de démonstration sans aucune donnée réelle.
                    """.formatted(i, i), StandardCharsets.UTF_8);
        }
    }
}
