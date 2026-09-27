/* © 2026 Équipe projet n°3 — ENSA. Tous droits réservés. Voir le fichier LICENSE. */
package ma.atelier.erp.crise;

import ma.atelier.erp.config.ErpProperties;
import ma.atelier.erp.crise.entity.DetecteurDeSeuil;
import ma.atelier.erp.crise.entity.Evenement;
import ma.atelier.erp.crise.entity.JournalAudit;
import ma.atelier.erp.crise.entity.Niveau;
import ma.atelier.erp.crise.repository.DetecteurDeSeuilRepository;
import ma.atelier.erp.crise.repository.EvenementRepository;
import ma.atelier.erp.crise.service.CelluleDeCriseService;
import ma.atelier.erp.crise.service.DetecteurDeSeuilService;
import ma.atelier.erp.crise.service.JournalAuditService;
import ma.atelier.erp.crise.service.TypesEvenement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Détecteur de seuils : grille du livrable 1.3. */
class DetecteurDeSeuilServiceTest {

    private final HorlogeMutable horloge = new HorlogeMutable(Instant.parse("2026-10-01T14:00:00Z"));
    private JournalAuditService journal;
    private EvenementRepository evenementRepository;
    private CelluleDeCriseService celluleService;
    private DetecteurDeSeuilService detecteur;

    @BeforeEach
    void preparer() {
        DetecteurDeSeuilRepository repo = mock(DetecteurDeSeuilRepository.class);
        when(repo.findById(1)).thenReturn(Optional.of(new DetecteurDeSeuil(20, 60)));
        when(repo.getReferenceById(1)).thenReturn(new DetecteurDeSeuil(20, 60));
        journal = mock(JournalAuditService.class);
        evenementRepository = mock(EvenementRepository.class);
        celluleService = mock(CelluleDeCriseService.class);
        ErpProperties props = new ErpProperties(null, null, null,
                new ErpProperties.Crise(new ErpProperties.Seuils(120, 1, 50, 60), 10_000, null), null);
        detecteur = new DetecteurDeSeuilService(repo, evenementRepository, journal, celluleService, props, horloge);
    }

    @Test
    @DisplayName("Entité : plus de 20 échecs dans la fenêtre → Evenement WARNING, 20 → rien")
    void evaluerEntite() {
        DetecteurDeSeuil d = new DetecteurDeSeuil(20, 60);
        assertThat(d.evaluer(20, Instant.now())).isNull();
        Evenement e = d.evaluer(21, Instant.now());
        assertThat(e).isNotNull();
        assertThat(e.getNiveau()).isEqualTo(Niveau.WARNING);
        assertThat(e.getType()).isEqualTo(TypesEvenement.SEUIL_AUTH);
    }

    @Test
    @DisplayName("21 échecs d'authentification en moins d'une minute → une alerte WARNING")
    void seuilAuthFranchi() {
        for (int i = 0; i < 20; i++) {
            detecteur.signalerEchecAuthentification();
            horloge.avancer(Duration.ofSeconds(1));
        }
        verify(journal, never()).enregistrer(any(JournalAuditService.Entree.class), any());

        detecteur.signalerEchecAuthentification();

        JournalAuditService.Entree entree = entreeEmise();
        assertThat(entree.niveau()).isEqualTo(Niveau.WARNING);
        assertThat(entree.type()).isEqualTo(TypesEvenement.SEUIL_AUTH);
        assertThat(entree.details()).containsEntry("count", 21);
    }

    @Test
    @DisplayName("Les échecs sortis de la fenêtre glissante ne comptent plus")
    void fenetreGlissante() {
        for (int i = 0; i < 20; i++) {
            detecteur.signalerEchecAuthentification();
        }
        horloge.avancer(Duration.ofSeconds(61));
        detecteur.signalerEchecAuthentification();

        verify(journal, never()).enregistrer(any(JournalAuditService.Entree.class), any());
        assertThat(detecteur.etat().echecsAuthDansFenetre()).isEqualTo(1);
    }

    @Test
    @DisplayName("Le compteur repart de zéro après une alerte (pas de rafale d'alertes)")
    void pasDeRafale() {
        for (int i = 0; i < 25; i++) {
            detecteur.signalerEchecAuthentification();
        }
        verify(journal, times(1)).enregistrer(any(JournalAuditService.Entree.class), any());
        assertThat(detecteur.etat().echecsAuthDansFenetre()).isEqualTo(4);
    }

    @Test
    @DisplayName("Altération de fichiers → Evenement CRITICAL persisté puis cellule sollicitée")
    void integriteDeclencheCrise() {
        boolean emis = detecteur.signalerAlterationsIntegrite(3, List.of("a.txt (modifié)"), false);
        assertThat(emis).isTrue();

        JournalAuditService.Entree entree = entreeEmise();
        assertThat(entree.niveau()).isEqualTo(Niveau.CRITICAL);
        assertThat(entree.type()).isEqualTo(TypesEvenement.INTEGRITE_FICHIERS);

        // Simule la persistance de l'entrée de journal : l'Evenement est créé et la cellule sollicitée
        executerSuite(mock(JournalAudit.class));
        ArgumentCaptor<Evenement> evt = ArgumentCaptor.forClass(Evenement.class);
        verify(evenementRepository).save(evt.capture());
        assertThat(evt.getValue().estCritique()).isTrue();
        assertThat(evt.getValue().getJournal()).isNotNull();
        verify(celluleService).activerSurEvenement(evt.getValue());
    }

    @Test
    @DisplayName("Indisponibilité : incident WARNING seulement au-delà de 2 min")
    void indisponibilite() {
        assertThat(detecteur.signalerIndisponibilite(Duration.ofSeconds(119), false)).isFalse();
        verify(journal, never()).enregistrer(any(JournalAuditService.Entree.class), any());

        assertThat(detecteur.signalerIndisponibilite(Duration.ofSeconds(120), false)).isTrue();
        JournalAuditService.Entree entree = entreeEmise();
        assertThat(entree.niveau()).isEqualTo(Niveau.WARNING);
        assertThat(entree.type()).isEqualTo(TypesEvenement.SERVICE_INDISPONIBLE);
    }

    @Test
    @DisplayName("Plus de 50 consultations de dossiers en 60 s par un même acteur → CRITICAL")
    void accesEnMasse() {
        for (int i = 0; i < 50; i++) {
            detecteur.signalerAccesDossier();
        }
        verify(journal, never()).enregistrer(any(JournalAuditService.Entree.class), any());
        detecteur.signalerAccesDossier();
        JournalAuditService.Entree entree = entreeEmise();
        assertThat(entree.niveau()).isEqualTo(Niveau.CRITICAL);
        assertThat(entree.type()).isEqualTo(TypesEvenement.ACCES_MASSE);
    }

    // ------------------------------------------------------------------------

    private JournalAuditService.Entree entreeEmise() {
        ArgumentCaptor<JournalAuditService.Entree> captor = ArgumentCaptor.forClass(JournalAuditService.Entree.class);
        verify(journal).enregistrer(captor.capture(), any());
        return captor.getValue();
    }

    @SuppressWarnings("unchecked")
    private void executerSuite(JournalAudit entreePersistee) {
        ArgumentCaptor<Consumer<JournalAudit>> suite = ArgumentCaptor.forClass(Consumer.class);
        verify(journal).enregistrer(any(JournalAuditService.Entree.class), suite.capture());
        suite.getValue().accept(entreePersistee);
    }
}
