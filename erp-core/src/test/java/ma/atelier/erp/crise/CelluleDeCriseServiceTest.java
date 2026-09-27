/* © 2026 Équipe projet n°3 — ENSA. Tous droits réservés. Voir le fichier LICENSE. */
package ma.atelier.erp.crise;

import ma.atelier.erp.common.RegleMetierException;
import ma.atelier.erp.crise.entity.*;
import ma.atelier.erp.crise.repository.CelluleDeCriseRepository;
import ma.atelier.erp.crise.repository.DecisionRepository;
import ma.atelier.erp.crise.service.CelluleDeCriseService;
import ma.atelier.erp.crise.service.JournalAuditService;
import ma.atelier.erp.crise.service.ModeDegradeService;
import ma.atelier.erp.crise.service.TypesEvenement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Activation de la cellule de crise, décisions et pilotage du mode dégradé. */
class CelluleDeCriseServiceTest {

    private final HorlogeMutable horloge = new HorlogeMutable(Instant.parse("2026-10-01T14:32:10Z"));
    private CelluleDeCriseRepository repository;
    private DecisionRepository decisionRepository;
    private ModeDegradeService modeDegrade;
    private JournalAuditService journal;
    private CelluleDeCriseService service;

    @BeforeEach
    void preparer() {
        repository = mock(CelluleDeCriseRepository.class);
        decisionRepository = mock(DecisionRepository.class);
        modeDegrade = mock(ModeDegradeService.class);
        journal = mock(JournalAuditService.class);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(decisionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(modeDegrade.etat()).thenReturn(new ModeDegrade());
        service = new CelluleDeCriseService(repository, decisionRepository, modeDegrade, journal, horloge);
    }

    @Test
    @DisplayName("Evenement CRITICAL sans crise en cours → cellule ACTIVE horodatée + alerte CRITICAL")
    void activationAutomatique() {
        when(repository.findFirstByStatut(StatutCellule.ACTIVE)).thenReturn(Optional.empty());

        Optional<CelluleDeCrise> cellule = service.activerSurEvenement(critique());

        assertThat(cellule).isPresent();
        assertThat(cellule.get().estActive()).isTrue();
        assertThat(cellule.get().getDateActivation()).isEqualTo(horloge.instant());
        verify(journal).enregistrerPour(isNull(), eq(Niveau.CRITICAL), eq(TypesEvenement.CELLULE_ACTIVEE),
                anyString(), anyString(), anyMap());
    }

    @Test
    @DisplayName("Evenement WARNING → la cellule n'est pas activée")
    void warningNActivePas() {
        assertThat(service.activerSurEvenement(new Evenement(Niveau.WARNING, "x", Instant.now()))).isEmpty();
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Crise déjà en cours → l'événement est rattaché, pas de seconde cellule")
    void rattachementACriseEnCours() {
        CelluleDeCrise enCours = new CelluleDeCrise();
        enCours.activer(Instant.parse("2026-10-01T14:00:00Z"));
        when(repository.findFirstByStatut(StatutCellule.ACTIVE)).thenReturn(Optional.of(enCours));

        assertThat(service.activerSurEvenement(critique())).contains(enCours);
        verify(repository, never()).save(any());
        verify(journal).enregistrerPour(isNull(), eq(Niveau.INFO), eq(TypesEvenement.EVENEMENT_RATTACHE),
                anyString(), anyString(), anyMap());
    }

    @Test
    @DisplayName("Activation manuelle refusée si une cellule est déjà active")
    void activationManuelleEnDouble() {
        when(repository.findFirstByStatut(StatutCellule.ACTIVE)).thenReturn(Optional.of(new CelluleDeCrise()));
        assertThatThrownBy(() -> service.activerManuellement("test")).isInstanceOf(RegleMetierException.class);
    }

    @Test
    @DisplayName("Décision horodatée enregistrée et tracée ; refusée sur une cellule clôturée")
    void decisions() {
        CelluleDeCrise c = new CelluleDeCrise();
        c.activer(horloge.instant());
        when(repository.findById(1)).thenReturn(Optional.of(c));

        Decision d = service.ajouterDecision(1, "  Isoler le serveur ERP du réseau  ", "Pilote");
        assertThat(d.getLibelle()).isEqualTo("Isoler le serveur ERP du réseau");
        assertThat(d.getTimestamp()).isEqualTo(horloge.instant());
        verify(journal).enregistrer(eq(Niveau.INFO), eq(TypesEvenement.DECISION), anyString(), anyString(), anyMap());

        c.cloturer();
        assertThatThrownBy(() -> service.ajouterDecision(1, "Trop tard", "Pilote"))
                .isInstanceOf(RegleMetierException.class);
    }

    @Test
    @DisplayName("Bascule en mode dégradé pendant une crise → consignée comme décision de la cellule")
    void modeDegradeConsigneCommeDecision() {
        CelluleDeCrise c = new CelluleDeCrise();
        c.activer(horloge.instant());
        when(repository.findFirstByStatut(StatutCellule.ACTIVE)).thenReturn(Optional.of(c));
        when(modeDegrade.definir(true, "rançongiciel")).thenReturn(true);

        assertThat(service.basculerModeDegrade(true, "rançongiciel")).isTrue();

        ArgumentCaptor<Decision> decision = ArgumentCaptor.forClass(Decision.class);
        verify(decisionRepository).save(decision.capture());
        assertThat(decision.getValue().getLibelle()).startsWith("Activation du mode dégradé");
    }

    @Test
    @DisplayName("Clôture : une cellule ne peut être clôturée qu'une fois")
    void cloture() {
        CelluleDeCrise c = new CelluleDeCrise();
        c.activer(horloge.instant());
        when(repository.findById(1)).thenReturn(Optional.of(c));

        assertThat(service.cloturer(1, "Service rétabli").getStatut()).isEqualTo(StatutCellule.CLOTUREE);
        assertThatThrownBy(() -> service.cloturer(1, "encore")).isInstanceOf(RegleMetierException.class);
    }

    private static Evenement critique() {
        return new Evenement(Niveau.CRITICAL, TypesEvenement.INTEGRITE_FICHIERS, Instant.parse("2026-10-01T14:32:09Z"));
    }
}
